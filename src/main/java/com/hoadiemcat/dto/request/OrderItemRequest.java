package com.hoadiemcat.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

    private Long menuItemId;

    private String name;

    private BigDecimal price;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String note;
}
