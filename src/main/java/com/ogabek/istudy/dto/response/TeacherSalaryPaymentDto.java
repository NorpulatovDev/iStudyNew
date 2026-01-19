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
    private BigDecimal amount;
    private String description;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdAt;
}
