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

    List<com.hoadiemcat.dto.response.TableDeviceResponse> getActiveDevices(String tableIdentifier, String currentDeviceToken);

    void kickDevice(String tableIdentifier, String hostDeviceToken, String targetDeviceToken);

    void transferHost(String tableIdentifier, String currentHostToken, String newHostToken);

    List<com.hoadiemcat.dto.response.TableDeviceResponse> getAdminActiveDevices(Long tableId);

    void adminResetHost(Long tableId);

    void adminKickDevice(Long tableId, String targetDeviceToken);

    void callStaff(String tableIdentifier, com.hoadiemcat.entity.enums.CallStaffType type, String message);

    void resolveCallStaff(Long tableId);

    void resolveCallStaff(Long tableId, com.hoadiemcat.entity.enums.CallStaffType type);
}
