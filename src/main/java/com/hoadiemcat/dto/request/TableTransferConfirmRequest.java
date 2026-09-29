package com.hoadiemcat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableTransferConfirmRequest {

    @NotBlank(message = "Mã số bàn đích không được để trống")
    private String targetTableNumber;

    @NotBlank(message = "Mã chuyển bàn không được để trống")
    private String transferCode;

    private String deviceFingerprint;

    private String deviceName;

    /**
     * Mã PIN 4 số của bàn đích (Bắt buộc khi GHÉP BÀN để chống đổ nợ hóa đơn sang người lạ).
     */
    private String targetPasscode;
}
