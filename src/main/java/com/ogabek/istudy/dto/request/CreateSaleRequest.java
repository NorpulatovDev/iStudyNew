package com.ogabek.istudy.dto.request;

import com.ogabek.istudy.entity.PaymentCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateSaleRequest {

    @NotBlank(message = "Mahsulot nomi majburiy")
    @Size(max = 255, message = "Mahsulot nomi 255 harfdan kam bo'lishi kerak")
    private String itemName;

    @NotNull(message = "Miqdor majburiy")
    @Min(value = 1, message = "Miqdor kamida 1 bo'lishi kerak")
    private Integer quantity;

    @NotNull(message = "Narx majburiy")
    @DecimalMin(value = "0.0", inclusive = false, message = "Narx 0 dan katta bo'lishi kerak")
    private BigDecimal unitPrice;

    @NotNull(message = "To'lov turi majburiy")
    private PaymentCategory paymentMethod;

    @Size(max = 255, message = "Tavsif 255 harfdan kam bo'lishi kerak")
    private String description;

    @NotNull(message = "Filial ID majburiy")
    private Long branchId;
}
