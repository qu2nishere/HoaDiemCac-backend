package com.hoadiemcat.controller.api;

import com.hoadiemcat.dto.response.ApiResponse;
import com.hoadiemcat.dto.response.InvoiceResponse;
import com.hoadiemcat.entity.enums.PaymentMethod;
import com.hoadiemcat.entity.enums.PaymentStatus;
import com.hoadiemcat.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/invoices")
@RequiredArgsConstructor
@Tag(name = "Admin Invoice API", description = "Quản lý và tra cứu lịch sử hóa đơn thanh toán")
public class InvoiceAdminController {

    private final InvoiceService invoiceService;

    @GetMapping
    @Operation(summary = "Tìm kiếm và phân trang danh sách hóa đơn theo nhiều tiêu chí")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> searchInvoices(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InvoiceResponse> invoices = invoiceService.searchInvoices(query, status, method, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một hóa đơn theo ID")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long id) {
        InvoiceResponse invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Tra cứu hóa đơn theo mã code")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByCode(@PathVariable String code) {
        InvoiceResponse invoice = invoiceService.getInvoiceByCode(code);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/{tableId}/settle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Xác nhận thanh toán hóa đơn cho bàn, giải phóng phiên và tự động xoay mã PIN mới")
    public ResponseEntity<ApiResponse<InvoiceResponse>> settleInvoice(
            @PathVariable Long tableId,
            @RequestParam(defaultValue = "CASH") PaymentMethod method,
            @RequestParam(required = false) BigDecimal cashReceived,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String username = userDetails != null ? userDetails.getUsername() : "admin";
        InvoiceResponse invoice = invoiceService.settleAndCloseTable(tableId, method, cashReceived, username);
        return ResponseEntity.ok(ApiResponse.success("Thanh toán thành công! Bàn đã được đóng và cấp mã PIN mới.", invoice));
    }
}
