package com.hoadiemcat.dto.request;

import com.hoadiemcat.entity.enums.TableArea;
import com.hoadiemcat.entity.enums.TableStatus;
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
public class TableCreateUpdateRequest {

    @NotBlank(message = "Mã bàn không được để trống")
    private String tableNumber;

    @NotBlank(message = "Tên bàn không được để trống")
    private String name;

    @NotNull(message = "Khu vực không được để trống")
    private TableArea area;

    @NotNull(message = "Sức chứa không được để trống")
    private Integer capacity;

    private TableStatus status;

    private Boolean isOrderLocked;

    private Integer maxActiveDevices;
}
