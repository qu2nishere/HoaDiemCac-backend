package com.hoadiemcat.entity.enums;

/**
 * Phân loại hình thức điều chuyển bàn ăn trong nhà hàng Hỏa Diệm Các.
 */
public enum TransferType {
    /**
     * Chuyển bàn (1:1): Chuyển toàn bộ phiên sang một bàn trống mới.
     */
    MOVE,

    /**
     * Ghép bàn (N:1): Hợp nhất giỏ hàng, đơn hàng và thiết bị vào một bàn đang có khách.
     */
    MERGE
}
