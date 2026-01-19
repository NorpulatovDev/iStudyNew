package com.ogabek.istudy.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class AttendanceDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long groupId;
    private String groupName;
    private LocalDate attendanceDate;
    private String status; // PRESENT or ABSENT
    private String note;
    private Long branchId;
    private String branchName;
    private LocalDateTime createdAt;
}