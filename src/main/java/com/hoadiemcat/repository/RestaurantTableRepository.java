package com.hoadiemcat.repository;

import com.hoadiemcat.entity.RestaurantTable;
import com.hoadiemcat.entity.enums.TableArea;
import com.hoadiemcat.entity.enums.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    Optional<RestaurantTable> findByTableNumber(String tableNumber);

    Optional<RestaurantTable> findByCurrentSessionToken(String currentSessionToken);

    List<RestaurantTable> findByArea(TableArea area);

    List<RestaurantTable> findByStatus(TableStatus status);

    List<RestaurantTable> findAllByOrderByTableNumberAsc();

    boolean existsByTableNumber(String tableNumber);
}
