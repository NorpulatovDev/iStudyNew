package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class GroupDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Long teacherId;
    private String teacherName;
    private Long branchId;
    private String branchName;
    private List<StudentPaymentInfo> studentPayments;
    private LocalDateTime createdAt;

    private String startTime;
    private String endTime;
    private List<String> daysOfWeek;
    
    private int studentCount;
}
