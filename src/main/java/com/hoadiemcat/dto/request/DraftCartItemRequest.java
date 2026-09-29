package com.hoadiemcat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftCartItemRequest {

    private Long menuItemId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private String note;
    private String image;
    private String unit;
}
