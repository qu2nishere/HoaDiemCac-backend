package com.hoadiemcat.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotNull
    private String tableNumber;

    private String sessionToken;

    private String note;

    private BigDecimal totalAmount;

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;
}
