package com.ogabek.istudy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceSummaryDto {
    private Long studentId;
    private String studentName;
    private String phoneNumber;
    private int totalPresent;
    private int totalAbsent;
    private int totalDays;
    private double attendancePercentage;
}