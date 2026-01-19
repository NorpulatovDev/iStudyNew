package com.ogabek.istudy.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class BulkAttendanceRequest {
    @NotNull(message = "Guruh ID majburiy")
    private Long groupId;

    @NotNull(message = "Sana majburiy")
    private LocalDate attendanceDate;

    @NotNull(message = "Filial ID majburiy")
    private Long branchId;

    @NotEmpty(message = "Kamida bitta o'quvchi davomat ma'lumoti kerak")
    @Valid
    private List<StudentAttendanceItem> attendances;

    @Getter
    @Setter
    public static class StudentAttendanceItem {
        @NotNull(message = "O'quvchi ID majburiy")
        private Long studentId;

        @NotNull(message = "Status majburiy (PRESENT yoki ABSENT)")
        private String status; // PRESENT or ABSENT

        private String note;
    }
}