package com.ogabek.istudy.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateGroupRequest {
    @NotBlank(message = "Guruh nomi majburiy")
    @Size(min = 2, max = 50, message = "Guruh nomi 2-50 harfdan iborat bo'lishi kerak")
    private String name;

    @Size(max = 500, message = "Tavsif 500 harfdan oshmasligi kerak")
    private String description;

    @NotNull(message = "Guruh narxi majburiy")
    @DecimalMin(value = "0.0", inclusive = false, message = "Narx 0 dan katta bo'lishi kerak")
    private BigDecimal price;

    @NotNull(message = "O'qituvchi majburiy")
    private Long teacherId;

    @NotNull(message = "Filial majburiy")
    private Long branchId;

    private List<Long> studentIds = new ArrayList<>();

    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Start time must be in HH:MM format (e.g., 13:00)")
    private String startTime;

    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "End time must be in HH:MM format (e.g., 15:00)")
    private String endTime;

    private List<String> daysOfWeek = new ArrayList<>();
}
