package com.ogabek.istudy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateStudentRequest {
    @NotBlank(message = "Ism kiritish majburiy")
    @Size(min = 2, max = 50, message = "Ism 2-50 harfdan iborat bo'lishi shart")
    private String firstName;

    @NotBlank(message = "Familiya kiritish majburiy")
    @Size(min = 2, max = 50, message = "Familiya 2-50 harfdan iborat bo'lishi shart")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Noto'gri formatdagi telefon raqam")
    private String phoneNumber;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Noto'gri formatdagi telefon raqam")
    private String parentPhoneNumber;

    @NotNull(message = "Filial kiritish majburiy")
    private Long branchId;

    private List<Long> groupIds = new ArrayList<>();
}
