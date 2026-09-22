package com.hoadiemcat.repository;

import com.hoadiemcat.entity.CallStaffLog;
import com.hoadiemcat.entity.RestaurantTable;
import com.hoadiemcat.entity.enums.CallStaffStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallStaffLogRepository extends JpaRepository<CallStaffLog, Long> {

    List<CallStaffLog> findByRestaurantTableAndStatus(RestaurantTable restaurantTable, CallStaffStatus status);

    List<CallStaffLog> findByRestaurantTableInAndStatus(List<RestaurantTable> restaurantTables, CallStaffStatus status);

    boolean existsByRestaurantTableAndStatus(RestaurantTable restaurantTable, CallStaffStatus status);

    List<CallStaffLog> findByStatusOrderByCreatedAtDesc(CallStaffStatus status);
}
