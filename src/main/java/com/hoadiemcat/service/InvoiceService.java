package com.hoadiemcat.service;

import com.hoadiemcat.dto.response.InvoiceResponse;
import com.hoadiemcat.entity.enums.PaymentMethod;
import com.hoadiemcat.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface InvoiceService {

    Page<InvoiceResponse> searchInvoices(
            String query,
            PaymentStatus status,
            PaymentMethod method,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    InvoiceResponse getInvoiceById(Long id);

    InvoiceResponse getInvoiceByCode(String code);

    InvoiceResponse settleAndCloseTable(Long tableId, PaymentMethod method, BigDecimal cashReceived, String cashierUsername);
}
