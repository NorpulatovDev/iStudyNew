package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StudentDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String parentPhoneNumber;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdAt;

    // List of groups the student belongs to
    private List<GroupInfo> groups = new ArrayList<>();

    // Payment status fields
    private Boolean hasPaidInMonth;
    private BigDecimal totalPaidInMonth;
    private BigDecimal remainingAmount;
    private String paymentStatus; // "PAID", "PARTIAL", "UNPAID"
    private LocalDateTime lastPaymentDate;

    public StudentDto() {}

    public StudentDto(Long id, String firstName, String lastName, String phoneNumber,
                      String parentPhoneNumber, Long branchId, String branchName, LocalDateTime createdAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.parentPhoneNumber = parentPhoneNumber;
        this.branchId = branchId;
        this.branchName = branchName;
        this.createdAt = createdAt;
    }

    @Getter
    @Setter
    public static class GroupInfo {
        private Long id;
        private String name;
        private BigDecimal price;
        private String teacherName;

        public GroupInfo() {}

        public GroupInfo(Long id, String name, BigDecimal price, String teacherName) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.teacherName = teacherName;
        }
    }
}
