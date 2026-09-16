package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.request.VerifyPasscodeRequest;
import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.dto.response.TableQrResponse;
import com.hoadiemcat.dto.response.VerifyPasscodeResponse;
import com.hoadiemcat.service.TableQrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/tables")
@RequiredArgsConstructor
@Tag(name = "Customer Table Access API", description = "Xác thực mã PIN 4 số và kiểm tra phiên của khách khi quét QR")
public class TableCustomerController {

    private final TableQrService tableQrService;

    @GetMapping("/{identifier}/info")
    @Operation(summary = "Lấy thông tin cơ bản của bàn khi khách quét QR đến trang check-in")
    public ResponseEntity<ApiResponse<TableQrResponse>> getTableInfo(@PathVariable String identifier) {
        TableQrResponse table = tableQrService.getTableByNumber(identifier);
        // Không trả về currentPasscode cho khách hàng công khai
        table.setCurrentPasscode(null);
        table.setCurrentSessionToken(null);
        return ResponseEntity.ok(ApiResponse.success(table));
    }

    @PostMapping("/{identifier}/verify-passcode")
    @Operation(summary = "Khách hàng nhập mã PIN 4 số để xác thực vào bàn gọi món")
    public ResponseEntity<ApiResponse<VerifyPasscodeResponse>> verifyPasscode(
            @PathVariable String identifier,
            @Valid @RequestBody VerifyPasscodeRequest request
    ) {
        VerifyPasscodeResponse response = tableQrService.verifyPasscode(identifier, request);
        return ResponseEntity.ok(ApiResponse.success("Xác thực mã PIN thành công! Quý khách có thể bắt đầu gọi món.", response));
    }

    @GetMapping("/validate-session")
    @Operation(summary = "Kiểm tra phiên bàn ăn hiện tại trong localStorage còn hiệu lực hay không")
    public ResponseEntity<ApiResponse<Boolean>> validateSession(
            @RequestHeader(value = "X-Table-Session-Token", required = false) String sessionToken
    ) {
        boolean isValid = tableQrService.validateSessionToken(sessionToken);
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }
}
