package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.request.TableCreateUpdateRequest;
import com.hoadiemcat.dto.request.VerifyPasscodeRequest;
import com.hoadiemcat.dto.response.TableQrResponse;
import com.hoadiemcat.dto.response.VerifyPasscodeResponse;
import com.hoadiemcat.entity.RestaurantTable;
import com.hoadiemcat.entity.enums.TableArea;
import com.hoadiemcat.entity.enums.TableStatus;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.exception.ErrorCode;
import com.hoadiemcat.repository.RestaurantTableRepository;
import com.hoadiemcat.service.TableQrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.hoadiemcat.dto.response.TableDeviceResponse;
import com.hoadiemcat.entity.TableSessionDevice;
import com.hoadiemcat.repository.TableSessionDeviceRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableQrServiceImpl implements TableQrService {

    private final RestaurantTableRepository tableRepository;
    private final TableSessionDeviceRepository tableSessionDeviceRepository;
    private final com.hoadiemcat.repository.OrderRepository orderRepository;
    private final com.hoadiemcat.repository.OrderItemRepository orderItemRepository;
    private final com.hoadiemcat.repository.CartRepository cartRepository;
    private final com.hoadiemcat.repository.CallStaffLogRepository callStaffLogRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<TableQrResponse> getAllTables() {
        List<RestaurantTable> tables = tableRepository.findAllByOrderByTableNumberAsc();
        if (tables.isEmpty()) {
            tables = seedDefaultTables();
        }

        // Tối ưu hóa N+1 Query: Gom 41 query thành đúng 2 query batch duy nhất
        List<com.hoadiemcat.entity.Order> allOrders = orderRepository.findActiveOrdersByTablesWithItems(tables);
        Map<Long, List<com.hoadiemcat.entity.Order>> ordersByTableId = allOrders.stream()
                .filter(o -> o.getRestaurantTable() != null && o.getRestaurantTable().getId() != null)
                .collect(Collectors.groupingBy(o -> o.getRestaurantTable().getId()));

        List<com.hoadiemcat.entity.CallStaffLog> allPendingLogs = callStaffLogRepository.findByRestaurantTableInAndStatus(
                tables, com.hoadiemcat.entity.enums.CallStaffStatus.PENDING
        );
        Map<Long, List<com.hoadiemcat.entity.CallStaffLog>> logsByTableId = allPendingLogs.stream()
                .filter(l -> l.getRestaurantTable() != null && l.getRestaurantTable().getId() != null)
                .collect(Collectors.groupingBy(l -> l.getRestaurantTable().getId()));

        return tables.stream()
                .map(table -> mapToResponseWithData(
                        table,
                        ordersByTableId.getOrDefault(table.getId(), Collections.emptyList()),
                        logsByTableId.getOrDefault(table.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TableQrResponse getTableById(Long id) {
        RestaurantTable table = findTableEntity(id);
        return mapToResponse(table);
    }

    @Override
    @Transactional(readOnly = true)
    public TableQrResponse getTableByNumber(String tableNumber) {
        RestaurantTable table = findByIdentifier(tableNumber);
        return mapToResponse(table);
    }

    @Override
    @Transactional
    public TableQrResponse createTable(TableCreateUpdateRequest request) {
        if (tableRepository.existsByTableNumber(request.getTableNumber().toUpperCase())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        RestaurantTable table = RestaurantTable.builder()
                .tableNumber(request.getTableNumber().toUpperCase().trim())
                .name(request.getName().trim())
                .area(request.getArea())
                .capacity(request.getCapacity())
                .status(request.getStatus() != null ? request.getStatus() : TableStatus.AVAILABLE)
                .isOrderLocked(request.getIsOrderLocked() != null ? request.getIsOrderLocked() : false)
                .maxActiveDevices(request.getMaxActiveDevices() != null ? request.getMaxActiveDevices() : (int) Math.round(request.getCapacity() * 1.5))
                .activeDeviceCount(0)
                .failedAttempts(0)
                .build();

        table.generateNewPasscode();
        table.setCurrentSessionToken(UUID.randomUUID().toString());
        RestaurantTable saved = tableRepository.save(table);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TableQrResponse updateTable(Long id, TableCreateUpdateRequest request) {
        RestaurantTable table = findTableEntity(id);
        table.setName(request.getName().trim());
        table.setArea(request.getArea());
        table.setCapacity(request.getCapacity());
        if (request.getStatus() != null) {
            table.setStatus(request.getStatus());
        }
        if (request.getIsOrderLocked() != null) {
            table.setIsOrderLocked(request.getIsOrderLocked());
        }
        if (request.getMaxActiveDevices() != null) {
            table.setMaxActiveDevices(request.getMaxActiveDevices());
        }
        RestaurantTable saved = tableRepository.save(table);
        broadcastTableUpdate(saved);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TableQrResponse regeneratePasscode(Long id) {
        RestaurantTable table = findTableEntity(id);
        resetTableToAvailableSession(table);
        RestaurantTable saved = tableRepository.save(table);
        broadcastTableUpdate(saved);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TableQrResponse toggleOrderLock(Long id) {
        RestaurantTable table = findTableEntity(id);
        table.setIsOrderLocked(!Boolean.TRUE.equals(table.getIsOrderLocked()));
        RestaurantTable saved = tableRepository.save(table);
        broadcastTableUpdate(saved);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TableQrResponse setOrderLock(Long id, boolean locked) {
        RestaurantTable table = findTableEntity(id);
        table.setIsOrderLocked(locked);
        RestaurantTable saved = tableRepository.save(table);
        broadcastTableUpdate(saved);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TableQrResponse updateTableStatus(Long id, TableStatus status) {
        RestaurantTable table = findTableEntity(id);
        if (status == TableStatus.AVAILABLE) {
            resetTableToAvailableSession(table);
        } else {
            table.setStatus(status);
        }
        RestaurantTable saved = tableRepository.save(table);
        broadcastTableUpdate(saved);
        return mapToResponse(saved);
    }

    private void broadcastTableUpdate(RestaurantTable table) {
        if (messagingTemplate != null && table != null) {
            try {
                TableQrResponse resp = mapToResponse(table);
                messagingTemplate.convertAndSend("/topic/tables", resp);
                if (table.getTableNumber() != null) {
                    messagingTemplate.convertAndSend("/topic/table/" + table.getTableNumber() + "/status", resp);
                }
                if (table.getCurrentSessionToken() != null) {
                    messagingTemplate.convertAndSend("/topic/table/" + table.getCurrentSessionToken(), resp);
                }
            } catch (Exception e) {
                log.warn("Không thể gửi thông báo WebSocket cập nhật bàn: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public VerifyPasscodeResponse verifyPasscode(String tableIdentifier, VerifyPasscodeRequest request) {
        RestaurantTable rawTable = findByIdentifier(tableIdentifier);
        RestaurantTable table = rawTable.getEffectiveTable();

        // 1. Kiểm tra khóa tạm thời do brute-force
        if (rawTable.isTemporarilyLocked() || table.isTemporarilyLocked()) {
            throw new AppException(ErrorCode.TABLE_LOCKED, "Bàn đang bị tạm khóa 60 giây do nhập sai mã PIN quá 5 lần. Vui lòng thử lại sau!");
        }

        // 2. Kiểm tra giới hạn số thiết bị đồng thời (Anti-abuse)
        int currentDevices = table.getActiveDeviceCount() != null ? table.getActiveDeviceCount() : 0;
        int maxDevices = table.getMaxActiveDevices() != null ? table.getMaxActiveDevices() : 6;
        if (currentDevices >= maxDevices) {
            throw new AppException(ErrorCode.DEVICE_LIMIT_EXCEEDED, "Bàn đã đạt giới hạn thiết bị kết nối đồng thời!");
        }

        // 3. Kiểm tra mã PIN 4 số (chấp nhận mã PIN của bàn phụ hoặc bàn chính)
        String masterPasscode = table.getCurrentPasscode();
        String rawPasscode = rawTable.getCurrentPasscode();
        String inputPass = request.getPasscode() != null ? request.getPasscode().trim() : "";

        boolean matches = (masterPasscode != null && masterPasscode.equals(inputPass))
                || (rawPasscode != null && rawPasscode.equals(inputPass));

        if (!matches) {
            rawTable.recordFailedAttempt();
            tableRepository.save(rawTable);
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã PIN 4 số không chính xác. Vui lòng kiểm tra lại!");
        }

        // 4. Nhập đúng: Reset bộ đếm lỗi & Cấp token phiên
        rawTable.resetFailedAttempts();
        tableRepository.save(rawTable);

        if (table.getCurrentSessionToken() == null) {
            table.setCurrentSessionToken(UUID.randomUUID().toString());
        }

        if (table.getStatus() == TableStatus.AVAILABLE) {
            table.setStatus(TableStatus.OCCUPIED);
            table.setSessionStartedAt(LocalDateTime.now());
        }

        table.setActiveDeviceCount(currentDevices + 1);
        tableRepository.save(table);

        String deviceToken = UUID.randomUUID().toString();

        // 5. Phân quyền Chủ Bàn (Host) vs Thành Viên (Member)
        boolean hasActiveHost = tableSessionDeviceRepository.findFirstByTableAndIsHostTrueAndIsActiveTrue(table).isPresent();
        boolean isHost = !hasActiveHost;

        String deviceName = request.getDeviceName();
        if (deviceName == null || deviceName.isBlank()) {
            deviceName = isHost ? "Chủ Bàn (Thiết bị 1)" : "Thành Viên (Thiết bị " + (currentDevices + 1) + ")";
        }

        TableSessionDevice device = TableSessionDevice.builder()
                .table(table)
                .deviceToken(deviceToken)
                .deviceName(deviceName)
                .deviceFingerprint(request.getDeviceFingerprint())
                .isHost(isHost)
                .isActive(true)
                .connectedAt(LocalDateTime.now())
                .build();
        tableSessionDeviceRepository.save(device);

        String displayTableName = rawTable.isLinked()
                ? "Bàn " + table.getTableNumber() + " (Ghép cùng " + rawTable.getTableNumber() + ")"
                : table.getName();

        return VerifyPasscodeResponse.builder()
                .tableId(table.getId())
                .tableNumber(table.getTableNumber())
                .tableName(displayTableName)
                .sessionToken(table.getCurrentSessionToken())
                .deviceToken(deviceToken)
                .deviceName(deviceName)
                .isHost(isHost)
                .activeDeviceCount(currentDevices + 1)
                .status(table.getStatus())
                .isOrderLocked(table.getIsOrderLocked())
                .message("Xác thực mã PIN bàn thành công" + (rawTable.isLinked() ? " (Đã gia nhập Cụm bàn " + table.getTableNumber() + ")" : ""))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateSessionToken(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) return false;
        return tableRepository.findByCurrentSessionToken(sessionToken)
                .map(t -> t.getStatus() == TableStatus.OCCUPIED)
                .orElse(false);
    }

    @Override
    @Transactional
    public void releaseTableSession(Long tableId) {
        RestaurantTable table = findTableEntity(tableId);

        // 1. Nếu bàn này là Master Table: Tự động giải phóng toàn bộ bàn phụ (Slaves) liên kết
        List<RestaurantTable> linkedSlaves = tableRepository.findByMasterTable(table);
        if (linkedSlaves != null && !linkedSlaves.isEmpty()) {
            for (RestaurantTable slave : linkedSlaves) {
                slave.setMasterTable(null);
                resetTableToAvailableSession(slave);
                tableRepository.save(slave);
                broadcastTableUpdate(slave);
                log.info("Cụm bàn Master {} thanh toán/kết thúc: Tự động giải phóng bàn phụ {}", table.getTableNumber(), slave.getTableNumber());
            }
        }

        // 2. Nếu bàn này là Slave Table: Gỡ liên kết khỏi Master
        if (table.isLinked()) {
            table.setMasterTable(null);
        }

        resetTableToAvailableSession(table);
        tableRepository.save(table);
        broadcastTableUpdate(table);
    }

    private void resetTableToAvailableSession(RestaurantTable table) {
        table.setStatus(TableStatus.AVAILABLE);
        table.setIsOrderLocked(false);
        table.setCurrentSessionToken(UUID.randomUUID().toString()); // Token mới vô hiệu hóa toàn bộ token cũ
        table.generateNewPasscode(); // Sinh PIN mới cho lượt khách kế tiếp
        table.setActiveDeviceCount(0);
        table.setSessionStartedAt(null);
        table.resetFailedAttempts();
        table.setMasterTable(null);
        int baseCap = table.getCapacity() != null ? table.getCapacity() : 4;
        table.setMaxActiveDevices((int) Math.round(baseCap * 1.5));

        // Vô hiệu hóa toàn bộ thiết bị cũ của bàn
        List<TableSessionDevice> oldDevices = tableSessionDeviceRepository.findByTable(table);
        for (TableSessionDevice d : oldDevices) {
            d.setIsActive(false);
        }
        tableSessionDeviceRepository.saveAll(oldDevices);

        // Xóa sạch toàn bộ các đợt order và log order-item của bàn để sẵn sàng đón lượt khách mới
        cleanTableOrdersAndLogs(table);
    }

    /**
     * Dọn sạch toàn bộ log đơn hàng, order-item, giỏ hàng và chuông gọi phục vụ của bàn khi bàn trở về AVAILABLE
     */
    private void cleanTableOrdersAndLogs(RestaurantTable table) {
        if (table == null) return;
        try {
            // 1. Xóa toàn bộ các đợt order và từng chi tiết món ăn (order_items) của bàn
            List<com.hoadiemcat.entity.Order> orders = orderRepository.findByRestaurantTableOrderByCreatedAtAsc(table);
            if (orders != null && !orders.isEmpty()) {
                for (com.hoadiemcat.entity.Order o : orders) {
                    if (o.getOrderItems() != null && !o.getOrderItems().isEmpty()) {
                        orderItemRepository.deleteAll(o.getOrderItems());
                        o.getOrderItems().clear();
                    }
                }
                orderRepository.deleteAll(orders);
                log.info("🧹 Đã dọn sạch {} đợt order và toàn bộ order-items của bàn {} ({}) để sẵn sàng đón lượt khách mới",
                        orders.size(), table.getTableNumber(), table.getName());
            }

            // 2. Xóa giỏ hàng chưa chốt (nếu có)
            if (cartRepository != null) {
                cartRepository.deleteByRestaurantTable(table);
            }

            // 3. Giải quyết toàn bộ chuông gọi phục vụ / yêu cầu thanh toán còn tồn đọng của bàn
            if (callStaffLogRepository != null) {
                List<com.hoadiemcat.entity.CallStaffLog> pendingLogs = callStaffLogRepository.findByRestaurantTableAndStatus(
                        table, com.hoadiemcat.entity.enums.CallStaffStatus.PENDING
                );
                if (pendingLogs != null && !pendingLogs.isEmpty()) {
                    for (com.hoadiemcat.entity.CallStaffLog l : pendingLogs) {
                        l.setStatus(com.hoadiemcat.entity.enums.CallStaffStatus.RESOLVED);
                        l.setResolvedAt(LocalDateTime.now());
                    }
                    callStaffLogRepository.saveAll(pendingLogs);
                }
            }

            // 4. Phát tín hiệu WebSocket thông báo KDS và Bàn ăn làm sạch dữ liệu
            if (messagingTemplate != null) {
                try {
                    messagingTemplate.convertAndSend("/topic/kitchen/orders", java.util.Map.of(
                            "type", "TABLE_RESET_AVAILABLE",
                            "tableId", table.getId() != null ? table.getId() : 0,
                            "tableNumber", table.getTableNumber() != null ? table.getTableNumber() : ""
                    ));
                    if (table.getTableNumber() != null) {
                        messagingTemplate.convertAndSend("/topic/table/" + table.getTableNumber() + "/status", java.util.Map.of(
                                "type", "TABLE_RESET_AVAILABLE",
                                "status", TableStatus.AVAILABLE.name()
                        ));
                    }
                } catch (Exception wsEx) {
                    log.warn("Không thể gửi thông báo WebSocket dọn dẹp bàn {}: {}", table.getTableNumber(), wsEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp order và logs của bàn {}: {}", table.getTableNumber(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableDeviceResponse> getActiveDevices(String tableIdentifier, String currentDeviceToken) {
        RestaurantTable table = findByIdentifier(tableIdentifier);
        List<TableSessionDevice> devices = tableSessionDeviceRepository.findByTableAndIsActiveTrueOrderByConnectedAtAsc(table);
        return devices.stream().map(d -> TableDeviceResponse.builder()
                .deviceToken(d.getDeviceToken())
                .deviceName(d.getDeviceName())
                .isHost(d.getIsHost())
                .isActive(d.getIsActive())
                .connectedAt(d.getConnectedAt())
                .isCurrentDevice(currentDeviceToken != null && currentDeviceToken.equals(d.getDeviceToken()))
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void kickDevice(String tableIdentifier, String hostDeviceToken, String targetDeviceToken) {
        RestaurantTable table = findByIdentifier(tableIdentifier);

        // 1. Kiểm tra người gọi phải là Host của bàn
        TableSessionDevice hostDevice = tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue(hostDeviceToken)
                .orElseThrow(() -> new AppException(ErrorCode.HOST_PERMISSION_REQUIRED));
        if (!Boolean.TRUE.equals(hostDevice.getIsHost()) || !java.util.Objects.equals(hostDevice.getTable().getId(), table.getId())) {
            throw new AppException(ErrorCode.HOST_PERMISSION_REQUIRED);
        }

        // 2. Không thể tự đá chính mình
        if (hostDeviceToken.equals(targetDeviceToken)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // 3. Tìm thiết bị mục tiêu và vô hiệu hóa
        TableSessionDevice targetDevice = tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue(targetDeviceToken)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));
        if (!java.util.Objects.equals(targetDevice.getTable().getId(), table.getId())) {
            throw new AppException(ErrorCode.DEVICE_NOT_FOUND);
        }

        targetDevice.setIsActive(false);
        tableSessionDeviceRepository.save(targetDevice);

        int newCount = Math.max(0, (table.getActiveDeviceCount() != null ? table.getActiveDeviceCount() : 1) - 1);
        table.setActiveDeviceCount(newCount);
        tableRepository.save(table);
    }

    @Override
    @Transactional
    public void transferHost(String tableIdentifier, String currentHostToken, String newHostToken) {
        RestaurantTable table = findByIdentifier(tableIdentifier);

        // 1. Xác thực người chuyển phải là Host hiện tại
        TableSessionDevice currentHost = tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue(currentHostToken)
                .orElseThrow(() -> new AppException(ErrorCode.HOST_PERMISSION_REQUIRED));
        if (!Boolean.TRUE.equals(currentHost.getIsHost()) || !java.util.Objects.equals(currentHost.getTable().getId(), table.getId())) {
            throw new AppException(ErrorCode.HOST_PERMISSION_REQUIRED);
        }

        // 2. Tìm thiết bị mới
        TableSessionDevice newHost = tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue(newHostToken)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));
        if (!java.util.Objects.equals(newHost.getTable().getId(), table.getId())) {
            throw new AppException(ErrorCode.DEVICE_NOT_FOUND);
        }

        currentHost.setIsHost(false);
        newHost.setIsHost(true);
        tableSessionDeviceRepository.save(currentHost);
        tableSessionDeviceRepository.save(newHost);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableDeviceResponse> getAdminActiveDevices(Long tableId) {
        RestaurantTable table = findTableEntity(tableId);
        return tableSessionDeviceRepository.findByTableAndIsActiveTrueOrderByConnectedAtAsc(table).stream()
                .map(d -> TableDeviceResponse.builder()
                        .deviceToken(d.getDeviceToken())
                        .deviceName(d.getDeviceName())
                        .isHost(d.getIsHost())
                        .isActive(d.getIsActive())
                        .connectedAt(d.getConnectedAt())
                        .isCurrentDevice(false)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void adminResetHost(Long tableId) {
        RestaurantTable table = findTableEntity(tableId);
        List<TableSessionDevice> devices = tableSessionDeviceRepository.findByTableAndIsActiveTrueOrderByConnectedAtAsc(table);
        if (!devices.isEmpty()) {
            for (int i = 0; i < devices.size(); i++) {
                devices.get(i).setIsHost(i == 0);
            }
            tableSessionDeviceRepository.saveAll(devices);
        }
    }

    @Override
    @Transactional
    public void adminKickDevice(Long tableId, String targetDeviceToken) {
        RestaurantTable table = findTableEntity(tableId);
        TableSessionDevice target = tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue(targetDeviceToken)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));
        if (java.util.Objects.equals(target.getTable().getId(), table.getId())) {
            target.setIsActive(false);
            tableSessionDeviceRepository.save(target);

            int newCount = Math.max(0, (table.getActiveDeviceCount() != null ? table.getActiveDeviceCount() : 1) - 1);
            table.setActiveDeviceCount(newCount);
            tableRepository.save(table);

            if (Boolean.TRUE.equals(target.getIsHost())) {
                adminResetHost(tableId);
            }
            broadcastTableUpdate(table);
        }
    }

    private RestaurantTable findTableEntity(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private RestaurantTable findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        String clean = identifier.trim().toUpperCase();

        // 1. Tìm chính xác theo tableNumber (VD: "B01", "VIP12")
        var opt = tableRepository.findByTableNumber(clean);
        if (opt.isPresent()) return opt.get();

        // 2. Nếu là số thuần túy (VD: "1", "01", "8") -> chuẩn hóa thành "B01", "B08"
        if (clean.matches("^\\d+$")) {
            int num = Integer.parseInt(clean);
            String normalizedNum = String.format("B%02d", num);
            var optNorm = tableRepository.findByTableNumber(normalizedNum);
            if (optNorm.isPresent()) return optNorm.get();

            return tableRepository.findById((long) num)
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        }

        throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Override
    @Transactional
    public void callStaff(String tableIdentifier, com.hoadiemcat.entity.enums.CallStaffType type, String message) {
        RestaurantTable table = findByIdentifier(tableIdentifier);
        com.hoadiemcat.entity.CallStaffLog logEntity = com.hoadiemcat.entity.CallStaffLog.builder()
                .restaurantTable(table)
                .requestType(type != null ? type : com.hoadiemcat.entity.enums.CallStaffType.CALL_STAFF)
                .message(message)
                .status(com.hoadiemcat.entity.enums.CallStaffStatus.PENDING)
                .build();
        callStaffLogRepository.save(logEntity);
        broadcastTableUpdate(table);
    }

    @Override
    @Transactional
    public void resolveCallStaff(Long tableId) {
        resolveCallStaff(tableId, null);
    }

    @Override
    @Transactional
    public void resolveCallStaff(Long tableId, com.hoadiemcat.entity.enums.CallStaffType type) {
        RestaurantTable table = findTableEntity(tableId);
        List<com.hoadiemcat.entity.CallStaffLog> pendingLogs = callStaffLogRepository.findByRestaurantTableAndStatus(
                table, com.hoadiemcat.entity.enums.CallStaffStatus.PENDING
        );
        List<com.hoadiemcat.entity.CallStaffLog> targetLogs = (type != null)
                ? pendingLogs.stream().filter(l -> l.getRequestType() == type).collect(Collectors.toList())
                : pendingLogs;

        for (com.hoadiemcat.entity.CallStaffLog l : targetLogs) {
            l.setStatus(com.hoadiemcat.entity.enums.CallStaffStatus.RESOLVED);
            l.setResolvedAt(LocalDateTime.now());
        }
        callStaffLogRepository.saveAll(targetLogs);
        broadcastTableUpdate(table);
    }

    private TableQrResponse mapToResponse(RestaurantTable table) {
        List<com.hoadiemcat.entity.Order> orders = orderRepository.findByRestaurantTableOrderByCreatedAtAsc(table);
        List<com.hoadiemcat.entity.CallStaffLog> pendingLogs = callStaffLogRepository.findByRestaurantTableAndStatus(
                table, com.hoadiemcat.entity.enums.CallStaffStatus.PENDING
        );
        return mapToResponseWithData(table, orders, pendingLogs);
    }

    private TableQrResponse mapToResponseWithData(
            RestaurantTable table,
            List<com.hoadiemcat.entity.Order> orders,
            List<com.hoadiemcat.entity.CallStaffLog> pendingLogs
    ) {
        String baseUrl = "https://hoadiemcat.vn/table/" + table.getTableNumber();

        java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        int activeOrderCount = 0;
        int activeItemCount = 0;
        boolean hasCallStaff = false;
        boolean isPaying = false;

        try {
            List<com.hoadiemcat.entity.Order> activeOrders = (orders != null ? orders : Collections.<com.hoadiemcat.entity.Order>emptyList()).stream()
                    .filter(o -> o.getStatus() != com.hoadiemcat.entity.enums.OrderStatus.CANCELLED)
                    .collect(Collectors.toList());

            activeOrderCount = activeOrders.size();
            totalAmount = activeOrders.stream()
                    .flatMap(o -> o.getOrderItems() != null ? o.getOrderItems().stream() : java.util.stream.Stream.empty())
                    .map(item -> item.getPrice() != null && item.getQuantity() != null
                            ? item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()))
                            : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            activeItemCount = activeOrders.stream()
                    .mapToInt(o -> o.getOrderItems() != null ? o.getOrderItems().size() : 0)
                    .sum();

            if (pendingLogs != null) {
                hasCallStaff = pendingLogs.stream().anyMatch(l -> l.getRequestType() == com.hoadiemcat.entity.enums.CallStaffType.CALL_STAFF);
                isPaying = pendingLogs.stream().anyMatch(l -> l.getRequestType() == com.hoadiemcat.entity.enums.CallStaffType.PAYMENT_REQUEST);
            }
        } catch (Exception e) {
            log.warn("Lỗi tính toán dữ liệu đơn hàng bàn {}: {}", table.getTableNumber(), e.getMessage());
        }

        Long masterId = table.getMasterTable() != null ? table.getMasterTable().getId() : null;
        String masterNum = table.getMasterTable() != null ? table.getMasterTable().getTableNumber() : null;
        boolean isMaster = table.isMaster();
        boolean isLinked = table.isLinked();
        List<String> linkedNums = isMaster && table.getLinkedTables() != null
                ? table.getLinkedTables().stream().map(RestaurantTable::getTableNumber).collect(Collectors.toList())
                : Collections.emptyList();

        return TableQrResponse.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .name(table.getName())
                .area(table.getArea())
                .capacity(table.getCapacity())
                .status(table.getStatus())
                .isOrderLocked(Boolean.TRUE.equals(table.getIsOrderLocked()))
                .currentSessionToken(table.getCurrentSessionToken())
                .currentPasscode(table.getCurrentPasscode())
                .qrCodeUrl(table.getQrCodeUrl())
                .qrEntryUrl(baseUrl)
                .activeDeviceCount(table.getActiveDeviceCount() != null ? table.getActiveDeviceCount() : 0)
                .maxActiveDevices(table.getMaxActiveDevices() != null ? table.getMaxActiveDevices() : 6)
                .isTemporarilyLocked(table.isTemporarilyLocked())
                .failedAttempts(table.getFailedAttempts() != null ? table.getFailedAttempts() : 0)
                .sessionStartedAt(table.getSessionStartedAt())
                .totalAmount(totalAmount)
                .activeOrderCount(activeOrderCount)
                .activeItemCount(activeItemCount)
                .hasCallStaff(hasCallStaff)
                .isPaying(isPaying)
                .masterTableId(masterId)
                .masterTableNumber(masterNum)
                .isMaster(isMaster)
                .isLinked(isLinked)
                .linkedTableNumbers(linkedNums)
                .build();
    }

    private List<RestaurantTable> seedDefaultTables() {
        log.info("Seeding default 20 restaurant tables with initial passcodes...");
        List<RestaurantTable> list = new ArrayList<>();

        // 10 Bàn Sảnh Chung (B01 - B10)
        for (int i = 1; i <= 10; i++) {
            String num = String.format("B%02d", i);
            RestaurantTable t = RestaurantTable.builder()
                    .tableNumber(num)
                    .name("Bàn " + String.format("%02d", i))
                    .area(TableArea.COMMON)
                    .capacity(4)
                    .status(i == 2 || i == 3 || i == 4 || i == 7 || i == 8 || i == 9 ? TableStatus.OCCUPIED : (i == 5 ? TableStatus.CLEANING : TableStatus.AVAILABLE))
                    .isOrderLocked(false)
                    .maxActiveDevices(6)
                    .activeDeviceCount(i == 4 || i == 8 ? 3 : (i == 2 ? 2 : 0))
                    .failedAttempts(0)
                    .build();
            t.generateNewPasscode();
            t.setCurrentSessionToken(UUID.randomUUID().toString());
            if (t.getStatus() == TableStatus.OCCUPIED) {
                t.setSessionStartedAt(LocalDateTime.now().minusMinutes(15 * i));
            }
            list.add(t);
        }

        // 10 Phòng VIP Hoàng Gia (VIP11 - VIP20)
        for (int i = 11; i <= 20; i++) {
            String num = "VIP" + i;
            RestaurantTable t = RestaurantTable.builder()
                    .tableNumber(num)
                    .name("Phòng VIP " + i)
                    .area(TableArea.VIP)
                    .capacity(10)
                    .status(i == 11 || i == 12 || i == 14 || i == 15 || i == 17 || i == 19 || i == 20 ? TableStatus.OCCUPIED : (i == 16 ? TableStatus.CLEANING : TableStatus.AVAILABLE))
                    .isOrderLocked(false)
                    .maxActiveDevices(15)
                    .activeDeviceCount(i == 12 ? 8 : (i == 11 ? 5 : 0))
                    .failedAttempts(0)
                    .build();
            t.generateNewPasscode();
            t.setCurrentSessionToken(UUID.randomUUID().toString());
            if (t.getStatus() == TableStatus.OCCUPIED) {
                t.setSessionStartedAt(LocalDateTime.now().minusMinutes(20 * (i - 10)));
            }
            list.add(t);
        }

        return tableRepository.saveAll(list);
    }
}
