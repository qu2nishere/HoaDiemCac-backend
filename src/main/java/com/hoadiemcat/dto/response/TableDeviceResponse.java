package com.hoadiemcat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDeviceResponse {

    private String deviceToken;
    private String deviceName;
    private Boolean isHost;
    private Boolean isActive;
    private LocalDateTime connectedAt;
    private Boolean isCurrentDevice;
}
