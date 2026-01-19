package com.ogabek.istudy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class BulkAttendanceResponse {
    private Long groupId;
    private String groupName;
    private LocalDate attendanceDate;
    private int totalStudents;
    private int totalPresent;
    private int totalAbsent;
    private List<AttendanceDto> attendances;
    private String message;
}