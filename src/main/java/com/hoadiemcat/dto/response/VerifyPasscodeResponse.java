package com.hoadiemcat.dto.response;

import com.hoadiemcat.entity.enums.TableStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPasscodeResponse {

    private Long tableId;
    private String tableNumber;
    private String tableName;
    private String sessionToken;
    private String deviceToken;
    private TableStatus status;
    private Boolean isOrderLocked;
    private String message;
}
