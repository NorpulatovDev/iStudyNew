package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.BulkAttendanceRequest;
import com.ogabek.istudy.dto.request.MarkAttendanceRequest;
import com.ogabek.istudy.dto.response.AttendanceDto;
import com.ogabek.istudy.dto.response.BulkAttendanceResponse;
import com.ogabek.istudy.dto.response.StudentAttendanceSummaryDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final BranchAccessControl branchAccessControl;

    // NEW: Mark bulk attendance (all students at once)
    @PostMapping("/bulk")
    public ResponseEntity<BulkAttendanceResponse> markBulkAttendance(@Valid @RequestBody BulkAttendanceRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        BulkAttendanceResponse response = attendanceService.markBulkAttendance(request);
        return ResponseEntity.ok(response);
    }

    // Mark attendance for a single student (optional - can still use this)
    @PostMapping
    public ResponseEntity<AttendanceDto> markAttendance(@Valid @RequestBody MarkAttendanceRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        AttendanceDto attendance = attendanceService.markAttendance(request);
        return ResponseEntity.ok(attendance);
    }

    // Get attendance for a group on specific date
    @GetMapping("/group/{groupId}/date/{date}")
    public ResponseEntity<List<AttendanceDto>> getAttendanceByGroupAndDate(
            @PathVariable Long groupId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<AttendanceDto> attendances = attendanceService.getAttendanceByGroupAndDate(groupId, date);
        return ResponseEntity.ok(attendances);
    }

    // Get student's attendance for specific month
    @GetMapping("/student/{studentId}/group/{groupId}/month")
    public ResponseEntity<List<AttendanceDto>> getStudentAttendanceByMonth(
            @PathVariable Long studentId,
            @PathVariable Long groupId,
            @RequestParam int year,
            @RequestParam int month) {
        
        List<AttendanceDto> attendances = attendanceService.getStudentAttendanceByMonth(studentId, groupId, year, month);
        return ResponseEntity.ok(attendances);
    }

    // Get attendance summary for entire group for specific month
    @GetMapping("/group/{groupId}/summary")
    public ResponseEntity<List<StudentAttendanceSummaryDto>> getGroupAttendanceSummary(
            @PathVariable Long groupId,
            @RequestParam int year,
            @RequestParam int month) {
        
        List<StudentAttendanceSummaryDto> summary = attendanceService.getGroupAttendanceSummary(groupId, year, month);
        return ResponseEntity.ok(summary);
    }

    // Delete attendance record
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.ok().build();
    }
}