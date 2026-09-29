package com.hoadiemcat.dto.response;

import com.hoadiemcat.entity.enums.TransferStatus;
import com.hoadiemcat.entity.enums.TransferType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableTransferResponse {

    private String transferCode;
    private TransferType transferType;
    private String sourceTableNumber;
    private String sourceTableName;
    private String targetTableNumber;
    private String targetTableName;
    private TransferStatus status;
    private LocalDateTime expiresAt;
    private Long ttlSeconds;
    private String message;
}
