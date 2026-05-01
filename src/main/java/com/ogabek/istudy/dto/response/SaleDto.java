package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class SaleDto {
    private Long id;
    private String itemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String description;
    private Long branchId;
    private String branchName;
    private int saleYear;
    private int saleMonth;
    private LocalDateTime createdAt;
}
