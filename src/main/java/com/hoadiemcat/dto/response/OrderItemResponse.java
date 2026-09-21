package com.hoadiemcat.dto.response;

import com.hoadiemcat.entity.enums.OrderItemStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;
    private Long menuItemId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String note;
    private OrderItemStatus status;
    private LocalDateTime servedAt;
}
