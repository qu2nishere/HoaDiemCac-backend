package com.hoadiemcat.dto.request;

import com.hoadiemcat.entity.enums.TransferType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDirectTransferRequest {

    @NotNull(message = "ID bàn nguồn không được để trống")
    private Long sourceTableId;

    @NotNull(message = "ID bàn đích không được để trống")
    private Long targetTableId;

    @NotNull(message = "Loại điều chuyển (MOVE hoặc MERGE) không được để trống")
    private TransferType transferType;

    private String reason;
}
