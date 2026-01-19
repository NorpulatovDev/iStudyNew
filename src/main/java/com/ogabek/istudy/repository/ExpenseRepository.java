package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.Expense;
import com.ogabek.istudy.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByBranchId(Long branchId);
    List<Expense> findByBranchIdAndCategory(Long branchId, ExpenseCategory category);
    List<Expense> findByBranchIdAndCreatedAtBetween(Long branchId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.branch.id = :branchId AND " +
           "DATE(e.createdAt) = DATE(:date)")
    BigDecimal sumDailyExpenses(@Param("branchId") Long branchId, @Param("date") LocalDateTime date);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.branch.id = :branchId AND " +
           "YEAR(e.createdAt) = :year AND MONTH(e.createdAt) = :month")
    BigDecimal sumMonthlyExpenses(@Param("branchId") Long branchId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.branch.id = :branchId AND " +
           "e.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumExpensesByDateRange(@Param("branchId") Long branchId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.branch.id = :branchId")
    BigDecimal sumAllTimeExpenses(@Param("branchId") Long branchId);
}
