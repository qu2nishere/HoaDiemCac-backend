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
public class TransferHostRequest {

    @NotBlank(message = "New host deviceToken không được để trống")
    private String newHostDeviceToken;

    private String currentHostToken;
}
