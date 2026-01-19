package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.Payment;
import com.ogabek.istudy.entity.PaymentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN FETCH p.student " +
            "LEFT JOIN FETCH p.group " +
            "LEFT JOIN FETCH p.branch " +
            "WHERE p.branch.id = :branchId " +
            "ORDER BY p.createdAt DESC")
    List<Payment> findByBranchIdWithAllRelations(@Param("branchId") Long branchId);

    // NEW: Find payments by category
    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN FETCH p.student " +
            "LEFT JOIN FETCH p.group " +
            "LEFT JOIN FETCH p.branch " +
            "WHERE p.branch.id = :branchId AND p.category = :category " +
            "ORDER BY p.createdAt DESC")
    List<Payment> findByBranchIdAndCategoryWithAllRelations(@Param("branchId") Long branchId, 
                                                            @Param("category") PaymentCategory category);

    // NEW: Find payments by category and month
    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN FETCH p.student " +
            "LEFT JOIN FETCH p.group " +
            "LEFT JOIN FETCH p.branch " +
            "WHERE p.branch.id = :branchId AND p.category = :category " +
            "AND p.paymentYear = :year AND p.paymentMonth = :month " +
            "ORDER BY p.createdAt DESC")
    List<Payment> findByBranchIdAndCategoryAndMonthWithAllRelations(@Param("branchId") Long branchId,
                                                                    @Param("category") PaymentCategory category,
                                                                    @Param("year") int year,
                                                                    @Param("month") int month);

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN FETCH p.student " +
            "LEFT JOIN FETCH p.group " +
            "LEFT JOIN FETCH p.branch " +
            "WHERE p.student.id = :studentId " +
            "ORDER BY p.createdAt DESC")
    List<Payment> findByStudentIdWithRelations(@Param("studentId") Long studentId);

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN FETCH p.student " +
            "LEFT JOIN FETCH p.group " +
            "LEFT JOIN FETCH p.branch " +
            "WHERE p.id = :id")
    Optional<Payment> findByIdWithAllRelations(@Param("id") Long id);

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN FETCH p.student " +
            "LEFT JOIN FETCH p.group " +
            "LEFT JOIN FETCH p.branch " +
            "WHERE p.branch.id = :branchId AND p.createdAt BETWEEN :start AND :end " +
            "ORDER BY p.createdAt DESC")
    List<Payment> findByBranchIdAndCreatedAtBetweenWithRelations(@Param("branchId") Long branchId,
                                                                 @Param("start") LocalDateTime start,
                                                                 @Param("end") LocalDateTime end);

    @Query("SELECT p FROM Payment p " +
            "LEFT JOIN FETCH p.student " +
            "LEFT JOIN FETCH p.group " +
            "LEFT JOIN FETCH p.branch " +
            "WHERE p.branch.id = :branchId AND p.paymentYear = :year AND p.paymentMonth = :month " +
            "ORDER BY p.createdAt DESC")
    List<Payment> findByBranchIdAndPaymentYearAndPaymentMonthWithRelations(@Param("branchId") Long branchId,
                                                                           @Param("year") int year,
                                                                           @Param("month") int month);

    List<Payment> findByBranchId(Long branchId);
    List<Payment> findByStudentId(Long studentId);
    List<Payment> findByGroupId(Long groupId);

    // Daily payments sum
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.branch.id = :branchId AND " +
            "DATE(p.createdAt) = DATE(:date)")
    BigDecimal sumDailyPayments(@Param("branchId") Long branchId, @Param("date") LocalDateTime date);

    // NEW: Daily payments sum by category
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.branch.id = :branchId AND " +
            "p.category = :category AND DATE(p.createdAt) = DATE(:date)")
    BigDecimal sumDailyPaymentsByCategory(@Param("branchId") Long branchId, 
                                         @Param("category") PaymentCategory category,
                                         @Param("date") LocalDateTime date);

    // Monthly payments sum
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.branch.id = :branchId AND " +
            "p.paymentYear = :year AND p.paymentMonth = :month")
    BigDecimal sumMonthlyPayments(@Param("branchId") Long branchId, @Param("year") int year, @Param("month") int month);

    // NEW: Monthly payments sum by category
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.branch.id = :branchId AND " +
            "p.category = :category AND p.paymentYear = :year AND p.paymentMonth = :month")
    BigDecimal sumMonthlyPaymentsByCategory(@Param("branchId") Long branchId,
                                           @Param("category") PaymentCategory category,
                                           @Param("year") int year,
                                           @Param("month") int month);

    // Range payments sum
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.branch.id = :branchId AND " +
            "p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumPaymentsByDateRange(@Param("branchId") Long branchId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // NEW: Range payments sum by category
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.branch.id = :branchId AND " +
            "p.category = :category AND p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumPaymentsByDateRangeAndCategory(@Param("branchId") Long branchId,
                                                 @Param("category") PaymentCategory category,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    // Teacher's student payments for salary calculation
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.group.teacher.id = :teacherId AND p.paymentYear = :year AND p.paymentMonth = :month")
    BigDecimal sumTeacherStudentPayments(@Param("teacherId") Long teacherId,
                                         @Param("year") int year, @Param("month") int month);

    // Count teacher's students who paid
    @Query("SELECT COUNT(DISTINCT p.student.id) FROM Payment p " +
            "WHERE p.group.teacher.id = :teacherId AND p.paymentYear = :year AND p.paymentMonth = :month")
    int countTeacherPaidStudents(@Param("teacherId") Long teacherId,
                                 @Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.student.id = :studentId AND p.group.id = :groupId " +
            "AND p.paymentYear = :year AND p.paymentMonth = :month")
    BigDecimal getTotalPaidByStudentInGroupForMonth(@Param("studentId") Long studentId,
                                                    @Param("groupId") Long groupId,
                                                    @Param("year") int year,
                                                    @Param("month") int month);

    @Modifying
    @Query("DELETE FROM Payment p WHERE p.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}