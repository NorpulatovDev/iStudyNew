package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<Sale> findByBranchIdAndSaleYearAndSaleMonthOrderByCreatedAtDesc(Long branchId, int year, int month);

    List<Sale> findByBranchIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long branchId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s " +
            "WHERE s.branch.id = :branchId AND s.saleYear = :year AND s.saleMonth = :month")
    BigDecimal sumMonthly(@Param("branchId") Long branchId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s " +
            "WHERE s.branch.id = :branchId AND DATE(s.createdAt) = DATE(:date)")
    BigDecimal sumDaily(@Param("branchId") Long branchId, @Param("date") LocalDateTime date);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s " +
            "WHERE s.branch.id = :branchId AND s.createdAt BETWEEN :start AND :end")
    BigDecimal sumByDateRange(@Param("branchId") Long branchId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.branch.id = :branchId")
    BigDecimal sumAllTime(@Param("branchId") Long branchId);
}
