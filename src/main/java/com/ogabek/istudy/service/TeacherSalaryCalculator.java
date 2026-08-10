package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.response.GroupSalaryInfo;
import com.ogabek.istudy.entity.Group;
import com.ogabek.istudy.entity.Student;
import com.ogabek.istudy.entity.Teacher;
import com.ogabek.istudy.repository.GroupRepository;
import com.ogabek.istudy.repository.PaymentRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Oylikni <b>joriy</b> ma'lumotlar asosida hisoblaydi (guruhlar, o'quvchilar,
 * to'lovlar, o'qituvchi tarifi).
 *
 * <p>Bu sinf hech narsa yozmaydi. Natijasi vaqt o'tishi bilan o'zgaradi — shuning
 * uchun davr yopilgach undan foydalanilmaydi, o'rniga
 * {@link com.ogabek.istudy.entity.TeacherSalaryPeriod} dagi muzlatilgan nusxa
 * o'qiladi. Hisoblash mantig'i o'zgartirilmagan, faqat alohida sinfga ajratilgan.
 */
@Component
@RequiredArgsConstructor
public class TeacherSalaryCalculator {

    private final GroupRepository groupRepository;
    private final PaymentRepository paymentRepository;

    @Getter
    public static class LiveSalary {
        private final BigDecimal baseSalary;
        private final BigDecimal paymentBasedSalary;
        private final BigDecimal totalSalary;
        private final BigDecimal totalStudentPayments;
        private final int totalPaidStudents;
        private final List<GroupSalaryInfo> groups;

        LiveSalary(BigDecimal baseSalary, BigDecimal paymentBasedSalary, BigDecimal totalSalary,
                   BigDecimal totalStudentPayments, int totalPaidStudents, List<GroupSalaryInfo> groups) {
            this.baseSalary = baseSalary;
            this.paymentBasedSalary = paymentBasedSalary;
            this.totalSalary = totalSalary;
            this.totalStudentPayments = totalStudentPayments;
            this.totalPaidStudents = totalPaidStudents;
            this.groups = groups;
        }
    }

    public LiveSalary compute(Teacher teacher, int year, int month) {
        List<Group> teacherGroups = groupRepository.findByTeacherIdWithRelations(teacher.getId());

        List<GroupSalaryInfo> groupInfos = new ArrayList<>();
        BigDecimal totalStudentPayments = BigDecimal.ZERO;
        int totalPaidStudents = 0;

        for (Group group : teacherGroups) {
            int totalStudentsInGroup = group.getStudents() != null ? group.getStudents().size() : 0;
            int paidStudentCount = 0;
            BigDecimal groupPayments = BigDecimal.ZERO;

            if (group.getStudents() != null) {
                for (Student student : group.getStudents()) {
                    BigDecimal studentGroupPayment = paymentRepository.getTotalPaidByStudentInGroupForMonth(
                            student.getId(), group.getId(), year, month);

                    if (studentGroupPayment != null && studentGroupPayment.compareTo(BigDecimal.ZERO) > 0) {
                        paidStudentCount++;
                        groupPayments = groupPayments.add(studentGroupPayment);
                    }
                }
            }

            totalPaidStudents += paidStudentCount;
            totalStudentPayments = totalStudentPayments.add(groupPayments);

            BigDecimal groupPrice = group.getPrice() != null ? group.getPrice() : BigDecimal.ZERO;

            groupInfos.add(new GroupSalaryInfo(
                    group.getId(),
                    group.getName(),
                    paidStudentCount,
                    groupPayments,
                    totalStudentsInGroup,
                    groupPrice));
        }

        BigDecimal baseSalary = teacher.getBaseSalary() != null ? teacher.getBaseSalary() : BigDecimal.ZERO;
        BigDecimal paymentBasedSalary = BigDecimal.ZERO;
        BigDecimal totalSalary;

        switch (teacher.getSalaryType()) {
            case FIXED:
                totalSalary = baseSalary;
                paymentBasedSalary = BigDecimal.ZERO;
                break;

            case PERCENTAGE:
                if (teacher.getPaymentPercentage() != null) {
                    paymentBasedSalary = totalStudentPayments
                            .multiply(teacher.getPaymentPercentage())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
                totalSalary = paymentBasedSalary;
                break;

            case MIXED:
                if (teacher.getPaymentPercentage() != null) {
                    paymentBasedSalary = totalStudentPayments
                            .multiply(teacher.getPaymentPercentage())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
                totalSalary = baseSalary.add(paymentBasedSalary);
                break;

            default:
                totalSalary = baseSalary;
                paymentBasedSalary = BigDecimal.ZERO;
        }

        return new LiveSalary(baseSalary, paymentBasedSalary, totalSalary,
                totalStudentPayments, totalPaidStudents, groupInfos);
    }
}
