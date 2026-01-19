package com.ogabek.istudy.dto.request;

import com.ogabek.istudy.entity.SalaryType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateTeacherRequest {
    @NotBlank(message = "Ism kiritish majburiy")
    @Size(min = 2, max = 50, message = "Ism 2-50 harfdan iborat bo'lishi shart")
    private String firstName;

    @NotBlank(message = "Familiya kiritish majburiy")
    @Size(min = 2, max = 50, message = "Familiya 2-50 harfdan iborat bo'lishi shart")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Noto'gri formatdagi telefon raqam")
    private String phoneNumber;

    private BigDecimal baseSalary = BigDecimal.ZERO;

    private BigDecimal paymentPercentage = BigDecimal.ZERO;

    @NotNull(message = "Maosh turini kiritish majburiy")
    private SalaryType salaryType;

    @NotNull(message = "Filial kiritish majburiy")
    private Long branchId;
}
