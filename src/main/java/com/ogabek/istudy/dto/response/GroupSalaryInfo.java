package com.ogabek.istudy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupSalaryInfo {
    private Long groupId;
    private String groupName;
    private int studentCount;
    private BigDecimal totalGroupPayments;
    private int totalStudentsInGroup;
    private BigDecimal groupPrice;

    public GroupSalaryInfo(Long groupId, String groupName, int studentCount, BigDecimal totalGroupPayments) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.studentCount = studentCount;
        this.totalGroupPayments = totalGroupPayments;
        this.totalStudentsInGroup = studentCount;
        this.groupPrice = BigDecimal.ZERO;
    }
}
