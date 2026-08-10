package com.ogabek.istudy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Yopilgan davrdagi bitta guruh bo'yicha muzlatilgan tafsilot.
 *
 * <p>Guruh nomi va narxi nusxa qilib saqlanadi — guruh keyinchalik qayta nomlansa,
 * boshqa o'qituvchiga o'tkazilsa yoki o'chirilsa ham eski hisob-kitob buzilmaydi.
 * Shu sababli bu yerda {@code Group} ga FK emas, oddiy {@code groupId} bor.
 */
@Entity
@Table(name = "teacher_salary_period_lines", indexes = {
        @Index(name = "idx_salary_period_line_period", columnList = "period_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSalaryPeriodLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private TeacherSalaryPeriod period;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "group_name", length = 255)
    private String groupName;

    @Column(name = "group_price", precision = 19, scale = 2)
    private BigDecimal groupPrice = BigDecimal.ZERO;

    @Column(name = "paid_student_count")
    private int paidStudentCount;

    @Column(name = "total_students_in_group")
    private int totalStudentsInGroup;

    @Column(name = "group_payments", precision = 19, scale = 2)
    private BigDecimal groupPayments = BigDecimal.ZERO;
}
