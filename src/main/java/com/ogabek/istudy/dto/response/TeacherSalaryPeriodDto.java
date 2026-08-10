package com.ogabek.istudy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * O'qituvchining bir oylik davri va uning muzlatilgan hisob-kitobi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSalaryPeriodDto {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private Long branchId;
    private String branchName;
    private int year;
    private int month;

    /** {@code OPEN} yoki {@code CLOSED}. */
    private String status;
    private boolean frozen;

    // Muzlatilgan qiymatlar
    private String salaryType;
    private BigDecimal baseSalary;
    private BigDecimal paymentPercentage;
    private BigDecimal paymentBasedSalary;
    private BigDecimal totalStudentPayments;
    private BigDecimal totalSalary;
    private int paidStudentCount;
    private List<GroupSalaryInfo> groups;

    // Daftar holati
    private BigDecimal alreadyPaid;
    private BigDecimal earningsAdjustments;
    private BigDecimal balance;

    // Audit
    private LocalDateTime closedAt;
    private String closedBy;
    private Boolean autoClosed;
    private Boolean backfilled;
    private int reopenCount;
    private LocalDateTime lastReopenedAt;
    private String lastReopenedBy;
    private String lastReopenReason;
}
