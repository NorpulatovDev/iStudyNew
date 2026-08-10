package com.ogabek.istudy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * To'lovni bekor qilish (teskari yozuv qo'shish) so'rovi.
 */
@Getter
@Setter
public class ReverseSalaryPaymentRequest {

    @NotBlank(message = "Bekor qilish sababi majburiy")
    @Size(max = 500, message = "Sabab 500 harfdan kam bo'lishi kerak")
    private String reason;
}
