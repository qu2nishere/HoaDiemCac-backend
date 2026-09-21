package com.hoadiemcat.repository;

import com.hoadiemcat.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("SELECT DISTINCT m FROM MenuItem m LEFT JOIN FETCH m.categories WHERE m.isDeleted = false ORDER BY m.id ASC")
    List<MenuItem> findAllActiveWithCategories();

    @Query("SELECT DISTINCT m FROM MenuItem m LEFT JOIN FETCH m.categories WHERE m.id = :id AND m.isDeleted = false")
    Optional<MenuItem> findByIdWithCategories(@Param("id") Long id);

    Optional<MenuItem> findByCode(String code);

    Optional<MenuItem> findByName(String name);

    boolean existsByCode(String code);
}
