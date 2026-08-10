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
public class TeacherSalaryPaymentDto {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private int year;
    private int month;
    /** Har doim musbat. Ishorali qiymat uchun {@link #signedAmount} ga qarang. */
    private BigDecimal amount;
    private String description;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdAt;

    // ------------------------------------------------------------------
    // Yangi maydonlar — eski frontend uchun xavfsiz (qo'shimcha)
    // ------------------------------------------------------------------

    /** {@code PAYOUT}, {@code REVERSAL}, {@code BONUS} yoki {@code DEDUCTION}. */
    private String type;

    /** PAYOUT/BONUS musbat, REVERSAL/DEDUCTION manfiy. */
    private BigDecimal signedAmount;

    /** {@code true} — kassa harakati (hisobotlarda xarajat). */
    private boolean settlement;

    private String reason;

    /** REVERSAL yozuvida — bekor qilingan to'lov id si. */
    private Long reversesPaymentId;

    /** Asl yozuvda — qachon bekor qilingani. */
    private LocalDateTime reversedAt;

    private boolean reversed;

    private String createdByUsername;
}
