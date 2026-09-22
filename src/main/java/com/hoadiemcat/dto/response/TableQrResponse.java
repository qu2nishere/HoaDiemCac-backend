package com.hoadiemcat.dto.response;

import com.hoadiemcat.entity.enums.TableArea;
import com.hoadiemcat.entity.enums.TableStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableQrResponse {

    private Long id;
    private String tableNumber;
    private String name;
    private TableArea area;
    private Integer capacity;
    private TableStatus status;
    private Boolean isOrderLocked;
    private String currentSessionToken;
    private String currentPasscode;
    private String qrCodeUrl;
    private String qrEntryUrl;
    private Integer activeDeviceCount;
    private Integer maxActiveDevices;
    private Boolean isTemporarilyLocked;
    private Integer failedAttempts;
    private LocalDateTime sessionStartedAt;
    private java.math.BigDecimal totalAmount;
    private Integer activeOrderCount;
    private Integer activeItemCount;
    private Boolean hasCallStaff;
    private Boolean isPaying;
}
