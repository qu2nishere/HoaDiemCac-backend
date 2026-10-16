package com.hoadiemcat.service.impl;

import com.hoadiemcat.dto.request.DraftCartItemRequest;
import com.hoadiemcat.dto.response.DraftCartItemResponse;
import com.hoadiemcat.entity.Cart;
import com.hoadiemcat.entity.CartItem;
import com.hoadiemcat.entity.MenuItem;
import com.hoadiemcat.entity.RestaurantTable;
import com.hoadiemcat.exception.AppException;
import com.hoadiemcat.exception.ErrorCode;
import com.hoadiemcat.exception.ResourceNotFoundException;
import com.hoadiemcat.repository.CartRepository;
import com.hoadiemcat.repository.MenuItemRepository;
import com.hoadiemcat.repository.RestaurantTableRepository;
import com.hoadiemcat.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final RestaurantTableRepository tableRepository;
    private final MenuItemRepository menuItemRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private String normalizeTableNumber(String raw) {
        if (raw == null || raw.isBlank()) return "B01";
        String trimmed = raw.trim().toUpperCase();
        if (trimmed.matches("^\\d+$")) {
            return String.format("B%02d", Integer.parseInt(trimmed));
        }
        return trimmed;
    }

    private RestaurantTable findTableByNumber(String rawTableNumber) {
        String normalized = normalizeTableNumber(rawTableNumber);
        return tableRepository.findByTableNumber(normalized)
                .or(() -> tableRepository.findByName(normalized))
                .or(() -> tableRepository.findByName(rawTableNumber.trim()))
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantTable", "tableNumber", normalized));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DraftCartItemResponse> getCart(String tableNumber) {
        RestaurantTable table = findTableByNumber(tableNumber);
        if (table.isLinked()) {
            table = table.getEffectiveTable();
        }
        return getDraftCartItemResponses(table);
    }

    @Override
    @Transactional
    public List<DraftCartItemResponse> updateCart(String tableNumber, List<DraftCartItemRequest> items, String senderDeviceToken) {
        RestaurantTable table = findTableByNumber(tableNumber);
        if (table.isLinked()) {
            table = table.getEffectiveTable();
        }

        if (Boolean.TRUE.equals(table.getIsOrderLocked())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bàn đang được chốt hóa đơn tạm tính. Thao tác giỏ hàng tạm thời bị đóng băng.");
        }

        RestaurantTable finalTable = table;
        List<Cart> existingCarts = cartRepository.findAllByRestaurantTable(table);
        Cart cart;
        if (existingCarts.isEmpty()) {
            cart = Cart.builder()
                    .restaurantTable(finalTable)
                    .sessionToken(finalTable.getCurrentSessionToken() != null ? finalTable.getCurrentSessionToken() : "SESSION_" + finalTable.getTableNumber())
                    .items(new ArrayList<>())
                    .build();
        } else {
            cart = existingCarts.get(0);
            if (existingCarts.size() > 1) {
                // Tự động dọn dẹp các bản ghi dư thừa do xung đột gửi đồng thời
                for (int i = 1; i < existingCarts.size(); i++) {
                    cartRepository.delete(existingCarts.get(i));
                }
            }
        }

        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        } else {
            cart.getItems().clear();
        }

        if (items != null) {
            for (DraftCartItemRequest draftItem : items) {
                if (draftItem.getQuantity() != null && draftItem.getQuantity() > 0) {
                    MenuItem menuItem = null;
                    if (draftItem.getMenuItemId() != null) {
                        menuItem = menuItemRepository.findById(draftItem.getMenuItemId()).orElse(null);
                    }
                    if (menuItem == null && draftItem.getName() != null) {
                        menuItem = menuItemRepository.findByName(draftItem.getName()).orElse(null);
                    }
                    if (menuItem != null) {
                        CartItem cartItem = CartItem.builder()
                                .cart(cart)
                                .menuItem(menuItem)
                                .quantity(Math.min(Math.max(draftItem.getQuantity(), 1), 99))
                                .note(draftItem.getNote() != null ? draftItem.getNote().trim() : null)
                                .build();
                        cart.addItem(cartItem);
                    }
                }
            }
        }

        cartRepository.save(cart);
        List<DraftCartItemResponse> responses = getDraftCartItemResponses(table);
        broadcastCart(table, responses, senderDeviceToken);
        return responses;
    }

    @Override
    @Transactional
    public void clearCart(String tableNumber) {
        RestaurantTable table = findTableByNumber(tableNumber);
        if (table.isLinked()) {
            table = table.getEffectiveTable();
        }
        List<Cart> carts = cartRepository.findAllByRestaurantTable(table);
        if (!carts.isEmpty()) {
            cartRepository.deleteAll(carts);
        }
        broadcastCart(table, Collections.emptyList(), null);
    }

    private void broadcastCart(RestaurantTable table, List<DraftCartItemResponse> responses, String senderDeviceToken) {
        try {
            java.util.Map<String, Object> message = new java.util.HashMap<>();
            message.put("senderDeviceToken", senderDeviceToken);
            message.put("items", responses);

            messagingTemplate.convertAndSend("/topic/table/" + table.getTableNumber() + "/cart", message);
            String norm = normalizeTableNumber(table.getTableNumber());
            if (!norm.equalsIgnoreCase(table.getTableNumber())) {
                messagingTemplate.convertAndSend("/topic/table/" + norm + "/cart", message);
            }
            if (table.getName() != null && !table.getName().equalsIgnoreCase(table.getTableNumber())) {
                messagingTemplate.convertAndSend("/topic/table/" + table.getName() + "/cart", message);
            }
            if (table.getCurrentSessionToken() != null) {
                messagingTemplate.convertAndSend("/topic/table/" + table.getCurrentSessionToken() + "/cart", message);
            }
        } catch (Exception e) {
            log.warn("Lỗi khi bắn WebSocket giỏ hàng: {}", e.getMessage());
        }
    }

    private List<DraftCartItemResponse> getDraftCartItemResponses(RestaurantTable table) {
        List<DraftCartItemResponse> result = new ArrayList<>();
        Optional<Cart> cartOpt = cartRepository.findFirstByRestaurantTableOrderByIdDesc(table);
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
}
