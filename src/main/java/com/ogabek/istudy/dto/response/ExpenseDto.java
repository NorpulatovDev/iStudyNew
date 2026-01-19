package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ExpenseDto {
    private Long id;
    private String description;
    private BigDecimal amount;
    private String category;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdAt;
}
