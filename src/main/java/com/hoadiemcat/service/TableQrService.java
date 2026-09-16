package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.TableCreateUpdateRequest;
import com.hoadiemcat.dto.request.VerifyPasscodeRequest;
import com.hoadiemcat.dto.response.TableQrResponse;
import com.hoadiemcat.dto.response.VerifyPasscodeResponse;
import com.hoadiemcat.entity.enums.TableStatus;

import java.util.List;

public interface TableQrService {

    List<TableQrResponse> getAllTables();

    TableQrResponse getTableById(Long id);

    TableQrResponse getTableByNumber(String tableNumber);

    TableQrResponse createTable(TableCreateUpdateRequest request);

    TableQrResponse updateTable(Long id, TableCreateUpdateRequest request);

    TableQrResponse regeneratePasscode(Long id);

    TableQrResponse toggleOrderLock(Long id);

    TableQrResponse updateTableStatus(Long id, TableStatus status);

    VerifyPasscodeResponse verifyPasscode(String tableIdentifier, VerifyPasscodeRequest request);

    boolean validateSessionToken(String sessionToken);

    void releaseTableSession(Long tableId);
}
