package com.hoadiemcat.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Thực thể CartItem đại diện cho một dòng món ăn nằm trong giỏ hàng chung của bàn (UC21, UC22).
 * Chứa số lượng món được chọn (ràng buộc 1 <= N <= 99 theo QĐ9) và ghi chú hương vị tùy chọn.
 */
@Entity
@Table(
    name = "cart_items",
    indexes = {
        @Index(name = "idx_cart_item_cart_id", columnList = "cart_id"),
        @Index(name = "idx_cart_item_menu_item_id", columnList = "menu_item_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cart", "menuItem"})
public class CartItem extends BaseEntity {

    /**
     * Giỏ hàng chung cha chứa dòng món này (UC04).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /**
     * Món ăn được chọn trong thực đơn (UC21).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    /**
     * Số lượng suất/đĩa được chọn. Ràng buộc theo Quy định QĐ9: 1 <= quantity <= 99 (UC22).
     */
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Ghi chú yêu cầu riêng của khách (VD: "Ít cay", "Không ớt", "Chấm sốt mè") (UC21).
     */
    @Column(name = "note", length = 255)
    private String note;
}
