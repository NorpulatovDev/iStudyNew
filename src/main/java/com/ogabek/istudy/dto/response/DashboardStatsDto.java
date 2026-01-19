package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DashboardStatsDto {
    private Long totalBranches;
    private Long totalUsers;
    private Long totalStudents;
    private Long totalTeachers;
    private Long totalGroups;
    private BigDecimal monthlyRevenue;
    private BigDecimal totalRevenue;

    public DashboardStatsDto() {}

    public DashboardStatsDto(Long totalBranches, Long totalUsers, Long totalStudents,
                           Long totalTeachers, Long totalGroups, BigDecimal monthlyRevenue, BigDecimal totalRevenue) {
        this.totalBranches = totalBranches;
        this.totalUsers = totalUsers;
        this.totalStudents = totalStudents;
        this.totalTeachers = totalTeachers;
        this.totalGroups = totalGroups;
        this.monthlyRevenue = monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }
}
