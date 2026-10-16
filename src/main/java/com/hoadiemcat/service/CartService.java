package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.DraftCartItemRequest;
import com.hoadiemcat.dto.response.DraftCartItemResponse;

import java.util.List;

/**
 * Service quản lý giỏ hàng cộng tác thời gian thực tại bàn (UC04 - Realtime Collaborative Cart).
 */
public interface CartService {

    /**
     * Lấy danh sách các món hiện có trong giỏ hàng nháp của bàn
     */
    List<DraftCartItemResponse> getCart(String tableNumber);

    /**
     * Cập nhật toàn bộ giỏ hàng nháp của bàn kèm senderDeviceToken để loại trừ tiếng vang dội ngược
     */
    List<DraftCartItemResponse> updateCart(String tableNumber, List<DraftCartItemRequest> items, String senderDeviceToken);

    default List<DraftCartItemResponse> updateCart(String tableNumber, List<DraftCartItemRequest> items) {
        return updateCart(tableNumber, items, null);
    }

    /**
     * Làm rỗng giỏ hàng nháp của bàn và phát WebSocket xóa giỏ
     */
    void clearCart(String tableNumber);
}
