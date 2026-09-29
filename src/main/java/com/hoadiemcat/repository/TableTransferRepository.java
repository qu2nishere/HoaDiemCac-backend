package com.hoadiemcat.repository;

import com.hoadiemcat.entity.RestaurantTable;
import com.hoadiemcat.entity.TableTransfer;
import com.hoadiemcat.entity.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableTransferRepository extends JpaRepository<TableTransfer, Long> {

    Optional<TableTransfer> findByTransferCode(String transferCode);

    Optional<TableTransfer> findFirstBySourceTableAndStatus(RestaurantTable sourceTable, TransferStatus status);

    List<TableTransfer> findByStatusAndExpiresAtBefore(TransferStatus status, LocalDateTime now);

    boolean existsByTransferCode(String transferCode);
}
