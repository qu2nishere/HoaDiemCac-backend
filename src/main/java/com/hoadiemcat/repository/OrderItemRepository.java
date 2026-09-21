package com.hoadiemcat.repository;

import com.hoadiemcat.entity.OrderItem;
import com.hoadiemcat.entity.enums.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByStatus(OrderItemStatus status);
}
