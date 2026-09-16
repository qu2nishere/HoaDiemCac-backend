package com.hoadiemcat.dto.response;

import com.hoadiemcat.entity.enums.PaymentMethod;
import com.hoadiemcat.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;
    private String invoiceCode;
    private Long tableId;
    private String tableNumber;
    private String tableName;
    private String sessionToken;
    private BigDecimal subtotal;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal taxPercent;
    private BigDecimal taxAmount;
    private BigDecimal finalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private String transactionRef;
    private String cashierName;
    private Boolean isPrinted;
    private Integer totalItems;
}
