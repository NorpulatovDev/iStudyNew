package com.ogabek.istudy.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MarkAttendanceRequest {
    @NotNull(message = "O'quvchi ID majburiy")
    private Long studentId;

    @NotNull(message = "Guruh ID majburiy")
    private Long groupId;

    @NotNull(message = "Sana majburiy")
    private LocalDate attendanceDate;

    @NotNull(message = "Status majburiy (PRESENT yoki ABSENT)")
    private String status; // PRESENT or ABSENT

    private String note;

    @NotNull(message = "Filial ID majburiy")
    private Long branchId;
}