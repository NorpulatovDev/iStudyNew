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
    private BigDecimal remainingAmount;
    private boolean isFullyPaid;
    private LocalDateTime lastPaymentDate;
    private int paymentCount;
}
