package com.ogabek.istudy.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Davrni qo'lda yopish yoki qayta ochish uchun so'rov.
 */
@Getter
@Setter
public class SalaryPeriodActionRequest {

    @NotNull(message = "O'qituvchi majburiy")
    private Long teacherId;

    @NotNull(message = "Yil majburiy")
    @Min(value = 2020, message = "Yil 2020 dan kichik bo'lmasligi kerak")
    @Max(value = 2100, message = "Yil 2100 dan katta bo'lmasligi kerak")
    private Integer year;

    @NotNull(message = "Oy majburiy")
    @Min(value = 1, message = "Oy 1-12 oralig'ida bo'lishi kerak")
    @Max(value = 12, message = "Oy 1-12 oralig'ida bo'lishi kerak")
    private Integer month;

    @NotNull(message = "Filial majburiy")
    private Long branchId;

    /** Qayta ochishda majburiy — nima uchun ochilayotgani audit uchun yoziladi. */
    @Size(max = 500, message = "Sabab 500 harfdan kam bo'lishi kerak")
    private String reason;
}
