package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UnpaidStudentDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String parentPhoneNumber;
    private BigDecimal remainingAmount;
    private String groupName;
    private Long groupId;

    public UnpaidStudentDto(Long id, String firstName, String lastName, String phoneNumber,
                           String parentPhoneNumber, BigDecimal remainingAmount, Long groupId, String groupName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.parentPhoneNumber = parentPhoneNumber;
        this.remainingAmount = remainingAmount;
        this.groupId = groupId;
        this.groupName = groupName;
    }
}
