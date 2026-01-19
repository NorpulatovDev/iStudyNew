package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.TeacherSalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TeacherSalaryPaymentRepository extends JpaRepository<TeacherSalaryPayment, Long> {

    @Query("SELECT tsp FROM TeacherSalaryPayment tsp " +
           "LEFT JOIN FETCH tsp.teacher " +
           "LEFT JOIN FETCH tsp.branch " +
           "WHERE tsp.branch.id = :branchId " +
           "ORDER BY tsp.createdAt DESC")
    List<TeacherSalaryPayment> findByBranchIdWithDetails(@Param("branchId") Long branchId);

    @Query("SELECT tsp FROM TeacherSalaryPayment tsp " +
           "LEFT JOIN FETCH tsp.teacher " +
           "LEFT JOIN FETCH tsp.branch " +
           "WHERE tsp.teacher.id = :teacherId " +
           "ORDER BY tsp.year DESC, tsp.month DESC, tsp.createdAt DESC")
    List<TeacherSalaryPayment> findByTeacherIdWithDetails(@Param("teacherId") Long teacherId);

    @Query("SELECT tsp FROM TeacherSalaryPayment tsp " +
           "LEFT JOIN FETCH tsp.teacher " +
           "LEFT JOIN FETCH tsp.branch " +
           "WHERE tsp.teacher.id = :teacherId AND tsp.year = :year AND tsp.month = :month " +
           "ORDER BY tsp.createdAt DESC")
    List<TeacherSalaryPayment> findByTeacherAndYearAndMonthWithDetails(@Param("teacherId") Long teacherId,
                                                                       @Param("year") int year,
                                                                       @Param("month") int month);

    @Query("SELECT COALESCE(SUM(tsp.amount), 0) FROM TeacherSalaryPayment tsp " +
           "WHERE tsp.teacher.id = :teacherId AND tsp.year = :year AND tsp.month = :month")
    BigDecimal sumByTeacherAndYearAndMonth(@Param("teacherId") Long teacherId,
                                           @Param("year") int year,
                                           @Param("month") int month);

    @Query("SELECT COALESCE(SUM(tsp.amount), 0) FROM TeacherSalaryPayment tsp " +
           "WHERE tsp.branch.id = :branchId AND tsp.year = :year AND tsp.month = :month")
    BigDecimal sumMonthlySalaryPayments(@Param("branchId") Long branchId,
                                        @Param("year") int year,
                                        @Param("month") int month);

    @Query("SELECT COALESCE(SUM(tsp.amount), 0) FROM TeacherSalaryPayment tsp " +
           "WHERE tsp.branch.id = :branchId AND tsp.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumSalaryPaymentsByDateRange(@Param("branchId") Long branchId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(tsp.amount), 0) FROM TeacherSalaryPayment tsp " +
           "WHERE tsp.branch.id = :branchId")
    BigDecimal sumAllTimeSalaryPayments(@Param("branchId") Long branchId);

    @Query("SELECT MAX(tsp.createdAt) FROM TeacherSalaryPayment tsp " +
           "WHERE tsp.teacher.id = :teacherId AND tsp.year = :year AND tsp.month = :month")
    LocalDateTime getLastPaymentDate(@Param("teacherId") Long teacherId,
                                     @Param("year") int year,
                                     @Param("month") int month);

    @Query("SELECT COUNT(tsp) FROM TeacherSalaryPayment tsp " +
           "WHERE tsp.teacher.id = :teacherId AND tsp.year = :year AND tsp.month = :month")
    int countPaymentsByTeacherAndYearAndMonth(@Param("teacherId") Long teacherId,
                                              @Param("year") int year,
                                              @Param("month") int month);

    @Query("SELECT DISTINCT tsp.year, tsp.month FROM TeacherSalaryPayment tsp " +
           "WHERE tsp.teacher.id = :teacherId " +
           "ORDER BY tsp.year DESC, tsp.month DESC")
    List<Object[]> findDistinctYearMonthByTeacherId(@Param("teacherId") Long teacherId);
}
