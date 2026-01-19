package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class TeacherDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private BigDecimal baseSalary;
    private BigDecimal paymentPercentage;
    private String salaryType;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdAt;
}
