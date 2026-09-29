package com.hoadiemcat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableClusterLinkRequest {

    @NotNull(message = "ID bàn chính không được để trống")
    private Long masterTableId;

    @NotEmpty(message = "Danh sách bàn phụ liên kết không được để trống")
    private List<Long> slaveTableIds;
}
