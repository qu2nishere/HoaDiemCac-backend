package com.hoadiemcat.repository;

import com.hoadiemcat.entity.Cart;
import com.hoadiemcat.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByRestaurantTable(RestaurantTable restaurantTable);

    Optional<Cart> findBySessionToken(String sessionToken);

    void deleteByRestaurantTable(RestaurantTable restaurantTable);
}
