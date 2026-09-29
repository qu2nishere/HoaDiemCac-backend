package com.hoadiemcat.dto.request;

import com.hoadiemcat.entity.enums.TransferType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableTransferRequest {

    @NotBlank(message = "Mã số bàn nguồn không được để trống")
    private String sourceTableNumber;

    @NotNull(message = "Loại điều chuyển (MOVE hoặc MERGE) không được để trống")
    private TransferType transferType;

    private String reason;

    private java.util.List<DraftCartItemRequest> draftCartItems;
}
