package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.DraftCartItemRequest;
import com.hoadiemcat.dto.request.TableClusterLinkRequest;
import com.hoadiemcat.dto.request.TableTransferConfirmRequest;
import com.hoadiemcat.dto.request.TableTransferRequest;
import com.hoadiemcat.dto.response.DraftCartItemResponse;
import com.hoadiemcat.dto.response.TableClusterResponse;
import com.hoadiemcat.dto.response.TableTransferConfirmResponse;
import com.hoadiemcat.dto.response.TableTransferResponse;
import com.hoadiemcat.entity.*;
import com.hoadiemcat.entity.enums.*;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.exception.ErrorCode;
import com.hoadiemcat.repository.*;
import com.hoadiemcat.service.impl.TableTransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableTransferServiceTest {

    @Mock
    private TableTransferRepository tableTransferRepository;

    @Mock
    private RestaurantTableRepository tableRepository;

    @Mock
    private TableSessionDeviceRepository tableSessionDeviceRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TableTransferServiceImpl tableTransferService;

    private RestaurantTable sourceTable;
    private RestaurantTable targetTable;
    private MenuItem menuItemHotpot;

    @BeforeEach
    void setUp() {
        sourceTable = RestaurantTable.builder()
                .tableNumber("B01")
                .name("Bàn 01")
                .status(TableStatus.OCCUPIED)
                .isOrderLocked(false)
                .currentSessionToken("session-token-b01")
                .currentPasscode("1111")
                .activeDeviceCount(1)
                .build();
        sourceTable.setId(1L);

        targetTable = RestaurantTable.builder()
                .tableNumber("B05")
                .name("Bàn 05")
                .status(TableStatus.AVAILABLE)
                .isOrderLocked(false)
                .currentSessionToken("session-token-b05")
                .currentPasscode("5555")
                .activeDeviceCount(0)
                .build();
        targetTable.setId(5L);

        menuItemHotpot = MenuItem.builder()
                .name("Lẩu Thái Hoàng Gia")
                .price(new BigDecimal("350000"))
                .isAvailable(true)
                .build();
        menuItemHotpot.setId(10L);
    }

    @Test
    @DisplayName("TC-TRF-01: Yêu cầu chuyển bàn thành công (Tạo mã & Khóa tạm thời 2-phase lock)")
    void testRequestTransfer_Success() {
        TableTransferRequest request = TableTransferRequest.builder()
                .sourceTableNumber("B01")
                .transferType(TransferType.MOVE)
                .reason("Khách đổi bàn")
                .build();

        when(tableRepository.findByTableNumber("B01")).thenReturn(Optional.of(sourceTable));
        when(invoiceRepository.findByRestaurantTableId(1L)).thenReturn(Collections.emptyList());
        when(tableTransferRepository.findFirstBySourceTableAndStatus(sourceTable, TransferStatus.PENDING))
                .thenReturn(Optional.empty());
        when(tableTransferRepository.existsByTransferCode(anyString())).thenReturn(false);
        when(tableTransferRepository.save(any(TableTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TableTransferResponse response = tableTransferService.requestTransfer(request, "device-host-1", "session-token-b01");

        assertNotNull(response);
        assertNotNull(response.getTransferCode());
        assertTrue(response.getTransferCode().startsWith("TRF-"));
        assertEquals("B01", response.getSourceTableNumber());
        assertEquals(TransferStatus.PENDING, response.getStatus());
        assertTrue(sourceTable.getIsOrderLocked(), "Bàn nguồn phải bị khóa tạm thời để bảo toàn giỏ hàng");
        verify(tableRepository).save(sourceTable);
        verify(tableTransferRepository).save(any(TableTransfer.class));
    }

    @Test
    @DisplayName("TC-TRF-02: Xác nhận Chuyển bàn thành công (MOVE 1:1 sang bàn trống)")
    void testConfirmTransfer_Move_Success() {
        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-8888")
                .transferType(TransferType.MOVE)
                .sourceTable(sourceTable)
                .sourceSessionToken("session-token-b01")
                .createdByDevice("device-host-1")
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        Cart sourceCart = Cart.builder()
                .restaurantTable(sourceTable)
                .sessionToken("session-token-b01")
                .items(new ArrayList<>())
                .build();

        when(tableTransferRepository.findByTransferCode("TRF-8888")).thenReturn(Optional.of(transfer));
        when(tableRepository.findByTableNumber("B05")).thenReturn(Optional.of(targetTable));
        when(cartRepository.findByRestaurantTable(targetTable)).thenReturn(Optional.empty()).thenReturn(Optional.of(sourceCart));
        when(cartRepository.findByRestaurantTable(sourceTable)).thenReturn(Optional.of(sourceCart));
        when(orderRepository.findByRestaurantTableOrderByCreatedAtAsc(sourceTable)).thenReturn(Collections.emptyList());
        when(tableSessionDeviceRepository.findFirstByTableAndIsHostTrueAndIsActiveTrue(targetTable))
                .thenReturn(Optional.empty());

        TableTransferConfirmRequest request = TableTransferConfirmRequest.builder()
                .targetTableNumber("B05")
                .transferCode("TRF-8888")
                .deviceName("Chủ Bàn Mới")
                .build();

        TableTransferConfirmResponse response = tableTransferService.confirmTransfer(request, "new-device-token");

        assertNotNull(response);
        assertEquals("B05", response.getNewTableNumber());
        assertTrue(response.getIsHost());
        assertEquals(TableStatus.OCCUPIED, targetTable.getStatus());
        assertEquals(TableStatus.AVAILABLE, sourceTable.getStatus());
        assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
        assertEquals(targetTable, sourceCart.getRestaurantTable(), "Giỏ hàng phải được chuyển sang bàn đích B05");
    }

    @Test
    @DisplayName("TC-TRF-03: Xác nhận Ghép bàn thành công (MERGE N:1 cộng dồn món trong giỏ)")
    void testConfirmTransfer_Merge_CombinesCartItems() {
        targetTable.setStatus(TableStatus.OCCUPIED);

        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-9999")
                .transferType(TransferType.MERGE)
                .sourceTable(sourceTable)
                .sourceSessionToken("session-token-b01")
                .createdByDevice("device-host-1")
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        Cart sourceCart = Cart.builder()
                .restaurantTable(sourceTable)
                .sessionToken("session-token-b01")
                .items(new ArrayList<>())
                .build();
        CartItem item1 = CartItem.builder().cart(sourceCart).menuItem(menuItemHotpot).quantity(2).note("Ít cay").build();
        sourceCart.addItem(item1);

        Cart targetCart = Cart.builder()
                .restaurantTable(targetTable)
                .sessionToken("session-token-b05")
                .items(new ArrayList<>())
                .build();
        CartItem item2 = CartItem.builder().cart(targetCart).menuItem(menuItemHotpot).quantity(3).note("Ít cay").build();
        targetCart.addItem(item2);

        when(tableTransferRepository.findByTransferCode("TRF-9999")).thenReturn(Optional.of(transfer));
        when(tableRepository.findByTableNumber("B05")).thenReturn(Optional.of(targetTable));
        when(cartRepository.findByRestaurantTable(sourceTable)).thenReturn(Optional.of(sourceCart));
        when(cartRepository.findByRestaurantTable(targetTable)).thenReturn(Optional.of(targetCart));
        when(orderRepository.findByRestaurantTableOrderByCreatedAtAsc(sourceTable)).thenReturn(Collections.emptyList());
        when(orderRepository.findByRestaurantTableOrderByCreatedAtAsc(targetTable)).thenReturn(Collections.emptyList());
        when(tableSessionDeviceRepository.findFirstByTableAndIsHostTrueAndIsActiveTrue(targetTable))
                .thenReturn(Optional.of(new TableSessionDevice()));

        TableTransferConfirmRequest request = TableTransferConfirmRequest.builder()
                .targetTableNumber("B05")
                .transferCode("TRF-9999")
                .targetPasscode("5555")
                .build();

        TableTransferConfirmResponse response = tableTransferService.confirmTransfer(request, "member-token");

        assertNotNull(response);
        assertEquals("B05", response.getNewTableNumber());
        assertEquals(1, targetCart.getItems().size());
        assertEquals(5, targetCart.getItems().get(0).getQuantity(), "Số lượng 2 + 3 phải được cộng dồn thành 5 món");
        verify(cartRepository).delete(sourceCart);
        verify(cartRepository).save(targetCart);
    }

    @Test
    @DisplayName("TC-TRF-04: Từ chối Chuyển bàn khi Bàn đích không trống (TARGET_TABLE_NOT_AVAILABLE)")
    void testConfirmTransfer_Move_ThrowsWhenTargetNotAvailable() {
        targetTable.setStatus(TableStatus.OCCUPIED); // Bàn đích đang có người ngồi

        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-7777")
                .transferType(TransferType.MOVE)
                .sourceTable(sourceTable)
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(tableTransferRepository.findByTransferCode("TRF-7777")).thenReturn(Optional.of(transfer));
        when(tableRepository.findByTableNumber("B05")).thenReturn(Optional.of(targetTable));

        TableTransferConfirmRequest request = TableTransferConfirmRequest.builder()
                .targetTableNumber("B05")
                .transferCode("TRF-7777")
                .build();

        AppException ex = assertThrows(AppException.class, () -> tableTransferService.confirmTransfer(request, "dev-1"));
        assertEquals(ErrorCode.TARGET_TABLE_NOT_AVAILABLE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-TRF-05: Từ chối Chuyển hoặc Ghép vào chính bàn hiện tại")
    void testConfirmTransfer_ThrowsWhenSelfTransfer() {
        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-1111")
                .transferType(TransferType.MOVE)
                .sourceTable(sourceTable)
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(tableTransferRepository.findByTransferCode("TRF-1111")).thenReturn(Optional.of(transfer));
        when(tableRepository.findByTableNumber("B01")).thenReturn(Optional.of(sourceTable));

        TableTransferConfirmRequest request = TableTransferConfirmRequest.builder()
                .targetTableNumber("B01")
                .transferCode("TRF-1111")
                .build();

        AppException ex = assertThrows(AppException.class, () -> tableTransferService.confirmTransfer(request, "dev-1"));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-TRF-06: Mã chuyển bàn quá hạn 5 phút (TRANSFER_CODE_EXPIRED & Hoàn trả khóa)")
    void testConfirmTransfer_ThrowsWhenExpired() {
        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-0000")
                .transferType(TransferType.MOVE)
                .sourceTable(sourceTable)
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // Đã quá hạn 1 phút
                .build();

        sourceTable.setIsOrderLocked(true);

        when(tableTransferRepository.findByTransferCode("TRF-0000")).thenReturn(Optional.of(transfer));

        TableTransferConfirmRequest request = TableTransferConfirmRequest.builder()
                .targetTableNumber("B05")
                .transferCode("TRF-0000")
                .build();

        AppException ex = assertThrows(AppException.class, () -> tableTransferService.confirmTransfer(request, "dev-1"));
        assertEquals(ErrorCode.TRANSFER_CODE_EXPIRED, ex.getErrorCode());
        assertEquals(TransferStatus.EXPIRED, transfer.getStatus());
        assertFalse(sourceTable.getIsOrderLocked(), "Bàn nguồn phải tự động mở khóa hoàn trả lại");
    }

    @Test
    @DisplayName("TC-TRF-07: Hủy yêu cầu chuyển bàn (CANCELLED & Mở khóa bảo toàn giỏ hàng)")
    void testCancelTransfer_Success() {
        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-4444")
                .transferType(TransferType.MOVE)
                .sourceTable(sourceTable)
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .build();

        sourceTable.setIsOrderLocked(true);

        when(tableTransferRepository.findByTransferCode("TRF-4444")).thenReturn(Optional.of(transfer));

        TableTransferResponse response = tableTransferService.cancelTransfer("TRF-4444", "device-host-1");

        assertNotNull(response);
        assertEquals(TransferStatus.CANCELLED, transfer.getStatus());
        assertFalse(sourceTable.getIsOrderLocked(), "Bàn nguồn phải được mở khóa bình thường");
        verify(tableTransferRepository).save(transfer);
        verify(tableRepository).save(sourceTable);
    }

    @Test
    @DisplayName("TC-TRF-08: Yêu cầu chuyển bàn kèm giỏ hàng nháp (Đồng bộ vào Cart DB bảo toàn món ăn)")
    void testRequestTransfer_WithDraftCartItems_SyncsToDatabase() {
        DraftCartItemRequest draftItem = DraftCartItemRequest.builder()
                .menuItemId(10L)
                .name("Lẩu Thái Hoàng Gia")
                .price(new BigDecimal("350000"))
                .quantity(3)
                .note("Ít cay")
                .build();

        TableTransferRequest request = TableTransferRequest.builder()
                .sourceTableNumber("B01")
                .transferType(TransferType.MOVE)
                .reason("Khách chuyển bàn")
                .draftCartItems(List.of(draftItem))
                .build();

        when(tableRepository.findByTableNumber("B01")).thenReturn(Optional.of(sourceTable));
        when(invoiceRepository.findByRestaurantTableId(1L)).thenReturn(Collections.emptyList());
        when(tableTransferRepository.findFirstBySourceTableAndStatus(sourceTable, TransferStatus.PENDING)).thenReturn(Optional.empty());
        when(cartRepository.findByRestaurantTable(sourceTable)).thenReturn(Optional.empty());
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(menuItemHotpot));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TableTransferResponse response = tableTransferService.requestTransfer(request, "device-host", "session-token-b01");

        assertNotNull(response);
        verify(cartRepository, atLeastOnce()).save(any(Cart.class));
    }

    @Test
    @DisplayName("TC-TRF-09: Xác nhận chuyển bàn trả về đầy đủ giỏ hàng nháp đã hợp nhất (cartItems trong response)")
    void testConfirmTransfer_ReturnsMergedDraftCart() {
        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-5555")
                .transferType(TransferType.MOVE)
                .sourceTable(sourceTable)
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        Cart sourceCart = Cart.builder()
                .restaurantTable(sourceTable)
                .sessionToken("session-token-b01")
                .items(new ArrayList<>())
                .build();
        CartItem cartItem = CartItem.builder()
                .cart(sourceCart)
                .menuItem(menuItemHotpot)
                .quantity(2)
                .note("Không hành")
                .build();
        sourceCart.addItem(cartItem);

        when(tableTransferRepository.findByTransferCode("TRF-5555")).thenReturn(Optional.of(transfer));
        when(tableRepository.findByTableNumber("B05")).thenReturn(Optional.of(targetTable));
        when(cartRepository.findByRestaurantTable(targetTable)).thenReturn(Optional.empty()).thenReturn(Optional.of(sourceCart));
        when(cartRepository.findByRestaurantTable(sourceTable)).thenReturn(Optional.of(sourceCart));
        when(orderRepository.findByRestaurantTableOrderByCreatedAtAsc(sourceTable)).thenReturn(Collections.emptyList());
        when(tableSessionDeviceRepository.findFirstByTableAndIsHostTrueAndIsActiveTrue(targetTable)).thenReturn(Optional.empty());

        TableTransferConfirmRequest confirmReq = TableTransferConfirmRequest.builder()
                .transferCode("TRF-5555")
                .targetTableNumber("B05")
                .deviceName("Chủ Bàn")
                .build();

        TableTransferConfirmResponse confirmResponse = tableTransferService.confirmTransfer(confirmReq, "device-token-1");

        assertNotNull(confirmResponse);
        assertEquals("B05", confirmResponse.getNewTableNumber());
        assertNotNull(confirmResponse.getCartItems());
        assertEquals(1, confirmResponse.getCartItems().size());
        assertEquals("Lẩu Thái Hoàng Gia", confirmResponse.getCartItems().get(0).getName());
        assertEquals(2, confirmResponse.getCartItems().get(0).getQuantity());
        assertEquals("Không hành", confirmResponse.getCartItems().get(0).getNote());
    }

    @Test
    @DisplayName("TC-TRF-10: Từ chối Ghép bàn khi khách hàng nhập sai hoặc thiếu mã PIN bàn đích (Chống gian lận dồn bill)")
    void testConfirmTransfer_Merge_ThrowsWhenTargetPasscodeInvalid() {
        targetTable.setStatus(TableStatus.OCCUPIED);

        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-9999")
                .transferType(TransferType.MERGE)
                .sourceTable(sourceTable)
                .sourceSessionToken("session-token-b01")
                .createdByDevice("device-host-1")
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(tableTransferRepository.findByTransferCode("TRF-9999")).thenReturn(Optional.of(transfer));
        when(tableRepository.findByTableNumber("B05")).thenReturn(Optional.of(targetTable));

        TableTransferConfirmRequest wrongPasscodeReq = TableTransferConfirmRequest.builder()
                .targetTableNumber("B05")
                .transferCode("TRF-9999")
                .targetPasscode("0000") // targetTable passcode is "5555"
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                tableTransferService.confirmTransfer(wrongPasscodeReq, "customer-device-token"));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-TRF-11: Tạo Cụm bàn tiệc lớn thành công (Master-Slave Table Clustering)")
    void testLinkTablesToCluster_Success() {
        RestaurantTable slaveTable = RestaurantTable.builder()
                .tableNumber("B06")
                .name("Bàn 06")
                .status(TableStatus.AVAILABLE)
                .capacity(4)
                .build();
        slaveTable.setId(6L);

        when(tableRepository.findById(5L)).thenReturn(Optional.of(targetTable));
        when(tableRepository.findById(6L)).thenReturn(Optional.of(slaveTable));
        when(invoiceRepository.findByRestaurantTableId(6L)).thenReturn(Collections.emptyList());

        TableClusterLinkRequest request = TableClusterLinkRequest.builder()
                .masterTableId(5L)
                .slaveTableIds(List.of(6L))
                .build();

        TableClusterResponse response = tableTransferService.linkTablesToCluster(request, "staff01");

        assertNotNull(response);
        assertEquals(5L, response.getMasterTableId());
        assertTrue(response.getLinkedTableNumbers().contains("B06"));
        assertEquals(targetTable, slaveTable.getMasterTable());
        assertEquals(TableStatus.OCCUPIED, slaveTable.getStatus());
        assertEquals(targetTable.getCurrentSessionToken(), slaveTable.getCurrentSessionToken());
        assertEquals(targetTable.getCurrentPasscode(), slaveTable.getCurrentPasscode());
        assertEquals(12, targetTable.getMaxActiveDevices(), "Trần thiết bị kết nối phải được nâng lên 12 cho cụm 8 chỗ");
        verify(tableRepository, atLeastOnce()).save(slaveTable);
    }

    @Test
    @DisplayName("TC-TRF-12: Tách bàn phụ ra khỏi Cụm bàn liên kết thành công")
    void testUnlinkTableFromCluster_Success() {
        targetTable.setCapacity(4);
        RestaurantTable slaveTable = RestaurantTable.builder()
                .tableNumber("B06")
                .name("Bàn 06")
                .status(TableStatus.OCCUPIED)
                .capacity(4)
                .masterTable(targetTable)
                .build();
        slaveTable.setId(6L);

        when(tableRepository.findById(6L)).thenReturn(Optional.of(slaveTable));
        lenient().when(tableRepository.findByMasterTable(any())).thenReturn(Collections.emptyList());

        TableClusterResponse response = tableTransferService.unlinkTableFromCluster(6L, "staff01");

        assertNotNull(response);
        assertNull(slaveTable.getMasterTable());
        assertEquals(TableStatus.AVAILABLE, slaveTable.getStatus());
        assertEquals(6, targetTable.getMaxActiveDevices(), "Trần thiết bị của bàn chính phải giảm về 6 khi cụm chỉ còn lại 1 bàn");
        verify(tableRepository, atLeastOnce()).save(slaveTable);
    }

    @Test
    @DisplayName("BUG-03: Từ chối Chuyển/Ghép bàn khi bàn đích đang dọn dẹp (CLEANING)")
    void testConfirmTransfer_RejectsWhenTargetIsCleaning() {
        targetTable.setStatus(TableStatus.CLEANING);

        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-1111")
                .transferType(TransferType.MOVE)
                .sourceTable(sourceTable)
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(tableTransferRepository.findByTransferCode("TRF-1111")).thenReturn(Optional.of(transfer));
        when(tableRepository.findByTableNumber("B05")).thenReturn(Optional.of(targetTable));

        TableTransferConfirmRequest request = TableTransferConfirmRequest.builder()
                .targetTableNumber("B05")
                .transferCode("TRF-1111")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                tableTransferService.confirmTransfer(request, "device-host"));
        assertEquals(ErrorCode.TARGET_TABLE_NOT_AVAILABLE, ex.getErrorCode());
    }

    @Test
    @DisplayName("BUG-03: Từ chối Ghép bàn khi bàn đích không có khách ngồi (chưa OCCUPIED)")
    void testConfirmTransfer_Merge_RejectsWhenTargetIsNotOccupied() {
        targetTable.setStatus(TableStatus.AVAILABLE);

        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-2222")
                .transferType(TransferType.MERGE)
                .sourceTable(sourceTable)
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(tableTransferRepository.findByTransferCode("TRF-2222")).thenReturn(Optional.of(transfer));
        when(tableRepository.findByTableNumber("B05")).thenReturn(Optional.of(targetTable));

        TableTransferConfirmRequest request = TableTransferConfirmRequest.builder()
                .targetTableNumber("B05")
                .transferCode("TRF-2222")
                .targetPasscode("5555")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                tableTransferService.confirmTransfer(request, "device-host"));
        assertEquals(ErrorCode.TARGET_TABLE_NOT_AVAILABLE, ex.getErrorCode());
    }

    @Test
    @DisplayName("BUG-02: Bảo toàn các thiết bị thành viên (Members) từ bàn cũ sang bàn mới khi chuyển bàn")
    void testConfirmTransfer_MigratesActiveMembersToTarget() {
        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-3333")
                .transferType(TransferType.MOVE)
                .sourceTable(sourceTable)
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        TableSessionDevice hostDevice = TableSessionDevice.builder()
                .table(sourceTable)
                .deviceToken("token-host")
                .deviceName("Chủ Bàn")
                .isHost(true)
                .isActive(true)
                .build();

        TableSessionDevice memberDevice = TableSessionDevice.builder()
                .table(sourceTable)
                .deviceToken("token-member")
                .deviceName("Bạn A")
                .isHost(false)
                .isActive(true)
                .build();

        List<TableSessionDevice> sourceDevices = new ArrayList<>(List.of(hostDevice, memberDevice));

        when(tableTransferRepository.findByTransferCode("TRF-3333")).thenReturn(Optional.of(transfer));
        when(tableRepository.findByTableNumber("B05")).thenReturn(Optional.of(targetTable));
        when(tableSessionDeviceRepository.findByTable(sourceTable)).thenReturn(sourceDevices, Collections.emptyList());
        when(tableSessionDeviceRepository.findByTableAndIsActiveTrueOrderByConnectedAtAsc(targetTable))
                .thenReturn(sourceDevices);

        TableTransferConfirmRequest request = TableTransferConfirmRequest.builder()
                .targetTableNumber("B05")
                .transferCode("TRF-3333")
                .build();

        TableTransferConfirmResponse response = tableTransferService.confirmTransfer(request, "token-host");

        assertNotNull(response);
        assertEquals(targetTable, memberDevice.getTable(), "Thiết bị thành viên phải được chuyển quyền sở hữu sang bàn đích");
        assertTrue(memberDevice.getIsActive(), "Thiết bị thành viên phải tiếp tục hoạt động tại bàn mới");
        assertFalse(memberDevice.getIsHost(), "Thành viên chuyển sang phải giữ vai trò Thành Viên (Member)");
        assertEquals(2, targetTable.getActiveDeviceCount(), "Số lượng thiết bị kích hoạt tại bàn đích phải là 2");
    }

    @Test
    @DisplayName("Từ chối tạo Cụm bàn khi Bàn phụ đã là bàn chính hoặc đang nằm trong cụm khác")
    void testLinkTablesToCluster_RejectsWhenSlaveIsAlreadyMasterOrLinked() {
        RestaurantTable slaveMaster = RestaurantTable.builder()
                .tableNumber("B06")
                .linkedTables(List.of(new RestaurantTable()))
                .build();
        slaveMaster.setId(6L);

        when(tableRepository.findById(5L)).thenReturn(Optional.of(targetTable));
        when(tableRepository.findById(6L)).thenReturn(Optional.of(slaveMaster));

        TableClusterLinkRequest request = TableClusterLinkRequest.builder()
                .masterTableId(5L)
                .slaveTableIds(List.of(6L))
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                tableTransferService.linkTablesToCluster(request, "staff01"));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    @DisplayName("EDGE-19: Từ chối Chuyển bàn khi phiên bàn nguồn đã thay đổi hoặc đã kết thúc")
    void testConfirmTransfer_RejectsWhenSourceSessionChangedOrEnded() {
        sourceTable.setStatus(TableStatus.AVAILABLE); // Bàn nguồn đã bị trả/thanh toán

        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-4444")
                .transferType(TransferType.MOVE)
                .sourceTable(sourceTable)
                .sourceSessionToken("session-token-old")
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(tableTransferRepository.findByTransferCode("TRF-4444")).thenReturn(Optional.of(transfer));

        TableTransferConfirmRequest request = TableTransferConfirmRequest.builder()
                .targetTableNumber("B05")
                .transferCode("TRF-4444")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                tableTransferService.confirmTransfer(request, "device-host"));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
        assertEquals(TransferStatus.CANCELLED, transfer.getStatus());
    }

    @Test
    @DisplayName("EDGE-23: Tính Đẳng cự (Idempotency) - Mobile retry gửi lại mã COMPLETED trả về thành công")
    void testConfirmTransfer_IdempotentRetryReturnsSuccess() {
        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-5555")
                .transferType(TransferType.MOVE)
                .sourceTable(sourceTable)
                .targetTable(targetTable)
                .targetSessionToken("session-token-b05")
                .status(TransferStatus.COMPLETED)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(tableTransferRepository.findByTransferCode("TRF-5555")).thenReturn(Optional.of(transfer));
        when(orderRepository.findByRestaurantTableOrderByCreatedAtAsc(targetTable)).thenReturn(Collections.emptyList());

        TableTransferConfirmRequest request = TableTransferConfirmRequest.builder()
                .targetTableNumber("B05")
                .transferCode("TRF-5555")
                .deviceName("Thiết bị khách")
                .build();

        TableTransferConfirmResponse response = tableTransferService.confirmTransfer(request, "device-host");
        assertNotNull(response);
        assertEquals("B05", response.getNewTableNumber());
        assertEquals("session-token-b05", response.getNewSessionToken());
    }

    @Test
    @DisplayName("EDGE-21: Từ chối tạo mã chuyển khi bàn nguồn là Bàn phụ (Slave) của cụm bàn")
    void testRequestTransfer_RejectsWhenSourceIsLinkedSlave() {
        RestaurantTable master = RestaurantTable.builder().tableNumber("B01").build();
        master.setId(1L);
        sourceTable.setMasterTable(master);

        TableTransferRequest request = TableTransferRequest.builder()
                .sourceTableNumber("B01")
                .transferType(TransferType.MOVE)
                .build();

        when(tableRepository.findByTableNumber("B01")).thenReturn(Optional.of(sourceTable));

        AppException ex = assertThrows(AppException.class, () ->
                tableTransferService.requestTransfer(request, "device-host", "session-token-b01"));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("đang là bàn phụ"));
    }

    @Test
    @DisplayName("EDGE-22: Từ chối tạo mã chuyển khi bàn nguồn là Bàn chính (Master) có các bàn phụ liên kết")
    void testRequestTransfer_RejectsWhenSourceIsMasterWithSlaves() {
        RestaurantTable slave = RestaurantTable.builder().tableNumber("B02").masterTable(sourceTable).build();
        slave.setId(2L);

        TableTransferRequest request = TableTransferRequest.builder()
                .sourceTableNumber("B01")
                .transferType(TransferType.MOVE)
                .build();

        when(tableRepository.findByTableNumber("B01")).thenReturn(Optional.of(sourceTable));
        when(tableRepository.findByMasterTable(sourceTable)).thenReturn(List.of(slave));

        AppException ex = assertThrows(AppException.class, () ->
                tableTransferService.requestTransfer(request, "device-host", "session-token-b01"));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("đang là Bàn chính"));
    }

    @Test
    @DisplayName("EDGE-20: Tự động điều hướng ghép vào Bàn chính khi Bàn đích là Bàn phụ của cụm")
    void testConfirmTransfer_Merge_AutoRoutesToMasterWhenTargetIsSlave() {
        RestaurantTable master = RestaurantTable.builder()
                .tableNumber("B09")
                .name("Bàn 09 (Chính)")
                .status(TableStatus.OCCUPIED)
                .currentSessionToken("session-token-b09")
                .currentPasscode("9999")
                .isOrderLocked(false)
                .build();
        master.setId(9L);

        RestaurantTable slave = RestaurantTable.builder()
                .tableNumber("B10")
                .name("Bàn 10 (Phụ)")
                .status(TableStatus.OCCUPIED)
                .masterTable(master)
                .currentSessionToken("session-token-b09")
                .currentPasscode("9999")
                .isOrderLocked(false)
                .build();
        slave.setId(10L);

        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-6666")
                .transferType(TransferType.MERGE)
                .sourceTable(sourceTable)
                .sourceSessionToken("session-token-b01")
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(tableTransferRepository.findByTransferCode("TRF-6666")).thenReturn(Optional.of(transfer));
        when(tableRepository.findByTableNumber("B10")).thenReturn(Optional.of(slave));
        when(cartRepository.findByRestaurantTable(sourceTable)).thenReturn(Optional.empty());
        when(cartRepository.findByRestaurantTable(master)).thenReturn(Optional.empty());
        when(orderRepository.findByRestaurantTableOrderByCreatedAtAsc(sourceTable)).thenReturn(Collections.emptyList());
        when(orderRepository.findByRestaurantTableOrderByCreatedAtAsc(master)).thenReturn(Collections.emptyList());

        TableTransferConfirmRequest request = TableTransferConfirmRequest.builder()
                .targetTableNumber("B10")
                .transferCode("TRF-6666")
                .targetPasscode("9999")
                .build();

        TableTransferConfirmResponse response = tableTransferService.confirmTransfer(request, "STAFF_POS");
        assertNotNull(response);
        assertEquals("B09", response.getNewTableNumber(), "Ghép vào bàn phụ B10 phải tự động chuyển thành ghép vào bàn chính B09");
    }

    @Test
    @DisplayName("EDGE-26: Từ chối hủy chuyển bàn khi thiết bị chỉ là Thành viên (Member)")
    void testCancelTransfer_RejectsWhenMemberCancels() {
        TableTransfer transfer = TableTransfer.builder()
                .transferCode("TRF-7777")
                .sourceTable(sourceTable)
                .status(TransferStatus.PENDING)
                .build();

        TableSessionDevice memberDevice = TableSessionDevice.builder()
                .table(sourceTable)
                .deviceToken("token-member")
                .isHost(false)
                .isActive(true)
                .build();

        when(tableTransferRepository.findByTransferCode("TRF-7777")).thenReturn(Optional.of(transfer));
        when(tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue("token-member"))
                .thenReturn(Optional.of(memberDevice));

        AppException ex = assertThrows(AppException.class, () ->
                tableTransferService.cancelTransfer("TRF-7777", "token-member"));
        assertEquals(ErrorCode.HOST_PERMISSION_REQUIRED, ex.getErrorCode());
    }
}
