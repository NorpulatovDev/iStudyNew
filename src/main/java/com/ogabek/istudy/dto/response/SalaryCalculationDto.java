package com.ogabek.istudy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
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
    private BigDecimal remainingAmount;
    private Long branchId;
    private String branchName;
    private List<GroupSalaryInfo> groups;
}
