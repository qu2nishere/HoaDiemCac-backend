package com.hoadiemcat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableTransferConfirmResponse {

    private Long newTableId;
    private String newTableNumber;
    private String newTableName;
    private String newSessionToken;
    private String deviceToken;
    private String deviceName;
    private Boolean isHost;
    private Integer cartItemCount;
    private Integer activeOrderRounds;
    private String message;
}
