package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.request.DraftCartItemRequest;
import com.hoadiemcat.dto.request.TableClusterLinkRequest;
import com.hoadiemcat.dto.request.TableDirectTransferRequest;
import com.hoadiemcat.dto.request.TableTransferConfirmRequest;
import com.hoadiemcat.dto.request.TableTransferRequest;
import com.hoadiemcat.dto.response.DraftCartItemResponse;
import com.hoadiemcat.dto.response.TableClusterResponse;
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
import java.util.stream.Collectors;

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

        // Chặn nếu bàn đang là bàn phụ trong cụm bàn tiệc lớn
        if (sourceTable.isLinked()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Bàn " + sourceTable.getTableNumber() + " đang là bàn phụ trong Cụm bàn "
                            + (sourceTable.getMasterTable() != null ? sourceTable.getMasterTable().getTableNumber() : "")
                            + ". Vui lòng thực hiện chuyển trên Bàn chính hoặc nhờ nhân viên hỗ trợ tách bàn!");
        }

        // Chặn nếu bàn đang là Bàn chính liên kết với các bàn phụ (yêu cầu tách hoặc điều chuyển cụm trên POS)
        List<RestaurantTable> currentSlaves = tableRepository.findByMasterTable(sourceTable);
        if (currentSlaves != null && !currentSlaves.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Bàn " + sourceTable.getTableNumber() + " đang là Bàn chính liên kết với các bàn phụ. Vui lòng liên hệ nhân viên để điều chuyển cả cụm bàn hoặc tách cụm trước khi chuyển bàn!");
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

        // 0. Tính Đẳng cự (Idempotency): Nếu mã đã COMPLETED và bàn đích trùng khớp (do mạng mobile retry gửi lại request)
        if (transfer.getStatus() == TransferStatus.COMPLETED && transfer.getTargetTable() != null) {
            RestaurantTable target = transfer.getTargetTable();
            String reqTarget = normalizeTableNumber(request.getTargetTableNumber());
            if (target.getTableNumber().equalsIgnoreCase(reqTarget) || target.getName().equalsIgnoreCase(request.getTargetTableNumber().trim())) {
                log.info("Request confirmTransfer gửi lại (Idempotent Retry) cho mã {} tại bàn {}", cleanCode, target.getTableNumber());
                List<DraftCartItemResponse> finalCartItems = getDraftCartItemResponses(target);
                List<Order> targetOrders = orderRepository.findByRestaurantTableOrderByCreatedAtAsc(target);
                int activeRounds = (int) targetOrders.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED).count();
                boolean isHost = tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue(deviceToken)
                        .map(d -> Boolean.TRUE.equals(d.getIsHost())).orElse(false);

                return TableTransferConfirmResponse.builder()
                        .newTableId(target.getId())
                        .newTableNumber(target.getTableNumber())
                        .newTableName(target.getName())
                        .newSessionToken(transfer.getTargetSessionToken())
                        .deviceToken(deviceToken)
                        .deviceName(request.getDeviceName())
                        .isHost(isHost)
                        .cartItemCount(finalCartItems.size())
                        .cartItems(finalCartItems)
                        .activeOrderRounds(activeRounds)
                        .message("Chuyển bàn sang " + target.getTableNumber() + " thành công!")
                        .build();
            }
        }

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

        // BẢO VỆ PHIÊN (Stale Session Hijacking Prevention):
        // Bàn nguồn bắt buộc phải đang OCCUPIED và Session Token phải còn trùng khớp với lúc sinh mã (nếu có)
        if (sourceTable.getStatus() != TableStatus.OCCUPIED
                || (transfer.getSourceSessionToken() != null && !Objects.equals(sourceTable.getCurrentSessionToken(), transfer.getSourceSessionToken()))) {
            transfer.setStatus(TransferStatus.CANCELLED);
            tableTransferRepository.save(transfer);
            throw new AppException(ErrorCode.INVALID_REQUEST, "Phiên ăn tại bàn nguồn đã kết thúc hoặc đã thanh toán. Mã chuyển bàn không còn hợp lệ!");
        }

        RestaurantTable targetTable = findTableByNumber(request.getTargetTableNumber());

        // Nếu bàn đích là bàn phụ trong cụm, tự động điều hướng sang bàn chính của cụm đó để ghép đúng dữ liệu
        if (targetTable.isLinked()) {
            log.info("Bàn đích {} là bàn phụ, tự động điều hướng sang Bàn chính {}",
                    targetTable.getTableNumber(), targetTable.getMasterTable() != null ? targetTable.getMasterTable().getTableNumber() : "N/A");
            targetTable = targetTable.getEffectiveTable();
        }

        // Kiểm tra không được chuyển vào chính nó
        if (sourceTable.getId().equals(targetTable.getId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Không thể chuyển hoặc ghép vào chính bàn hiện tại");
        }

        // Không thể chuyển hoặc ghép vào bàn đang dọn dẹp (CLEANING)
        if (targetTable.getStatus() == TableStatus.CLEANING) {
            throw new AppException(ErrorCode.TARGET_TABLE_NOT_AVAILABLE, "Bàn đích đang trong quá trình dọn dẹp, chưa sẵn sàng để tiếp nhận khách!");
        }

        // Kiểm tra bàn đích có đang trong tiến trình thanh toán không
        boolean hasPendingInvoiceTarget = invoiceRepository.findByRestaurantTableId(targetTable.getId()).stream()
                .anyMatch(inv -> inv.getPaymentStatus() == PaymentStatus.PENDING);
        if (hasPendingInvoiceTarget) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bàn đích đang trong quy trình thanh toán hóa đơn, không thể chuyển hoặc ghép bàn!");
        }

        String oldSessionToken = sourceTable.getCurrentSessionToken();

        // Đảm bảo targetTable có Session Token
        if (targetTable.getCurrentSessionToken() == null) {
            targetTable.setCurrentSessionToken(UUID.randomUUID().toString());
        }
        String newSessionToken = targetTable.getCurrentSessionToken();

        // Kiểm tra bảo mật khi GHÉP BÀN (MERGE): Bắt buộc nhập đúng mã PIN bàn đích để chống đổ nợ hóa đơn sang người lạ
        boolean isStaff = deviceToken != null && (deviceToken.startsWith("STAFF_") || deviceToken.startsWith("ADMIN_") || "STAFF_DIRECT_TOKEN".equals(deviceToken));
        if (transfer.getTransferType() == TransferType.MERGE && !isStaff) {
            String targetPass = targetTable.getCurrentPasscode();
            String inputPass = request.getTargetPasscode();
            if (targetPass != null && !targetPass.isBlank()) {
                if (inputPass == null || !inputPass.trim().equals(targetPass.trim())) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Để ghép vào bàn đang có khách, vui lòng nhập đúng mã PIN 4 số của bàn đích để xác thực!");
                }
            }
        }

        int activeOrderRounds = 0;

        if (transfer.getTransferType() == TransferType.MOVE) {
            // Nghiệp vụ 1: CHUYỂN BÀN (MOVE: 1 -> 1)
            // Bàn đích bắt buộc phải đang AVAILABLE
            if (targetTable.getStatus() != TableStatus.AVAILABLE) {
                throw new AppException(ErrorCode.TARGET_TABLE_NOT_AVAILABLE, "Bàn đích hiện đang có khách ngồi hoặc không trống, vui lòng chọn bàn trống khác!");
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
            if (!Boolean.TRUE.equals(targetTable.getIsOrderLocked())) {
                targetTable.setIsOrderLocked(false);
            }
            tableRepository.save(targetTable);

        } else {
            // Nghiệp vụ 2: GHÉP BÀN (MERGE: N -> 1)
            // Bàn đích bắt buộc phải đang OCCUPIED (có khách đang phục vụ)
            if (targetTable.getStatus() != TableStatus.OCCUPIED) {
                throw new AppException(ErrorCode.TARGET_TABLE_NOT_AVAILABLE, "Bàn đích hiện không có khách ngồi để ghép. Vui lòng chọn bàn đang phục vụ hoặc dùng tính năng Chuyển Bàn!");
            }

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
            if (!Boolean.TRUE.equals(targetTable.getIsOrderLocked())) {
                targetTable.setIsOrderLocked(false);
            }
            tableRepository.save(targetTable);
        }

        // 4. Di chuyển & đồng bộ danh sách thiết bị từ bàn nguồn sang bàn đích (tránh rơi rụng thiết bị thành viên)
        String effectiveDeviceToken = (deviceToken != null && !deviceToken.isBlank()) ? deviceToken : UUID.randomUUID().toString();
        boolean hasHost = tableSessionDeviceRepository.findFirstByTableAndIsHostTrueAndIsActiveTrue(targetTable).isPresent();
        boolean isHost = !hasHost;

        String deviceName = request.getDeviceName();
        if (deviceName == null || deviceName.isBlank()) {
            deviceName = isHost ? "Chủ Bàn (Thiết bị chuyển)" : "Thành Viên (" + targetTable.getTableNumber() + ")";
        }

        List<TableSessionDevice> sourceDevices = tableSessionDeviceRepository.findByTable(sourceTable);
        boolean confirmedDeviceFound = false;

        for (TableSessionDevice d : sourceDevices) {
            if (Boolean.TRUE.equals(d.getIsActive())) {
                d.setTable(targetTable);
                if (d.getDeviceToken().equals(effectiveDeviceToken)) {
                    d.setIsHost(isHost);
                    d.setDeviceName(deviceName);
                    confirmedDeviceFound = true;
                } else {
                    // Mọi thành viên khác từ bàn nguồn sang bàn đích luôn có vai trò Thành Viên (Member)
                    d.setIsHost(false);
                }
            }
        }

        if (!confirmedDeviceFound) {
            TableSessionDevice newDevice = TableSessionDevice.builder()
                    .table(targetTable)
                    .deviceToken(effectiveDeviceToken)
                    .deviceName(deviceName)
                    .deviceFingerprint(request.getDeviceFingerprint())
                    .isHost(isHost)
                    .isActive(true)
                    .connectedAt(LocalDateTime.now())
                    .build();
            sourceDevices.add(newDevice);
        }

        tableSessionDeviceRepository.saveAll(sourceDevices);

        // 5. Giải phóng Bàn Nguồn về trạng thái AVAILABLE
        resetSourceTableSession(sourceTable);

        // 6. Cập nhật lại số lượng thiết bị hoạt động thực tế tại bàn đích
        int totalActiveDevices = (int) tableSessionDeviceRepository.findByTableAndIsActiveTrueOrderByConnectedAtAsc(targetTable).size();
        targetTable.setActiveDeviceCount(totalActiveDevices);
        tableRepository.save(targetTable);

        // 7. Hoàn tất giao dịch TableTransfer
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setTargetTable(targetTable);
        transfer.setTargetSessionToken(newSessionToken);
        tableTransferRepository.save(transfer);
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

        // Phân quyền: Kiểm tra người hủy phải là Chủ Bàn (Host) hoặc Nhân viên (Staff)
        if (deviceToken != null && !deviceToken.isBlank()
                && !deviceToken.startsWith("STAFF_")
                && !deviceToken.startsWith("ADMIN_")
                && !"STAFF_DIRECT_TOKEN".equals(deviceToken)) {
            tableSessionDeviceRepository.findByDeviceTokenAndIsActiveTrue(deviceToken).ifPresent(device -> {
                if (device.getTable().getId().equals(sourceTable.getId()) && !Boolean.TRUE.equals(device.getIsHost())) {
                    throw new AppException(ErrorCode.HOST_PERMISSION_REQUIRED, "Chỉ Chủ Bàn (Host) mới có quyền hủy yêu cầu chuyển hoặc ghép bàn");
                }
            });
        }

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
        table.setMasterTable(null);
        tableRepository.save(table);

        List<TableSessionDevice> oldDevices = tableSessionDeviceRepository.findByTable(table);
        for (TableSessionDevice d : oldDevices) {
            if (d.getTable() != null && Objects.equals(d.getTable().getId(), table.getId())) {
                d.setIsActive(false);
            }
        }
        tableSessionDeviceRepository.saveAll(oldDevices);
    }

    @Override
    @Transactional
    public TableClusterResponse linkTablesToCluster(TableClusterLinkRequest request, String username) {
        RestaurantTable masterTable = tableRepository.findById(request.getMasterTableId())
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", "id", request.getMasterTableId()));

        if (masterTable.isLinked()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bàn này đang là bàn phụ của một cụm khác, không thể làm bàn chính");
        }

        // Đảm bảo bàn chính có Session Token và ở trạng thái OCCUPIED
        if (masterTable.getCurrentSessionToken() == null) {
            masterTable.setCurrentSessionToken(UUID.randomUUID().toString());
        }
        if (masterTable.getStatus() == TableStatus.AVAILABLE) {
            masterTable.setStatus(TableStatus.OCCUPIED);
            masterTable.setSessionStartedAt(LocalDateTime.now());
        }
        if (masterTable.getCurrentPasscode() == null) {
            masterTable.generateNewPasscode();
        }
        tableRepository.save(masterTable);

        List<String> linkedNumbers = new ArrayList<>();
        int addedCapacity = masterTable.getCapacity() != null ? masterTable.getCapacity() : 4;

        for (Long slaveId : request.getSlaveTableIds()) {
            if (slaveId.equals(masterTable.getId())) continue;

            RestaurantTable slave = tableRepository.findById(slaveId)
                    .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", "id", slaveId));

            if (slave.isMaster()) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Bàn " + slave.getTableNumber() + " đang là bàn chính của một cụm bàn khác, vui lòng tách cụm cũ trước");
            }
            if (slave.isLinked()) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Bàn " + slave.getTableNumber() + " đang liên kết trong một cụm khác");
            }

            // Kiểm tra bàn phụ có đang trong tiến trình thanh toán không
            boolean hasPendingInvoice = invoiceRepository.findByRestaurantTableId(slave.getId()).stream()
                    .anyMatch(inv -> inv.getPaymentStatus() == PaymentStatus.PENDING);
            if (hasPendingInvoice) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Bàn " + slave.getTableNumber() + " đang trong quy trình thanh toán, không thể ghép cụm");
            }

            // Gán liên kết vào masterTable
            slave.setMasterTable(masterTable);
            slave.setStatus(TableStatus.OCCUPIED);
            slave.setCurrentSessionToken(masterTable.getCurrentSessionToken());
            slave.setCurrentPasscode(masterTable.getCurrentPasscode());
            slave.setIsOrderLocked(false);
            tableRepository.save(slave);

            linkedNumbers.add(slave.getTableNumber());
            addedCapacity += (slave.getCapacity() != null ? slave.getCapacity() : 4);
            broadcastTableUpdate(slave);
        }

        broadcastTableUpdate(masterTable);

        log.info("Nhân viên {} đã tạo Cụm bàn tiệc lớn: Master {} liên kết với {}", username, masterTable.getTableNumber(), linkedNumbers);

        return TableClusterResponse.builder()
                .masterTableId(masterTable.getId())
                .masterTableNumber(masterTable.getTableNumber())
                .masterTableName(masterTable.getName())
                .linkedTableNumbers(linkedNumbers)
                .totalCapacity(addedCapacity)
                .message("Đã liên kết Cụm bàn thành công cho bàn " + masterTable.getTableNumber() + "!")
                .build();
    }

    @Override
    @Transactional
    public TableClusterResponse unlinkTableFromCluster(Long slaveTableId, String username) {
        RestaurantTable slave = tableRepository.findById(slaveTableId)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", "id", slaveTableId));

        if (!slave.isLinked()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bàn " + slave.getTableNumber() + " không nằm trong cụm bàn liên kết nào");
        }

        RestaurantTable master = slave.getMasterTable();
        slave.setMasterTable(null);
        resetSourceTableSession(slave);

        broadcastTableUpdate(slave);
        broadcastTableUpdate(master);

        log.info("Nhân viên {} đã tách bàn phụ {} ra khỏi Cụm bàn {}", username, slave.getTableNumber(), master.getTableNumber());

        List<RestaurantTable> remainingSlavesList = tableRepository.findByMasterTable(master);
        List<String> remainingSlaves = remainingSlavesList.stream()
                .map(RestaurantTable::getTableNumber)
                .collect(java.util.stream.Collectors.toList());

        int totalCap = (master.getCapacity() != null ? master.getCapacity() : 4) +
                remainingSlavesList.stream().mapToInt(t -> t.getCapacity() != null ? t.getCapacity() : 4).sum();

        return TableClusterResponse.builder()
                .masterTableId(master.getId())
                .masterTableNumber(master.getTableNumber())
                .masterTableName(master.getName())
                .linkedTableNumbers(remainingSlaves)
                .totalCapacity(totalCap)
                .message("Đã tách bàn " + slave.getTableNumber() + " ra khỏi cụm bàn " + master.getTableNumber() + " thành công!")
                .build();
    }

    private void broadcastTableUpdate(RestaurantTable table) {
        if (messagingTemplate != null && table != null) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("id", table.getId());
                payload.put("tableNumber", table.getTableNumber());
                payload.put("name", table.getName());
                payload.put("status", table.getStatus());
                payload.put("isOrderLocked", table.getIsOrderLocked());
                payload.put("activeDeviceCount", table.getActiveDeviceCount() != null ? table.getActiveDeviceCount() : 0);
                payload.put("isMaster", table.isMaster());
                payload.put("isLinked", table.isLinked());
                payload.put("masterTableNumber", table.getMasterTable() != null ? table.getMasterTable().getTableNumber() : null);

                // Bổ sung danh sách các bàn phụ liên kết cho Sơ đồ bàn POS
                List<String> linkedNums = tableRepository.findByMasterTable(table).stream()
                        .map(RestaurantTable::getTableNumber)
                        .collect(Collectors.toList());
                payload.put("linkedTableNumbers", linkedNums);

                messagingTemplate.convertAndSend("/topic/tables", payload);
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
