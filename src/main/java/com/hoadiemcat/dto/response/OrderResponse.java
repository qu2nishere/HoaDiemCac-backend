package com.hoadiemcat.dto.response;

import com.hoadiemcat.entity.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private String tableCode;
    private String tableName;
    private String sessionToken;
    private Integer roundNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String note;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}
