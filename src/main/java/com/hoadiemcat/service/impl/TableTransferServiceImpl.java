package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.request.DraftCartItemRequest;
import com.hoadiemcat.dto.request.TableDirectTransferRequest;
import com.hoadiemcat.dto.request.TableTransferConfirmRequest;
import com.hoadiemcat.dto.request.TableTransferRequest;
import com.hoadiemcat.dto.response.DraftCartItemResponse;
import com.hoadiemcat.dto.response.TableTransferConfirmResponse;
import com.hoadiemcat.dto.response.TableTransferResponse;
import com.hoadiemcat.entity.*;
import com.hoadiemcat.entity.enums.*;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.exception.ErrorCode;
import com.hoadiemcat.exception.ResourceNotFoundException;
import com.hoadiemcat.repository.*;
import com.hoadiemcat.service.TableTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableTransferServiceImpl implements TableTransferService {

    private final TableTransferRepository tableTransferRepository;
    private final RestaurantTableRepository tableRepository;
    private final TableSessionDeviceRepository tableSessionDeviceRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final MenuItemRepository menuItemRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int TRANSFER_TTL_MINUTES = 5;
    private final SecureRandom secureRandom = new SecureRandom();

    private String normalizeTableNumber(String raw) {
        if (raw == null || raw.isBlank()) return "B01";
        String normalized = raw.trim().toUpperCase();
        if (normalized.startsWith("VIP")) {
            String digits = normalized.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? normalized : "VIP" + digits;
        }
        if (normalized.startsWith("BÀN ") || normalized.startsWith("BAN ")) {
            String digits = normalized.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    return "B" + String.format("%02d", Integer.parseInt(digits));
                } catch (Exception ignored) {}
            }
            return "B" + digits;
        }
        if (normalized.startsWith("B")) {
            String digits = normalized.substring(1).trim();
            if (digits.matches("^\\d+$")) {
                try {
                    return "B" + String.format("%02d", Integer.parseInt(digits));
                } catch (Exception ignored) {}
            }
            return normalized;
        }
        if (normalized.matches("^\\d+$")) {
            try {
                return "B" + String.format("%02d", Integer.parseInt(normalized));
            } catch (Exception ignored) {}
        }
        return normalized;
    }

    private RestaurantTable findTableByNumber(String rawTableNumber) {
        String normalized = normalizeTableNumber(rawTableNumber);
        return tableRepository.findByTableNumber(normalized)
                .or(() -> tableRepository.findByName(normalized))
                .or(() -> tableRepository.findByName(rawTableNumber.trim()))
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", "tableNumber", normalized));
    }

    private String generateTransferCode() {
        String code;
        do {
            int num = 1000 + secureRandom.nextInt(9000);
            code = "TRF-" + num;
        } while (tableTransferRepository.existsByTransferCode(code));
        return code;
    }

    @Override
    @Transactional
    public TableTransferResponse requestTransfer(TableTransferRequest request, String deviceToken, String sessionToken) {
        RestaurantTable sourceTable = findTableByNumber(request.getSourceTableNumber());

        // 1. Kiểm tra trạng thái bàn nguồn
        if (sourceTable.getStatus() != TableStatus.OCCUPIED) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chỉ có thể chuyển hoặc ghép bàn khi bàn đang có khách");
        }

        // 2. Kiểm tra bàn có đang trong tiến trình thanh toán không
        boolean hasPendingInvoice = invoiceRepository.findByRestaurantTableId(sourceTable.getId()).stream()
                .anyMatch(inv -> inv.getPaymentStatus() == PaymentStatus.PENDING);
        if (hasPendingInvoice) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bàn đang trong quy trình thanh toán, không thể chuyển/ghép bàn");
        }

        // 3. Phân quyền: Kiểm tra thiết bị có phải là Chủ Bàn (Host) không
        if (deviceToken != null && !deviceToken.isBlank()) {
            tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue(deviceToken).ifPresent(device -> {
                if (device.getTable().getId().equals(sourceTable.getId()) && !Boolean.TRUE.equals(device.getIsHost())) {
                    throw new AppException(ErrorCode.HOST_PERMISSION_REQUIRED, "Chỉ Chủ Bàn (Host) mới có quyền yêu cầu chuyển hoặc ghép bàn");
                }
            });
        }

        // 4. Kiểm tra xem bàn có mã PENDING nào còn hiệu lực không
        Optional<TableTransfer> existingOpt = tableTransferRepository.findFirstBySourceTableAndStatus(sourceTable, TransferStatus.PENDING);
        if (existingOpt.isPresent()) {
            TableTransfer existing = existingOpt.get();
            if (!existing.isExpired()) {
                // Đồng bộ cập nhật giỏ hàng nếu khách thêm món mới trong khi mã còn hiệu lực
                syncDraftCart(sourceTable, request.getDraftCartItems());

                long remainingSeconds = Duration.between(LocalDateTime.now(), existing.getExpiresAt()).getSeconds();
                return TableTransferResponse.builder()
                        .transferCode(existing.getTransferCode())
                        .transferType(existing.getTransferType())
                        .sourceTableNumber(sourceTable.getTableNumber())
                        .sourceTableName(sourceTable.getName())
                        .status(existing.getStatus())
                        .expiresAt(existing.getExpiresAt())
                        .ttlSeconds(Math.max(0, remainingSeconds))
                        .message("Bàn đang có mã chuyển còn hiệu lực")
                        .build();
            } else {
                existing.setStatus(TransferStatus.EXPIRED);
                tableTransferRepository.save(existing);
            }
        }

        // 5. Sinh mã chuyển bàn mới
        String transferCode = generateTransferCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TRANSFER_TTL_MINUTES);

        // 6. Khóa tạm thời quyền gọi món của bàn nguồn để bảo toàn giỏ hàng
        sourceTable.setIsOrderLocked(true);
        tableRepository.save(sourceTable);

        // Đồng bộ giỏ hàng nháp từ client vào Database để bảo toàn dữ liệu trước khi di chuyển
        syncDraftCart(sourceTable, request.getDraftCartItems());

        TableTransfer transfer = TableTransfer.builder()
                .transferCode(transferCode)
                .transferType(request.getTransferType())
                .sourceTable(sourceTable)
                .sourceSessionToken(sourceTable.getCurrentSessionToken() != null ? sourceTable.getCurrentSessionToken() : "SESSION_" + sourceTable.getTableNumber())
                .createdByDevice(deviceToken != null ? deviceToken : "SYSTEM")
                .status(TransferStatus.PENDING)
                .expiresAt(expiresAt)
                .reason(request.getReason())
                .build();

        tableTransferRepository.save(transfer);

        // Phát WebSocket cập nhật sơ đồ bàn
        broadcastTableUpdate(sourceTable);

        log.info("Đã tạo mã chuyển bàn {} (loại: {}) cho bàn {}", transferCode, request.getTransferType(), sourceTable.getTableNumber());

        return TableTransferResponse.builder()
                .transferCode(transferCode)
                .transferType(transfer.getTransferType())
                .sourceTableNumber(sourceTable.getTableNumber())
                .sourceTableName(sourceTable.getName())
                .status(transfer.getStatus())
                .expiresAt(expiresAt)
                .ttlSeconds((long) TRANSFER_TTL_MINUTES * 60)
                .message("Tạo mã chuyển bàn thành công. Mã có hiệu lực trong " + TRANSFER_TTL_MINUTES + " phút.")
                .build();
    }

    @Override
    @Transactional
    public TableTransferConfirmResponse confirmTransfer(TableTransferConfirmRequest request, String deviceToken) {
        String cleanCode = request.getTransferCode().trim().toUpperCase();
        TableTransfer transfer = tableTransferRepository.findByTransferCode(cleanCode)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_CODE_INVALID, "Mã chuyển bàn không tồn tại hoặc không hợp lệ"));

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new AppException(ErrorCode.TRANSFER_CODE_INVALID, "Mã chuyển bàn đã được sử dụng hoặc đã bị hủy");
        }

        RestaurantTable sourceTable = transfer.getSourceTable();

        // Kiểm tra hết hạn TTL 5 phút
        if (transfer.isExpired()) {
            transfer.setStatus(TransferStatus.EXPIRED);
            tableTransferRepository.save(transfer);

            sourceTable.setIsOrderLocked(false);
            tableRepository.save(sourceTable);
            broadcastTableUpdate(sourceTable);

            throw new AppException(ErrorCode.TRANSFER_CODE_EXPIRED, "Mã chuyển bàn đã quá hạn 5 phút. Vui lòng tạo mã mới!");
        }

        RestaurantTable targetTable = findTableByNumber(request.getTargetTableNumber());

        // Kiểm tra không được chuyển vào chính nó
        if (sourceTable.getId().equals(targetTable.getId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể chuyển hoặc ghép vào chính bàn hiện tại");
        }

        String oldSessionToken = sourceTable.getCurrentSessionToken();

        // Đảm bảo targetTable có Session Token
        if (targetTable.getCurrentSessionToken() == null) {
            targetTable.setCurrentSessionToken(UUID.randomUUID().toString());
        }
        String newSessionToken = targetTable.getCurrentSessionToken();

        int activeOrderRounds = 0;

        if (transfer.getTransferType() == TransferType.MOVE) {
            // Nghiệp vụ 1: CHUYỂN BÀN (MOVE: 1 -> 1)
            // Bàn đích bắt buộc phải đang AVAILABLE
            if (targetTable.getStatus() != TableStatus.AVAILABLE) {
                throw new AppException(ErrorCode.TARGET_TABLE_NOT_AVAILABLE, "Bàn đích hiện đang có khách ngồi hoặc đang dọn dẹp, vui lòng chọn bàn trống khác!");
            }

            // 1. Chuyển giỏ hàng nháp (Cart): Dọn dẹp giỏ tồn đọng ở bàn đích trước
            cartRepository.findByRestaurantTable(targetTable).ifPresent(cartRepository::delete);

            Optional<Cart> sourceCartOpt = cartRepository.findByRestaurantTable(sourceTable);
            if (sourceCartOpt.isPresent()) {
                Cart sourceCart = sourceCartOpt.get();
                sourceCart.setRestaurantTable(targetTable);
                sourceCart.setSessionToken(newSessionToken);
                cartRepository.save(sourceCart);
            }

            // 2. Chuyển các đợt gọi món đã đặt (Orders)
            List<Order> sourceOrders = orderRepository.findByRestaurantTableOrderByCreatedAtAsc(sourceTable);
            for (Order o : sourceOrders) {
                if (o.getStatus() != OrderStatus.CANCELLED) {
                    o.setRestaurantTable(targetTable);
                    o.setSessionToken(newSessionToken);
                    orderRepository.save(o);
                    activeOrderRounds++;
                }
            }

            // 3. Cập nhật trạng thái Bàn Đích
            targetTable.setStatus(TableStatus.OCCUPIED);
            targetTable.setSessionStartedAt(sourceTable.getSessionStartedAt() != null ? sourceTable.getSessionStartedAt() : LocalDateTime.now());
            targetTable.setIsOrderLocked(false);
            targetTable.setActiveDeviceCount(1);
            tableRepository.save(targetTable);

            // 4. Giải phóng Bàn Nguồn về trạng thái AVAILABLE / CLEANING
            resetSourceTableSession(sourceTable);

        } else {
            // Nghiệp vụ 2: GHÉP BÀN (MERGE: N -> 1)
            // 1. Hợp nhất giỏ hàng (Cart Merge Algorithm)
            Optional<Cart> sourceCartOpt = cartRepository.findByRestaurantTable(sourceTable);
            Optional<Cart> targetCartOpt = cartRepository.findByRestaurantTable(targetTable);

            if (sourceCartOpt.isPresent() && targetCartOpt.isPresent()) {
                Cart sourceCart = sourceCartOpt.get();
                Cart targetCart = targetCartOpt.get();

                for (CartItem sItem : sourceCart.getItems()) {
                    String sNote = sItem.getNote() != null ? sItem.getNote().trim() : "";
                    Optional<CartItem> matchOpt = targetCart.getItems().stream()
                            .filter(tItem -> tItem.getMenuItem().getId().equals(sItem.getMenuItem().getId()) &&
                                    (Objects.equals(tItem.getNote() != null ? tItem.getNote().trim() : "", sNote)))
                            .findFirst();

                    if (matchOpt.isPresent()) {
                        CartItem match = matchOpt.get();
                        match.setQuantity(Math.min(match.getQuantity() + sItem.getQuantity(), 99)); // QĐ9
                    } else {
                        CartItem newItem = CartItem.builder()
                                .cart(targetCart)
                                .menuItem(sItem.getMenuItem())
                                .quantity(Math.min(sItem.getQuantity(), 99))
                                .note(sItem.getNote())
                                .build();
                        targetCart.addItem(newItem);
                    }
                }
                cartRepository.save(targetCart);
                cartRepository.delete(sourceCart);

            } else if (sourceCartOpt.isPresent()) {
                Cart sourceCart = sourceCartOpt.get();
                sourceCart.setRestaurantTable(targetTable);
                sourceCart.setSessionToken(newSessionToken);
                cartRepository.save(sourceCart);
            }

            // 2. Hợp nhất các đợt gọi món đã đặt (Orders)
            List<Order> targetOrders = orderRepository.findByRestaurantTableOrderByCreatedAtAsc(targetTable);
            int maxRound = targetOrders.stream().mapToInt(Order::getRoundNumber).max().orElse(0);

            List<Order> sourceOrders = orderRepository.findByRestaurantTableOrderByCreatedAtAsc(sourceTable);
            for (Order o : sourceOrders) {
                if (o.getStatus() != OrderStatus.CANCELLED) {
                    o.setRestaurantTable(targetTable);
                    o.setSessionToken(newSessionToken);
                    o.setRoundNumber(maxRound + o.getRoundNumber());
                    String originalNote = o.getNote() != null ? o.getNote() + " " : "";
                    o.setNote(originalNote + "[Ghép từ " + sourceTable.getTableNumber() + "]");
                    orderRepository.save(o);
                    activeOrderRounds++;
                }
            }

            // 3. Cập nhật Bàn Đích
            if (targetTable.getStatus() == TableStatus.AVAILABLE) {
                targetTable.setStatus(TableStatus.OCCUPIED);
                targetTable.setSessionStartedAt(LocalDateTime.now());
            }
            targetTable.setActiveDeviceCount((targetTable.getActiveDeviceCount() != null ? targetTable.getActiveDeviceCount() : 0) + 1);
            targetTable.setIsOrderLocked(false);
            tableRepository.save(targetTable);

            // 4. Giải phóng Bàn Nguồn
            resetSourceTableSession(sourceTable);
        }

        // 5. Cấp phát quyền thiết bị tại bàn mới cho người nhập mã
        String effectiveDeviceToken = (deviceToken != null && !deviceToken.isBlank()) ? deviceToken : UUID.randomUUID().toString();
        boolean hasHost = tableSessionDeviceRepository.findFirstByTableAndIsHostTrueAndIsActiveTrue(targetTable).isPresent();
        boolean isHost = !hasHost;

        String deviceName = request.getDeviceName();
        if (deviceName == null || deviceName.isBlank()) {
            deviceName = isHost ? "Chủ Bàn (Thiết bị chuyển)" : "Thành Viên (" + targetTable.getTableNumber() + ")";
        }

        TableSessionDevice newDevice = TableSessionDevice.builder()
                .table(targetTable)
                .deviceToken(effectiveDeviceToken)
                .deviceName(deviceName)
                .deviceFingerprint(request.getDeviceFingerprint())
                .isHost(isHost)
                .isActive(true)
                .connectedAt(LocalDateTime.now())
                .build();
        tableSessionDeviceRepository.save(newDevice);

        // 6. Hoàn tất giao dịch TableTransfer
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setTargetTable(targetTable);
        transfer.setTargetSessionToken(newSessionToken);
        tableTransferRepository.save(transfer);

        // Lấy danh sách món trong giỏ hàng hiện tại của bàn đích sau khi chuyển/ghép
        List<DraftCartItemResponse> finalCartItems = getDraftCartItemResponses(targetTable);

        // 7. Phát các sự kiện WebSocket đồng bộ toàn hệ thống
        broadcastTransferEvents(sourceTable, targetTable, oldSessionToken, newSessionToken, finalCartItems);

        log.info("Chuyển/ghép bàn thành công từ {} sang {} với mã {}", sourceTable.getTableNumber(), targetTable.getTableNumber(), cleanCode);

        return TableTransferConfirmResponse.builder()
                .newTableId(targetTable.getId())
                .newTableNumber(targetTable.getTableNumber())
                .newTableName(targetTable.getName())
                .newSessionToken(newSessionToken)
                .deviceToken(effectiveDeviceToken)
                .deviceName(deviceName)
                .isHost(isHost)
                .cartItemCount(finalCartItems.size())
                .cartItems(finalCartItems)
                .activeOrderRounds(activeOrderRounds)
                .message("Chuyển bàn sang " + targetTable.getTableNumber() + " thành công!")
                .build();
    }

    @Override
    @Transactional
    public TableTransferResponse cancelTransfer(String transferCode, String deviceToken) {
        String cleanCode = transferCode.trim().toUpperCase();
        TableTransfer transfer = tableTransferRepository.findByTransferCode(cleanCode)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_CODE_INVALID, "Mã chuyển bàn không tồn tại"));

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã chuyển bàn đã được xử lý hoặc đã kết thúc");
        }

        RestaurantTable sourceTable = transfer.getSourceTable();

        transfer.setStatus(TransferStatus.CANCELLED);
        tableTransferRepository.save(transfer);

        // Mở khóa lại bàn nguồn
        sourceTable.setIsOrderLocked(false);
        tableRepository.save(sourceTable);
        broadcastTableUpdate(sourceTable);

        log.info("Đã hủy yêu cầu chuyển bàn {} cho bàn {}", cleanCode, sourceTable.getTableNumber());

        return TableTransferResponse.builder()
                .transferCode(cleanCode)
                .transferType(transfer.getTransferType())
                .sourceTableNumber(sourceTable.getTableNumber())
                .sourceTableName(sourceTable.getName())
                .status(TransferStatus.CANCELLED)
                .expiresAt(transfer.getExpiresAt())
                .ttlSeconds(0L)
                .message("Đã hủy yêu cầu chuyển bàn thành công. Giỏ hàng và bàn ăn đã được mở khóa bình thường.")
                .build();
    }

    @Override
    @Transactional
    public TableTransferConfirmResponse directTransfer(TableDirectTransferRequest request, String username) {
        RestaurantTable sourceTable = tableRepository.findById(request.getSourceTableId())
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", "id", request.getSourceTableId()));
        RestaurantTable targetTable = tableRepository.findById(request.getTargetTableId())
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", "id", request.getTargetTableId()));

        if (sourceTable.getId().equals(targetTable.getId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể chuyển hoặc ghép vào chính bàn hiện tại");
        }

        // Tạo mã chuyển bàn ảo nội bộ cho Admin
        String internalCode = generateTransferCode();
        TableTransfer transfer = TableTransfer.builder()
                .transferCode(internalCode)
                .transferType(request.getTransferType())
                .sourceTable(sourceTable)
                .targetTable(targetTable)
                .sourceSessionToken(sourceTable.getCurrentSessionToken() != null ? sourceTable.getCurrentSessionToken() : "SESSION_" + sourceTable.getTableNumber())
                .createdByDevice("STAFF_" + (username != null ? username : "POS"))
                .status(TransferStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .reason(request.getReason() != null ? request.getReason() : "Nhân viên thực hiện trực tiếp trên POS")
                .build();
        tableTransferRepository.save(transfer);

        TableTransferConfirmRequest confirmReq = TableTransferConfirmRequest.builder()
                .targetTableNumber(targetTable.getTableNumber())
                .transferCode(internalCode)
                .deviceName("POS Quản Lý")
                .build();

        return confirmTransfer(confirmReq, "STAFF_DIRECT_TOKEN");
    }

    @Override
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireOutdatedTransfers() {
        LocalDateTime now = LocalDateTime.now();
        List<TableTransfer> expiredList = tableTransferRepository.findByStatusAndExpiresAtBefore(TransferStatus.PENDING, now);
        for (TableTransfer t : expiredList) {
            t.setStatus(TransferStatus.EXPIRED);
            tableTransferRepository.save(t);

            RestaurantTable sourceTable = t.getSourceTable();
            if (sourceTable != null) {
                // Kiểm tra xem bàn còn mã PENDING nào khác không
                boolean hasOtherPending = tableTransferRepository.findFirstBySourceTableAndStatus(sourceTable, TransferStatus.PENDING).isPresent();
                if (!hasOtherPending) {
                    sourceTable.setIsOrderLocked(false);
                    tableRepository.save(sourceTable);
                    broadcastTableUpdate(sourceTable);
                }
            }
            log.info("Mã chuyển bàn {} đã hết hạn 5 phút và tự động hoàn trả khóa cho bàn {}", t.getTransferCode(), sourceTable != null ? sourceTable.getTableNumber() : "N/A");
        }
    }

    private void resetSourceTableSession(RestaurantTable table) {
        table.setStatus(TableStatus.AVAILABLE);
        table.setIsOrderLocked(false);
        table.setCurrentSessionToken(UUID.randomUUID().toString()); // Token mới vô hiệu hóa toàn bộ token cũ
        table.generateNewPasscode();
        table.setActiveDeviceCount(0);
        table.setSessionStartedAt(null);
        table.resetFailedAttempts();
        tableRepository.save(table);

        List<TableSessionDevice> oldDevices = tableSessionDeviceRepository.findByTable(table);
        for (TableSessionDevice d : oldDevices) {
            d.setIsActive(false);
        }
        tableSessionDeviceRepository.saveAll(oldDevices);
    }

    private void broadcastTableUpdate(RestaurantTable table) {
        if (messagingTemplate != null && table != null) {
            try {
                messagingTemplate.convertAndSend("/topic/tables", Map.of(
                        "id", table.getId(),
                        "tableNumber", table.getTableNumber(),
                        "name", table.getName(),
                        "status", table.getStatus(),
                        "isOrderLocked", table.getIsOrderLocked(),
                        "activeDeviceCount", table.getActiveDeviceCount() != null ? table.getActiveDeviceCount() : 0
                ));
            } catch (Exception e) {
                log.warn("Không thể gửi thông báo WebSocket cập nhật bàn qua /topic/tables: {}", e.getMessage());
            }
        }
    }

    private void syncDraftCart(RestaurantTable table, List<DraftCartItemRequest> draftItems) {
        if (draftItems == null || draftItems.isEmpty()) {
            return;
        }
        Cart cart = cartRepository.findByRestaurantTable(table)
                .orElseGet(() -> Cart.builder()
                        .restaurantTable(table)
                        .sessionToken(table.getCurrentSessionToken() != null ? table.getCurrentSessionToken() : "SESSION_" + table.getTableNumber())
                        .items(new ArrayList<>())
                        .build()
                );

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        } else {
            cart.getItems().clear();
        }

        for (DraftCartItemRequest draftItem : draftItems) {
            if (draftItem.getMenuItemId() != null && draftItem.getQuantity() != null && draftItem.getQuantity() > 0) {
                menuItemRepository.findById(draftItem.getMenuItemId()).ifPresent(menuItem -> {
                    CartItem cartItem = CartItem.builder()
                            .cart(cart)
                            .menuItem(menuItem)
                            .quantity(Math.min(draftItem.getQuantity(), 99))
                            .note(draftItem.getNote() != null ? draftItem.getNote().trim() : null)
                            .build();
                    cart.addItem(cartItem);
                });
            }
        }
        cartRepository.save(cart);
    }

    private List<DraftCartItemResponse> getDraftCartItemResponses(RestaurantTable table) {
        List<DraftCartItemResponse> result = new ArrayList<>();
        Optional<Cart> cartOpt = cartRepository.findByRestaurantTable(table);
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            if (cart.getItems() != null) {
                for (CartItem ci : cart.getItems()) {
                    MenuItem m = ci.getMenuItem();
                    if (m != null) {
                        result.add(DraftCartItemResponse.builder()
                                .id(m.getId())
                                .name(m.getName())
                                .price(m.getPrice())
                                .quantity(ci.getQuantity())
                                .note(ci.getNote())
                                .image(m.getImageUrl())
                                .unit(m.getUnit())
                                .build());
                    }
                }
            }
        }
        return result;
    }

    private void broadcastTransferEvents(RestaurantTable sourceTable, RestaurantTable targetTable, String oldSessionToken, String newSessionToken, List<DraftCartItemResponse> cartItems) {
        if (messagingTemplate == null) return;

        try {
            // 1. Kênh bàn cũ: Chuyển hướng các thiết bị đang mở app kèm theo giỏ hàng nháp đã hợp nhất
            Map<String, Object> redirectPayload = new HashMap<>();
            redirectPayload.put("event", "TABLE_TRANSFERRED");
            redirectPayload.put("oldTableNumber", sourceTable.getTableNumber());
            redirectPayload.put("newTableNumber", targetTable.getTableNumber());
            redirectPayload.put("newTableName", targetTable.getName());
            redirectPayload.put("newSessionToken", newSessionToken);
            redirectPayload.put("cartItems", cartItems != null ? cartItems : Collections.emptyList());
            redirectPayload.put("message", "Bàn ăn của bạn đã được chuyển sang " + targetTable.getName() + " (" + targetTable.getTableNumber() + ").");

            messagingTemplate.convertAndSend("/topic/table/" + oldSessionToken, redirectPayload);

            // 2. Kênh trạm Bếp KDS & Phục Vụ Waiter: Cập nhật số bàn
            Map<String, Object> kitchenPayload = Map.of(
                    "event", "TABLE_CHANGED",
                    "oldTableNumber", sourceTable.getTableNumber(),
                    "newTableNumber", targetTable.getTableNumber(),
                    "message", "Các món của bàn " + sourceTable.getTableNumber() + " đã đổi sang phục vụ tại bàn " + targetTable.getTableNumber()
            );
            messagingTemplate.convertAndSend("/topic/kitchen/orders", kitchenPayload);
            messagingTemplate.convertAndSend("/topic/waiter/orders", kitchenPayload);

            // 3. Kênh sơ đồ bàn: Cập nhật Admin & Waiter
            broadcastTableUpdate(sourceTable);
            broadcastTableUpdate(targetTable);

        } catch (Exception e) {
            log.warn("Lỗi khi bắn sự kiện WebSocket sau khi chuyển bàn: {}", e.getMessage());
        }
    }
}
