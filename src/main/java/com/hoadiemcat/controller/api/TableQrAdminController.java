package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.request.TableCreateUpdateRequest;
import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.dto.response.TableQrResponse;
import com.hoadiemcat.entity.enums.TableStatus;
import com.hoadiemcat.service.TableQrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tables")
@RequiredArgsConstructor
@Tag(name = "Admin Table & QR API", description = "Quản lý bàn ăn, mã QR và mật khẩu PIN xoay vòng")
public class TableQrAdminController {

    private final TableQrService tableQrService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả bàn ăn kèm QR và mã PIN hiện tại")
    public ResponseEntity<ApiResponse<List<TableQrResponse>>> getAllTables() {
        List<TableQrResponse> tables = tableQrService.getAllTables();
        return ResponseEntity.ok(ApiResponse.success(tables));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin chi tiết một bàn theo ID")
    public ResponseEntity<ApiResponse<TableQrResponse>> getTableById(@PathVariable Long id) {
        TableQrResponse table = tableQrService.getTableById(id);
        return ResponseEntity.ok(ApiResponse.success(table));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo mới một bàn ăn")
    public ResponseEntity<ApiResponse<TableQrResponse>> createTable(@Valid @RequestBody TableCreateUpdateRequest request) {
        TableQrResponse created = tableQrService.createTable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo bàn ăn thành công", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cập nhật thông tin bàn ăn")
    public ResponseEntity<ApiResponse<TableQrResponse>> updateTable(
            @PathVariable Long id,
            @Valid @RequestBody TableCreateUpdateRequest request
    ) {
        TableQrResponse updated = tableQrService.updateTable(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin bàn thành công", updated));
    }

    @PostMapping("/{id}/regenerate-pin")
    @Operation(summary = "Sinh lại mã PIN 4 số mới cho bàn và vô hiệu hóa phiên cũ")
    public ResponseEntity<ApiResponse<TableQrResponse>> regeneratePin(@PathVariable Long id) {
        TableQrResponse updated = tableQrService.regeneratePasscode(id);
        return ResponseEntity.ok(ApiResponse.success("Đã sinh mã PIN 4 số mới cho bàn", updated));
    }

    @PostMapping("/{id}/toggle-order-lock")
    @Operation(summary = "Khóa hoặc mở khóa quyền gọi món khẩn cấp của bàn")
    public ResponseEntity<ApiResponse<TableQrResponse>> toggleOrderLock(@PathVariable Long id) {
        TableQrResponse updated = tableQrService.toggleOrderLock(id);
        String msg = updated.getIsOrderLocked() ? "Đã khóa gọi món khẩn cấp" : "Đã mở khóa gọi món cho bàn";
        return ResponseEntity.ok(ApiResponse.success(msg, updated));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Cập nhật trạng thái vận hành của bàn (AVAILABLE, OCCUPIED, CLEANING)")
    public ResponseEntity<ApiResponse<TableQrResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam TableStatus status
    ) {
        TableQrResponse updated = tableQrService.updateTableStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái bàn thành công", updated));
    }

    @GetMapping("/{id}/devices")
    @Operation(summary = "Xem danh sách thiết bị đang kết nối vào bàn (Admin)")
    public ResponseEntity<ApiResponse<java.util.List<com.hoadiemcat.dto.response.TableDeviceResponse>>> getDevices(@PathVariable Long id) {
        var devices = tableQrService.getAdminActiveDevices(id);
        return ResponseEntity.ok(ApiResponse.success(devices));
    }

    @PostMapping("/{id}/reset-host")
    @Operation(summary = "Đặt lại quyền Chủ Bàn cho bàn (Admin)")
    public ResponseEntity<ApiResponse<Void>> resetHost(@PathVariable Long id) {
        tableQrService.adminResetHost(id);
        return ResponseEntity.ok(ApiResponse.success("Đã đặt lại quyền Chủ Bàn thành công", null));
    }

    @PostMapping("/{id}/kick-device")
    @Operation(summary = "Đá một thiết bị ra khỏi bàn (Admin)")
    public ResponseEntity<ApiResponse<Void>> kickDevice(
            @PathVariable Long id,
            @RequestBody com.hoadiemcat.dto.request.KickDeviceRequest request
    ) {
        tableQrService.adminKickDevice(id, request.getTargetDeviceToken());
        return ResponseEntity.ok(ApiResponse.success("Đã vô hiệu hóa thiết bị thành công", null));
    }

    @PostMapping("/{id}/resolve-call-staff")
    @Operation(summary = "Tắt / xử lý chuông gọi phục vụ hoặc yêu cầu thanh toán của bàn (Admin)")
    public ResponseEntity<ApiResponse<Void>> resolveCallStaff(
            @PathVariable Long id,
            @RequestParam(required = false) com.hoadiemcat.entity.enums.CallStaffType type
    ) {
        tableQrService.resolveCallStaff(id, type);
        return ResponseEntity.ok(ApiResponse.success("Đã xử lý thông báo của bàn", null));
    }
}
