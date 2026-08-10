package com.ogabek.istudy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSalaryHistoryDto {
    private Long teacherId;
    private String teacherName;
    private int year;
    private int month;
    private BigDecimal totalSalary;
    private BigDecimal totalPaid;
    /** Eskicha: hech qachon manfiy bo'lmaydi. */
    private BigDecimal remainingAmount;
    private boolean isFullyPaid;
    private LocalDateTime lastPaymentDate;
    private int paymentCount;

    // ------------------------------------------------------------------
    // Yangi maydonlar — eski frontend uchun xavfsiz (qo'shimcha)
    // ------------------------------------------------------------------

    /** {@code OPEN} yoki {@code CLOSED}. */
    private String status;

    /** {@code true} bo'lsa, bu oy summasi endi o'zgarmaydi. */
    private boolean frozen;

    private LocalDateTime closedAt;

    /** BONUS − DEDUCTION. */
    private BigDecimal earningsAdjustments;

    /** {@code totalSalary − totalPaid}. Manfiy bo'lishi mumkin. */
    private BigDecimal balance;

    private boolean overpaid;
}
