package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.CreateStudentRequest;
import com.ogabek.istudy.dto.response.StudentDto;
import com.ogabek.istudy.dto.response.UnpaidStudentDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class StudentController {

    private final StudentService studentService;
    private final BranchAccessControl branchAccessControl;

    @GetMapping
    public ResponseEntity<List<StudentDto>> getStudentsByBranch(
            @RequestParam Long branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(403).build();
        }

        List<StudentDto> students;
        if (year != null && month != null) {
            students = studentService.getStudentsByBranch(branchId, year, month);
        } else {
            students = studentService.getStudentsByBranch(branchId);
        }

        return ResponseEntity.ok(students);
    }

    @GetMapping("/by-group")
    public ResponseEntity<List<StudentDto>> getStudentsByGroup(
            @RequestParam Long groupId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        List<StudentDto> students = studentService.getStudentsByGroup(groupId, year, month);

        if (!students.isEmpty() && !branchAccessControl.hasAccessToBranch(students.get(0).getBranchId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(students);
    }

    @GetMapping("/unpaid")
    public ResponseEntity<List<UnpaidStudentDto>> getUnpaidStudents(
            @RequestParam Long branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(403).build();
        }
        List<UnpaidStudentDto> students = studentService.getUnpaidStudents(branchId, year, month);
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentDto> getStudentById(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        StudentDto student;
        if (year != null && month != null) {
            student = studentService.getStudentById(id, year, month);
        } else {
            student = studentService.getStudentById(id);
        }

        if (!branchAccessControl.hasAccessToBranch(student.getBranchId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(student);
    }

    @PostMapping
    public ResponseEntity<StudentDto> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(403).build();
        }
        StudentDto student = studentService.createStudent(request);
        return ResponseEntity.ok(student);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDto> updateStudent(@PathVariable Long id, @Valid @RequestBody CreateStudentRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(403).build();
        }

        StudentDto existingStudent = studentService.getStudentById(id);
        if (!branchAccessControl.hasAccessToBranch(existingStudent.getBranchId())) {
            return ResponseEntity.status(403).build();
        }

        StudentDto student = studentService.updateStudent(id, request);
        return ResponseEntity.ok(student);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        StudentDto student = studentService.getStudentById(id);
        if (!branchAccessControl.hasAccessToBranch(student.getBranchId())) {
            return ResponseEntity.status(403).build();
        }
        studentService.deleteStudent(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<StudentDto>> searchStudents(
            @RequestParam Long branchId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(403).build();
        }

        List<StudentDto> students;
        if (name != null && !name.trim().isEmpty()) {
            students = studentService.searchStudentsByName(branchId, name);
        } else {
            if (year != null && month != null) {
                students = studentService.getStudentsByBranch(branchId, year, month);
            } else {
                students = studentService.getStudentsByBranch(branchId);
            }
        }

        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}/payment-history")
    public ResponseEntity<?> getStudentPaymentHistory(@PathVariable Long id) {
        StudentDto student = studentService.getStudentById(id);
        if (!branchAccessControl.hasAccessToBranch(student.getBranchId())) {
            return ResponseEntity.status(403).build();
        }

        var paymentHistory = studentService.getStudentPaymentHistory(id);
        return ResponseEntity.ok(paymentHistory);
    }

    @GetMapping("/{id}/groups")
    public ResponseEntity<?> getStudentGroups(@PathVariable Long id) {
        StudentDto student = studentService.getStudentById(id);
        if (!branchAccessControl.hasAccessToBranch(student.getBranchId())) {
            return ResponseEntity.status(403).build();
        }

        var groups = studentService.getStudentGroups(id);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStudentStatistics(@RequestParam Long branchId) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(403).build();
        }

        var statistics = studentService.getStudentStatistics(branchId);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<StudentDto>> getRecentStudents(@RequestParam Long branchId, @RequestParam(defaultValue = "10") int limit) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(403).build();
        }

        List<StudentDto> recentStudents = studentService.getRecentStudents(branchId, limit);
        return ResponseEntity.ok(recentStudents);
    }

    @GetMapping("/by-payment-status")
    public ResponseEntity<List<StudentDto>> getStudentsByPaymentStatus(
            @RequestParam Long branchId,
            @RequestParam String paymentStatus,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(403).build();
        }

        List<StudentDto> allStudents;
        if (year != null && month != null) {
            allStudents = studentService.getStudentsByBranch(branchId, year, month);
        } else {
            allStudents = studentService.getStudentsByBranch(branchId);
        }

        List<StudentDto> filteredStudents = allStudents.stream()
                .filter(student -> paymentStatus.equalsIgnoreCase(student.getPaymentStatus()))
                .toList();

        return ResponseEntity.ok(filteredStudents);
    }
}
