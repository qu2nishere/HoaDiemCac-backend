package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.VerifyPasscodeRequest;
import com.hoadiemcat.dto.response.VerifyPasscodeResponse;
import com.hoadiemcat.entity.RestaurantTable;
import com.hoadiemcat.entity.enums.TableArea;
import com.hoadiemcat.entity.enums.TableStatus;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.exception.ErrorCode;
import com.hoadiemcat.entity.TableSessionDevice;
import com.hoadiemcat.repository.RestaurantTableRepository;
import com.hoadiemcat.repository.TableSessionDeviceRepository;
import com.hoadiemcat.service.impl.TableQrServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableQrServiceTest {

    @Mock
    private RestaurantTableRepository tableRepository;

    @Mock
    private TableSessionDeviceRepository tableSessionDeviceRepository;

    @InjectMocks
    private TableQrServiceImpl tableQrService;

    private RestaurantTable mockTable;

    @BeforeEach
    void setUp() {
        mockTable = RestaurantTable.builder()
                .tableNumber("B01")
                .name("Bàn 01")
                .area(TableArea.COMMON)
                .capacity(4)
                .status(TableStatus.AVAILABLE)
                .isOrderLocked(false)
                .currentPasscode("1234")
                .failedAttempts(0)
                .maxActiveDevices(6)
                .activeDeviceCount(0)
                .currentSessionToken("session-token-1")
                .build();
        mockTable.setId(1L);
    }

    @Test
    @DisplayName("Sinh mã PIN 4 số ngẫu nhiên thành công")
    void testGenerateNewPasscode() {
        String pin = mockTable.generateNewPasscode();
        assertNotNull(pin);
        assertEquals(4, pin.length());
        assertTrue(pin.matches("^\\d{4}$"));
        assertEquals(0, mockTable.getFailedAttempts());
        assertNull(mockTable.getLockedUntil());
    }

    @Test
    @DisplayName("Xác thực mã PIN đúng - Thiết bị đầu tiên trở thành Chủ Bàn (Host)")
    void testVerifyPasscode_FirstDeviceBecomesHost() {
        when(tableRepository.findByTableNumber("B01")).thenReturn(Optional.of(mockTable));
        when(tableRepository.save(any(RestaurantTable.class))).thenReturn(mockTable);
        when(tableSessionDeviceRepository.findFirstByTableAndIsHostTrueAndIsActiveTrue(mockTable)).thenReturn(Optional.empty());

        VerifyPasscodeRequest request = VerifyPasscodeRequest.builder()
                .passcode("1234")
                .deviceFingerprint("device-abc")
                .build();

        VerifyPasscodeResponse response = tableQrService.verifyPasscode("B01", request);

        assertNotNull(response);
        assertEquals("B01", response.getTableNumber());
        assertEquals(TableStatus.OCCUPIED, response.getStatus());
        assertNotNull(response.getSessionToken());
        assertNotNull(response.getDeviceToken());
        assertTrue(response.getIsHost());
        assertEquals(1, mockTable.getActiveDeviceCount());
        verify(tableSessionDeviceRepository, times(1)).save(any(TableSessionDevice.class));
    }

    @Test
    @DisplayName("Xác thực mã PIN đúng - Thiết bị vào sau trở thành Thành Viên (Member)")
    void testVerifyPasscode_SecondDeviceBecomesMember() {
        mockTable.setActiveDeviceCount(1);
        when(tableRepository.findByTableNumber("B01")).thenReturn(Optional.of(mockTable));
        when(tableRepository.save(any(RestaurantTable.class))).thenReturn(mockTable);

        TableSessionDevice existingHost = TableSessionDevice.builder()
                .table(mockTable)
                .deviceToken("host-token")
                .isHost(true)
                .isActive(true)
                .connectedAt(LocalDateTime.now())
                .build();
        when(tableSessionDeviceRepository.findFirstByTableAndIsHostTrueAndIsActiveTrue(mockTable)).thenReturn(Optional.of(existingHost));

        VerifyPasscodeRequest request = VerifyPasscodeRequest.builder()
                .passcode("1234")
                .deviceFingerprint("device-xyz")
                .build();

        VerifyPasscodeResponse response = tableQrService.verifyPasscode("B01", request);

        assertNotNull(response);
        assertFalse(response.getIsHost());
        assertEquals(2, mockTable.getActiveDeviceCount());
        verify(tableSessionDeviceRepository, times(1)).save(any(TableSessionDevice.class));
    }

    @Test
    @DisplayName("Chủ Bàn đá thiết bị lạ ra khỏi bàn thành công")
    void testKickDevice_ByHost_Success() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(mockTable));
        mockTable.setActiveDeviceCount(2);

        TableSessionDevice hostDevice = TableSessionDevice.builder()
                .table(mockTable)
                .deviceToken("host-token")
                .isHost(true)
                .isActive(true)
                .build();

        TableSessionDevice targetDevice = TableSessionDevice.builder()
                .table(mockTable)
                .deviceToken("troll-token")
                .isHost(false)
                .isActive(true)
                .build();

        when(tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue("host-token")).thenReturn(Optional.of(hostDevice));
        when(tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue("troll-token")).thenReturn(Optional.of(targetDevice));

        tableQrService.kickDevice("1", "host-token", "troll-token");

        assertFalse(targetDevice.getIsActive());
        assertEquals(1, mockTable.getActiveDeviceCount());
        verify(tableSessionDeviceRepository, times(1)).save(targetDevice);
    }

    @Test
    @DisplayName("Thành viên thường không thể đá thiết bị khác (Yêu cầu quyền Chủ Bàn)")
    void testKickDevice_ByMember_ThrowsForbidden() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(mockTable));

        TableSessionDevice memberDevice = TableSessionDevice.builder()
                .table(mockTable)
                .deviceToken("member-token")
                .isHost(false)
                .isActive(true)
                .build();

        when(tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue("member-token")).thenReturn(Optional.of(memberDevice));

        AppException ex = assertThrows(AppException.class, () -> tableQrService.kickDevice("1", "member-token", "target-token"));
        assertEquals(ErrorCode.HOST_PERMISSION_REQUIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Chuyển quyền Chủ Bàn sang thiết bị khác thành công")
    void testTransferHost_Success() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(mockTable));

        TableSessionDevice currentHost = TableSessionDevice.builder()
                .table(mockTable)
                .deviceToken("host-token")
                .isHost(true)
                .isActive(true)
                .build();

        TableSessionDevice memberDevice = TableSessionDevice.builder()
                .table(mockTable)
                .deviceToken("member-token")
                .isHost(false)
                .isActive(true)
                .build();

        when(tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue("host-token")).thenReturn(Optional.of(currentHost));
        when(tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue("member-token")).thenReturn(Optional.of(memberDevice));

        tableQrService.transferHost("1", "host-token", "member-token");

        assertFalse(currentHost.getIsHost());
        assertTrue(memberDevice.getIsHost());
        verify(tableSessionDeviceRepository, times(1)).save(currentHost);
        verify(tableSessionDeviceRepository, times(1)).save(memberDevice);
    }

    @Test
    @DisplayName("Xác thực mã PIN sai - Ghi nhận failed attempt và văng lỗi")
    void testVerifyPasscode_WrongPin() {
        when(tableRepository.findByTableNumber("B01")).thenReturn(Optional.of(mockTable));
        when(tableRepository.save(any(RestaurantTable.class))).thenReturn(mockTable);

        VerifyPasscodeRequest request = VerifyPasscodeRequest.builder()
                .passcode("9999")
                .build();

        AppException ex = assertThrows(AppException.class, () -> tableQrService.verifyPasscode("B01", request));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
        assertEquals(1, mockTable.getFailedAttempts());
    }

    @Test
    @DisplayName("Chống Brute-force: Khóa bàn sau 5 lần nhập sai liên tiếp")
    void testVerifyPasscode_BruteForceLockout() {
        mockTable.setFailedAttempts(4);
        when(tableRepository.findByTableNumber("B01")).thenReturn(Optional.of(mockTable));
        when(tableRepository.save(any(RestaurantTable.class))).thenReturn(mockTable);

        VerifyPasscodeRequest request = VerifyPasscodeRequest.builder()
                .passcode("0000")
                .build();

        assertThrows(AppException.class, () -> tableQrService.verifyPasscode("B01", request));

        assertEquals(5, mockTable.getFailedAttempts());
        assertTrue(mockTable.isTemporarilyLocked());
        assertNotNull(mockTable.getLockedUntil());
    }

    @Test
    @DisplayName("Chặn truy cập khi vượt quá giới hạn thiết bị đồng thời (Anti-Abuse)")
    void testVerifyPasscode_DeviceLimitExceeded() {
        mockTable.setMaxActiveDevices(3);
        mockTable.setActiveDeviceCount(3);
        when(tableRepository.findByTableNumber("B01")).thenReturn(Optional.of(mockTable));

        VerifyPasscodeRequest request = VerifyPasscodeRequest.builder()
                .passcode("1234")
                .build();

        AppException ex = assertThrows(AppException.class, () -> tableQrService.verifyPasscode("B01", request));
        assertEquals(ErrorCode.DEVICE_LIMIT_EXCEEDED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Giải phóng phiên bàn ăn: Xoay mã PIN mới & thu hồi token cũ")
    void testReleaseTableSession() {
        when(tableRepository.findById(1L)).thenReturn(Optional.of(mockTable));
        when(tableRepository.save(any(RestaurantTable.class))).thenReturn(mockTable);

        String oldPasscode = mockTable.getCurrentPasscode();
        String oldSession = mockTable.getCurrentSessionToken();

        tableQrService.releaseTableSession(1L);

        assertNotEquals(oldSession, mockTable.getCurrentSessionToken());
        assertEquals(0, mockTable.getActiveDeviceCount());
        assertNull(mockTable.getSessionStartedAt());
        verify(tableRepository, times(1)).save(mockTable);
    }
}
