package com.hoadiemcat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể Cart đại diện cho giỏ hàng cộng tác dùng chung theo thời gian thực tại bàn (UC04, BR04.1).
 * Lưu trữ tạm thời các món khách cùng bàn đang lựa chọn trước khi nhấn "Gửi đơn vào bếp" (UC05).
 * Giỏ hàng được đồng bộ liên tục qua WebSocket kênh /topic/table/{sessionToken}/cart.
 */
@Entity
@Table(
    name = "carts",
    indexes = {
        @Index(name = "idx_cart_table_id", columnList = "table_id"),
        @Index(name = "idx_cart_session_token", columnList = "session_token")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"restaurantTable", "items"})
public class Cart extends BaseEntity {

    /**
     * Bàn ăn sở hữu giỏ hàng chung này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable restaurantTable;

    /**
     * Token phiên QR động của bàn gắn với giỏ hàng này (QĐ2, UC04).
     */
    @Column(name = "session_token", nullable = false, length = 128)
    private String sessionToken;

    /**
     * Danh sách các dòng món ăn đang được chọn trong giỏ hàng (UC21, UC22, UC23).
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    /**
     * Thêm một món vào giỏ hàng hoặc cập nhật số lượng nếu đã tồn tại.
     */
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    /**
     * Xóa một món khỏi giỏ hàng.
     */
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    /**
     * Làm trống toàn bộ giỏ hàng sau khi đã gửi đơn vào bếp (UC05).
     */
    public void clearItems() {
        items.clear();
    }
}
