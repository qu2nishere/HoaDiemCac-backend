package com.hoadiemcat.repository;

import com.hoadiemcat.entity.Order;
import com.hoadiemcat.entity.RestaurantTable;
import com.hoadiemcat.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByRestaurantTableOrderByCreatedAtAsc(RestaurantTable restaurantTable);

    List<Order> findByRestaurantTableInOrderByCreatedAtAsc(List<RestaurantTable> restaurantTables);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.restaurantTable IN (:tables) AND o.status != com.hoadiemcat.entity.enums.OrderStatus.CANCELLED ORDER BY o.createdAt ASC")
    List<Order> findActiveOrdersByTablesWithItems(@Param("tables") List<RestaurantTable> tables);

    List<Order> findBySessionTokenOrderByCreatedAtAsc(String sessionToken);

    @Query("SELECT o FROM Order o WHERE o.status IN (:statuses) ORDER BY o.createdAt ASC")
    List<Order> findByStatusInOrderByCreatedAtAsc(@Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE (o.restaurantTable.tableNumber = :tableNumber OR o.restaurantTable.name = :tableNumber) ORDER BY o.createdAt ASC")
    List<Order> findByTableNumberOrderByCreatedAtAsc(@Param("tableNumber") String tableNumber);
}
