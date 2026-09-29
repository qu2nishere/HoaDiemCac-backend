package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.request.TableTransferConfirmRequest;
import com.hoadiemcat.dto.request.TableTransferRequest;
import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.dto.response.TableTransferConfirmResponse;
import com.hoadiemcat.dto.response.TableTransferResponse;
import com.hoadiemcat.service.TableTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/tables/transfers")
@RequiredArgsConstructor
@Tag(name = "Customer Table Transfer API", description = "API cho khách hàng yêu cầu và xác nhận chuyển/ghép bàn")
public class TableTransferCustomerController {

    private final TableTransferService tableTransferService;

    @PostMapping("/request")
    @Operation(summary = "Khách hàng (Chủ Bàn) yêu cầu tạo mã Chuyển hoặc Ghép bàn (TTL 5 phút)")
    public ResponseEntity<ApiResponse<TableTransferResponse>> requestTransfer(
            @Valid @RequestBody TableTransferRequest request,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @RequestHeader(value = "X-Table-Session-Token", required = false) String sessionToken
    ) {
        TableTransferResponse response = tableTransferService.requestTransfer(request, deviceToken, sessionToken);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Khách hàng xác nhận nhập mã Chuyển hoặc Ghép bàn tại bàn đích")
    public ResponseEntity<ApiResponse<TableTransferConfirmResponse>> confirmTransfer(
            @Valid @RequestBody TableTransferConfirmRequest request,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken
    ) {
        TableTransferConfirmResponse response = tableTransferService.confirmTransfer(request, deviceToken);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Khách hàng hủy yêu cầu chuyển/ghép bàn tại bàn cũ")
    public ResponseEntity<ApiResponse<TableTransferResponse>> cancelTransfer(
            @RequestParam String transferCode,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken
    ) {
        TableTransferResponse response = tableTransferService.cancelTransfer(transferCode, deviceToken);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }
}
