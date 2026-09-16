package com.hoadiemcat.repository;

import com.hoadiemcat.entity.RestaurantTable;
import com.hoadiemcat.entity.TableSessionDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableSessionDeviceRepository extends JpaRepository<TableSessionDevice, Long> {

    List<TableSessionDevice> findByTableAndIsActiveTrueOrderByConnectedAtAsc(RestaurantTable table);

    Optional<TableSessionDevice> findByDeviceTokenAndIsActiveTrue(String deviceToken);

    Optional<TableSessionDevice> findFirstByTableAndIsHostTrueAndIsActiveTrue(RestaurantTable table);

    int countByTableAndIsActiveTrue(RestaurantTable table);

    List<TableSessionDevice> findByTable(RestaurantTable table);
}
