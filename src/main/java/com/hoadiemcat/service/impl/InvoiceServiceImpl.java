package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.response.InvoiceResponse;
import com.hoadiemcat.entity.Invoice;
import com.hoadiemcat.entity.RestaurantTable;
import com.hoadiemcat.entity.User;
import com.hoadiemcat.entity.enums.PaymentMethod;
import com.hoadiemcat.entity.enums.PaymentStatus;
import com.hoadiemcat.entity.enums.TableStatus;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.exception.ErrorCode;
import com.hoadiemcat.repository.InvoiceRepository;
import com.hoadiemcat.repository.RestaurantTableRepository;
import com.hoadiemcat.repository.UserRepository;
import com.hoadiemcat.service.InvoiceService;
import com.hoadiemcat.service.TableQrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final TableQrService tableQrService;
    private final com.hoadiemcat.repository.OrderRepository orderRepository;
    private final com.hoadiemcat.repository.CallStaffLogRepository callStaffLogRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public Page<InvoiceResponse> searchInvoices(
            String query,
            PaymentStatus status,
            PaymentMethod method,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        if (invoiceRepository.count() == 0) {
            seedDefaultInvoices();
        }

        Page<Invoice> page = invoiceRepository.searchInvoices(
                (query != null && !query.isBlank()) ? query.trim() : null,
                status,
                method,
                startDate,
                endDate,
                pageable
        );

        List<InvoiceResponse> dtoList = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByCode(String code) {
        Invoice invoice = invoiceRepository.findByInvoiceCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse settleAndCloseTable(Long tableId, PaymentMethod method, BigDecimal cashReceived, String cashierUsername) {
        RestaurantTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        User cashier = null;
        if (cashierUsername != null) {
            cashier = userRepository.findByUsername(cashierUsername).orElse(null);
        }

        List<com.hoadiemcat.entity.Order> orders = orderRepository.findByRestaurantTableOrderByCreatedAtAsc(table);
        BigDecimal subtotal = orders.stream()
                .filter(o -> o.getStatus() != com.hoadiemcat.entity.enums.OrderStatus.CANCELLED)
                .flatMap(o -> o.getOrderItems() != null ? o.getOrderItems().stream() : java.util.stream.Stream.empty())
                .map(item -> item.getPrice() != null && item.getQuantity() != null
                        ? item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
            subtotal = new BigDecimal("0.00");
        }

        BigDecimal vatPercent = new BigDecimal("8.00");
        BigDecimal vatAmount = subtotal.multiply(vatPercent).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal finalAmount = subtotal.add(vatAmount);

        BigDecimal cashChange = BigDecimal.ZERO;
        if (method == PaymentMethod.CASH && cashReceived != null && cashReceived.compareTo(finalAmount) >= 0) {
            cashChange = cashReceived.subtract(finalAmount);
        }

        String invoiceCode = "HD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + String.format("%04d", new Random().nextInt(9999));

        Invoice invoice = Invoice.builder()
                .invoiceCode(invoiceCode)
                .restaurantTable(table)
                .sessionToken(table.getCurrentSessionToken() != null ? table.getCurrentSessionToken() : UUID.randomUUID().toString())
                .subtotal(subtotal)
                .discountPercent(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .taxPercent(vatPercent)
                .taxAmount(vatAmount)
                .finalAmount(finalAmount)
                .paymentMethod(method)
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .cashReceived(cashReceived)
                .cashChange(cashChange)
                .transactionRef(method == PaymentMethod.VIETQR ? "VQR" + System.currentTimeMillis() : null)
                .cashier(cashier)
                .isPrinted(true)
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        // Cập nhật trạng thái các đơn hàng sang COMPLETED
        for (com.hoadiemcat.entity.Order o : orders) {
            if (o.getStatus() != com.hoadiemcat.entity.enums.OrderStatus.CANCELLED) {
                o.setStatus(com.hoadiemcat.entity.enums.OrderStatus.COMPLETED);
            }
        }
        orderRepository.saveAll(orders);

        // Giải quyết chuông gọi nhân viên đang chờ (nếu có)
        List<com.hoadiemcat.entity.CallStaffLog> pendingLogs = callStaffLogRepository.findByRestaurantTableAndStatus(
                table, com.hoadiemcat.entity.enums.CallStaffStatus.PENDING
        );
        for (com.hoadiemcat.entity.CallStaffLog l : pendingLogs) {
            l.setStatus(com.hoadiemcat.entity.enums.CallStaffStatus.RESOLVED);
            l.setResolvedAt(LocalDateTime.now());
        }
        callStaffLogRepository.saveAll(pendingLogs);

        // Kích hoạt giải phóng phiên bàn ăn: Xoay mã PIN 4 số mới và thu hồi Session Token cũ
        tableQrService.releaseTableSession(tableId);
        table.setStatus(TableStatus.CLEANING);
        RestaurantTable savedTable = tableRepository.save(table);

        try {
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/tables", tableQrService.getTableById(tableId));
            }
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo WebSocket cập nhật bàn qua /topic/tables sau thanh toán: {}", e.getMessage());
        }

        return mapToResponse(saved);
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .tableId(invoice.getRestaurantTable() != null ? invoice.getRestaurantTable().getId() : null)
                .tableNumber(invoice.getRestaurantTable() != null ? invoice.getRestaurantTable().getTableNumber() : "B00")
                .tableName(invoice.getRestaurantTable() != null ? invoice.getRestaurantTable().getName() : "Bàn")
                .sessionToken(invoice.getSessionToken())
                .subtotal(invoice.getSubtotal())
                .discountPercent(invoice.getDiscountPercent())
                .discountAmount(invoice.getDiscountAmount())
                .taxPercent(invoice.getTaxPercent())
                .taxAmount(invoice.getTaxAmount())
                .finalAmount(invoice.getFinalAmount())
                .paymentMethod(invoice.getPaymentMethod())
                .paymentStatus(invoice.getPaymentStatus())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .transactionRef(invoice.getTransactionRef())
                .cashierName(invoice.getCashier() != null ? invoice.getCashier().getFullName() : "Thu Ngân Ca Trực")
                .isPrinted(Boolean.TRUE.equals(invoice.getIsPrinted()))
                .totalItems(5)
                .build();
    }

    private void seedDefaultInvoices() {
        log.info("Seeding initial mock invoice history...");
        List<RestaurantTable> tables = tableRepository.findAll();
        if (tables.isEmpty()) return;

        List<Invoice> seeds = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        String todayStr = LocalDateTime.now().format(dtf);

        long[] amounts = { 890000, 1620000, 1150000, 740000, 1317000, 1480000, 6850000, 8420000, 3200000, 5150000, 4390000, 7920000, 9150000 };

        for (int i = 0; i < amounts.length; i++) {
            RestaurantTable table = tables.get(i % tables.size());
            BigDecimal subtotal = BigDecimal.valueOf(amounts[i]);
            BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.08"));
            BigDecimal finalAmount = subtotal.add(taxAmount);
            PaymentMethod method = (i % 2 == 0) ? PaymentMethod.VIETQR : PaymentMethod.CASH;

            Invoice inv = Invoice.builder()
                    .invoiceCode("HD-" + todayStr + "-" + String.format("%04d", 100 + i))
                    .restaurantTable(table)
                    .sessionToken(UUID.randomUUID().toString())
                    .subtotal(subtotal)
                    .discountPercent(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .taxPercent(new BigDecimal("8.00"))
                    .taxAmount(taxAmount)
                    .finalAmount(finalAmount)
                    .paymentMethod(method)
                    .paymentStatus(PaymentStatus.PAID)
                    .paidAt(LocalDateTime.now().minusHours(amounts.length - i).minusMinutes(12))
                    .transactionRef(method == PaymentMethod.VIETQR ? "VQR" + (98234120 + i) : null)
                    .isPrinted(true)
                    .build();
            seeds.add(inv);
        }

        invoiceRepository.saveAll(seeds);
    }
}
