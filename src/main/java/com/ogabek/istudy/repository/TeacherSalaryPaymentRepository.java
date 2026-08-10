package com.ogabek.istudy.repository;

import com.ogabek.istudy.entity.TeacherSalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Oylik daftari (ledger).
 *
 * <p>Daftarda ikki xil yozuv bor:
 * <ul>
 *   <li><b>Settlement</b> (PAYOUT, REVERSAL) — kassa harakati. Hisobotlarda xarajat.</li>
 *   <li><b>Earnings adjustment</b> (BONUS, DEDUCTION) — ishlab topilgan summani
 *       o'zgartiradi, kassaga tegmaydi.</li>
 * </ul>
 *
 * <p><b>Backward compatibility:</b> {@code type} ustuni bazadagi eski satrlarda
 * {@code NULL}. Settlement so'rovlari {@code type IS NULL} ni ham qamrab oladi va
 * {@code signedAmount} yo'q bo'lsa {@code amount} ga qaytadi — shu sababli
 * migratsiya ishlamagan taqdirda ham hisobotlar oldingidek natija beradi.
 */
@Repository
public interface TeacherSalaryPaymentRepository extends JpaRepository<TeacherSalaryPayment, Long> {

    /** Eski (NULL turli) satrlar PAYOUT deb qaraladi. */
    String SETTLEMENT_FILTER =
            " AND (tsp.type IS NULL OR tsp.type IN (" +
            "   com.ogabek.istudy.entity.SalaryTransactionType.PAYOUT," +
            "   com.ogabek.istudy.entity.SalaryTransactionType.REVERSAL)) ";

    /** Bu turlar faqat yangi kod tomonidan yoziladi, shuning uchun NULL qamralmaydi. */
    String EARNINGS_ADJUSTMENT_FILTER =
            " AND tsp.type IN (" +
            "   com.ogabek.istudy.entity.SalaryTransactionType.BONUS," +
            "   com.ogabek.istudy.entity.SalaryTransactionType.DEDUCTION) ";

    String SIGNED_SUM = "SELECT COALESCE(SUM(COALESCE(tsp.signedAmount, tsp.amount)), 0) " +
            "FROM TeacherSalaryPayment tsp ";

    // ------------------------------------------------------------------
    // O'qish
    // ------------------------------------------------------------------

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

    @Query("SELECT tsp FROM TeacherSalaryPayment tsp " +
           "LEFT JOIN FETCH tsp.teacher " +
           "LEFT JOIN FETCH tsp.branch " +
           "WHERE tsp.id = :id")
    Optional<TeacherSalaryPayment> findByIdWithDetails(@Param("id") Long id);

    // ------------------------------------------------------------------
    // Summalar — settlement (kassa harakati)
    // ------------------------------------------------------------------

    /** O'qituvchiga shu oy uchun haqiqatda berilgan pul: PAYOUT − REVERSAL. */
    @Query(SIGNED_SUM + "WHERE tsp.teacher.id = :teacherId AND tsp.year = :year AND tsp.month = :month"
            + SETTLEMENT_FILTER)
    BigDecimal sumByTeacherAndYearAndMonth(@Param("teacherId") Long teacherId,
                                           @Param("year") int year,
                                           @Param("month") int month);

    /** Hisobot uchun: shu oyda kassadan chiqqan oylik xarajati. */
    @Query(SIGNED_SUM + "WHERE tsp.branch.id = :branchId AND tsp.year = :year AND tsp.month = :month"
            + SETTLEMENT_FILTER)
    BigDecimal sumMonthlySalaryPayments(@Param("branchId") Long branchId,
                                        @Param("year") int year,
                                        @Param("month") int month);

    /** Hisobot uchun: sanalar oralig'ida kassadan chiqqan oylik xarajati. */
    @Query(SIGNED_SUM + "WHERE tsp.branch.id = :branchId AND tsp.createdAt BETWEEN :startDate AND :endDate"
            + SETTLEMENT_FILTER)
    BigDecimal sumSalaryPaymentsByDateRange(@Param("branchId") Long branchId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /** Hisobot uchun: butun davr bo'yicha kassadan chiqqan oylik xarajati. */
    @Query(SIGNED_SUM + "WHERE tsp.branch.id = :branchId" + SETTLEMENT_FILTER)
    BigDecimal sumAllTimeSalaryPayments(@Param("branchId") Long branchId);

    /** Davrni muzlatish kerakmi — kassa yozuvi bormi. */
    @Query("SELECT COUNT(tsp) FROM TeacherSalaryPayment tsp " +
           "WHERE tsp.teacher.id = :teacherId AND tsp.year = :year AND tsp.month = :month"
            + SETTLEMENT_FILTER)
    long countSettlements(@Param("teacherId") Long teacherId,
                          @Param("year") int year,
                          @Param("month") int month);

    // ------------------------------------------------------------------
    // Summalar — ishlab topilgan summaga tuzatishlar
    // ------------------------------------------------------------------

    /** Shu oy uchun BONUS − DEDUCTION. Muzlatilgan summaga qo'shiladi. */
    @Query(SIGNED_SUM + "WHERE tsp.teacher.id = :teacherId AND tsp.year = :year AND tsp.month = :month"
            + EARNINGS_ADJUSTMENT_FILTER)
    BigDecimal sumEarningsAdjustments(@Param("teacherId") Long teacherId,
                                      @Param("year") int year,
                                      @Param("month") int month);

    // ------------------------------------------------------------------
    // Boshqa
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Migratsiya (SalaryLedgerBackfillRunner)
    // ------------------------------------------------------------------

    @Query("SELECT COUNT(tsp) FROM TeacherSalaryPayment tsp WHERE tsp.type IS NULL")
    long countUntypedRows();

    /** Eski satrlarni PAYOUT deb belgilaydi. Idempotent. */
    @Modifying
    @Query("UPDATE TeacherSalaryPayment tsp " +
           "SET tsp.type = com.ogabek.istudy.entity.SalaryTransactionType.PAYOUT, " +
           "    tsp.signedAmount = tsp.amount " +
           "WHERE tsp.type IS NULL")
    int backfillLegacyRowsAsPayout();

    /** Migratsiya uchun: daftar yozuvi bor barcha (teacherId, year, month) uchliklari. */
    @Query("SELECT DISTINCT tsp.teacher.id, tsp.year, tsp.month FROM TeacherSalaryPayment tsp " +
           "ORDER BY tsp.teacher.id, tsp.year, tsp.month")
    List<Object[]> findAllDistinctTeacherYearMonth();
}
