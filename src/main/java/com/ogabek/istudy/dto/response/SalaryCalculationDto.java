package com.ogabek.istudy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaryCalculationDto {
    private Long teacherId;
    private String teacherName;
    private int year;
    private int month;
    private BigDecimal baseSalary;
    private BigDecimal paymentBasedSalary;
    private BigDecimal totalSalary;
    private BigDecimal totalStudentPayments;
    private int totalStudents;
    private BigDecimal alreadyPaid;
    /** Eskicha: hech qachon manfiy bo'lmaydi. Ortiqcha to'lovni ko'rish uchun {@link #balance} ga qarang. */
    private BigDecimal remainingAmount;
    private Long branchId;
    private String branchName;
    private List<GroupSalaryInfo> groups;

    // ------------------------------------------------------------------
    // Yangi maydonlar — eski frontend uchun xavfsiz (qo'shimcha)
    // ------------------------------------------------------------------

    /** {@code OPEN} — jonli hisoblanadi, {@code CLOSED} — muzlatilgan. */
    private String status;

    /** {@code true} bo'lsa, bu oy summasi endi o'zgarmaydi. */
    private boolean frozen;

    private LocalDateTime closedAt;
    private String closedBy;
    private int reopenCount;

    /** BONUS − DEDUCTION. {@link #totalSalary} ichiga allaqachon qo'shilgan. */
    private BigDecimal earningsAdjustments;

    /** {@code totalSalary − alreadyPaid}. Manfiy bo'lishi mumkin (ortiqcha to'langan). */
    private BigDecimal balance;

    /** {@code true} — o'qituvchiga kerakligidan ko'p berilgan. */
    private boolean overpaid;

    /**
     * Faqat {@code includeDrift=true} so'ralganda va davr yopiq bo'lganda to'ladi.
     * Yopilgandan keyin o'quvchi to'lovlari / guruhlar o'zgargan bo'lsa {@code true}.
     */
    private Boolean driftDetected;

    /** Yopilgandan keyin o'zgargan summa (jonli hisob − muzlatilgan hisob). */
    private BigDecimal driftAmount;
}
