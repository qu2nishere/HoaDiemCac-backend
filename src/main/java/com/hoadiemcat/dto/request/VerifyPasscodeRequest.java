package com.hoadiemcat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPasscodeRequest {

    @NotBlank(message = "Mã PIN 4 số không được để trống")
    @Pattern(regexp = "^\\d{4}$", message = "Mã PIN phải bao gồm đúng 4 chữ số")
    private String passcode;

    private String deviceFingerprint;
}
