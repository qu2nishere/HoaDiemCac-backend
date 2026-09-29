package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.DraftCartItemRequest;
import com.hoadiemcat.dto.request.TableTransferConfirmRequest;
import com.hoadiemcat.dto.request.TableTransferRequest;
import com.hoadiemcat.dto.response.DraftCartItemResponse;
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
}
