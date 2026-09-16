package com.hoadiemcat.repository;

import com.hoadiemcat.entity.Invoice;
import com.hoadiemcat.entity.enums.PaymentMethod;
import com.hoadiemcat.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    List<Invoice> findByRestaurantTableId(Long tableId);

    Page<Invoice> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE " +
           "(:query IS NULL OR LOWER(i.invoiceCode) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(i.restaurantTable.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:status IS NULL OR i.paymentStatus = :status) AND " +
           "(:method IS NULL OR i.paymentMethod = :method) AND " +
           "(:startDate IS NULL OR i.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR i.createdAt <= :endDate)")
    Page<Invoice> searchInvoices(
            @Param("query") String query,
            @Param("status") PaymentStatus status,
            @Param("method") PaymentMethod method,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(i.finalAmount), 0) FROM Invoice i WHERE i.paymentStatus = 'PAID' AND i.paidAt BETWEEN :start AND :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.paymentStatus = 'PAID' AND i.paidAt BETWEEN :start AND :end")
    Long countPaidInvoicesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT i.paymentMethod, COALESCE(SUM(i.finalAmount), 0), COUNT(i) FROM Invoice i WHERE i.paymentStatus = 'PAID' AND i.paidAt BETWEEN :start AND :end GROUP BY i.paymentMethod")
    List<Object[]> getRevenueByPaymentMethodBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Invoice> findTop10ByPaymentStatusOrderByPaidAtDesc(PaymentStatus paymentStatus);
}
