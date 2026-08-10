package com.ogabek.istudy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * O'qituvchining bir oylik davri (teacher + year + month).
 *
 * <p>Davr yopilganda ({@link SalaryPeriodStatus#CLOSED}) o'sha paytdagi hisob-kitob
 * shu yerga "muzlatib" yoziladi. Shundan keyin o'qituvchi tarifi, guruh egasi,
 * guruh a'zolari yoki o'quvchi to'lovlari o'zgarsa ham bu davr summasi o'zgarmaydi.
 *
 * <p>Bu yangi jadval — mavjud bazaga zarar bermaydi, {@code ddl-auto=update}
 * uni avtomatik yaratadi.
 */
@Entity
@Table(name = "teacher_salary_periods", uniqueConstraints = {
        @UniqueConstraint(name = "uk_salary_period_teacher_year_month", columnNames = { "teacher_id", "period_year",
                "period_month" })
}, indexes = {
        @Index(name = "idx_salary_period_branch_year_month", columnList = "branch_id,period_year,period_month"),
        @Index(name = "idx_salary_period_teacher", columnList = "teacher_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSalaryPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "period_year", nullable = false)
    private int year;

    @Column(name = "period_month", nullable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SalaryPeriodStatus status = SalaryPeriodStatus.OPEN;

    // ---------------------------------------------------------------
    // Muzlatilgan hisob-kitob (faqat CLOSED holatda ishonchli)
    // ---------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type_snapshot", length = 16)
    private SalaryType salaryTypeSnapshot;

    @Column(name = "base_salary_snapshot", precision = 19, scale = 2)
    private BigDecimal baseSalarySnapshot = BigDecimal.ZERO;

    @Column(name = "payment_percentage_snapshot", precision = 7, scale = 2)
    private BigDecimal paymentPercentageSnapshot = BigDecimal.ZERO;

    @Column(name = "payment_based_salary_snapshot", precision = 19, scale = 2)
    private BigDecimal paymentBasedSalarySnapshot = BigDecimal.ZERO;

    @Column(name = "student_payments_snapshot", precision = 19, scale = 2)
    private BigDecimal studentPaymentsSnapshot = BigDecimal.ZERO;

    @Column(name = "total_salary_snapshot", precision = 19, scale = 2)
    private BigDecimal totalSalarySnapshot = BigDecimal.ZERO;

    @Column(name = "paid_student_count_snapshot")
    private int paidStudentCountSnapshot;

    @OneToMany(mappedBy = "period", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TeacherSalaryPeriodLine> lines = new ArrayList<>();

    // ---------------------------------------------------------------
    // Audit
    // ---------------------------------------------------------------

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by_user_id")
    private Long closedByUserId;

    @Column(name = "closed_by_username", length = 100)
    private String closedByUsername;

    /** Davr avtomatik yopilganmi (to'lov paytida / oy tugagach) yoki admin qo'lda yopganmi. */
    @Column(name = "auto_closed")
    private Boolean autoClosed;

    /** Ishga tushirilgandagi migratsiya natijasida yopilgan eski davrlar. */
    @Column(name = "backfilled")
    private Boolean backfilled;

    @Column(name = "reopen_count", nullable = false)
    private int reopenCount;

    @Column(name = "last_reopened_at")
    private LocalDateTime lastReopenedAt;

    @Column(name = "last_reopened_by_username", length = 100)
    private String lastReopenedByUsername;

    @Column(name = "last_reopen_reason", length = 500)
    private String lastReopenReason;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isClosed() {
        return status == SalaryPeriodStatus.CLOSED;
    }

    public void replaceLines(List<TeacherSalaryPeriodLine> newLines) {
        lines.clear();
        if (newLines != null) {
            for (TeacherSalaryPeriodLine line : newLines) {
                line.setPeriod(this);
                lines.add(line);
            }
        }
    }
}
