package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.CreateTeacherRequest;
import com.ogabek.istudy.dto.response.TeacherDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class TeacherController {

    private final TeacherService teacherService;
    private final BranchAccessControl branchAccessControl;

    @GetMapping
    public ResponseEntity<List<TeacherDto>> getTeachersByBranch(@RequestParam Long branchId) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(403).build();
        }
        List<TeacherDto> teachers = teacherService.getTeachersByBranch(branchId);
        return ResponseEntity.ok(teachers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeacherDto> getTeacherById(@PathVariable Long id) {
        TeacherDto teacher = teacherService.getTeacherById(id);
        if (!branchAccessControl.hasAccessToBranch(teacher.getBranchId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(teacher);
    }

    @PostMapping
    public ResponseEntity<TeacherDto> createTeacher(@Valid @RequestBody CreateTeacherRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(403).build();
        }
        TeacherDto teacher = teacherService.createTeacher(request);
        return ResponseEntity.ok(teacher);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeacherDto> updateTeacher(@PathVariable Long id,
                                                    @Valid @RequestBody CreateTeacherRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(403).build();
        }

        TeacherDto existingTeacher = teacherService.getTeacherById(id);
        if (!branchAccessControl.hasAccessToBranch(existingTeacher.getBranchId())) {
            return ResponseEntity.status(403).build();
        }

        TeacherDto teacher = teacherService.updateTeacher(id, request);
        return ResponseEntity.ok(teacher);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        TeacherDto teacher = teacherService.getTeacherById(id);
        if (!branchAccessControl.hasAccessToBranch(teacher.getBranchId())) {
            return ResponseEntity.status(403).build();
        }
        teacherService.deleteTeacher(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<TeacherDto>> searchTeachers(@RequestParam Long branchId,
                                                          @RequestParam(required = false) String name) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(403).build();
        }

        List<TeacherDto> teachers;
        if (name != null && !name.trim().isEmpty()) {
            teachers = teacherService.searchTeachersByName(branchId, name);
        } else {
            teachers = teacherService.getTeachersByBranch(branchId);
        }

        return ResponseEntity.ok(teachers);
    }

    @GetMapping("/by-salary-type")
    public ResponseEntity<List<TeacherDto>> getTeachersBySalaryType(@RequestParam Long branchId,
                                                                   @RequestParam String salaryType) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(403).build();
        }

        List<TeacherDto> teachers = teacherService.getTeachersBySalaryType(branchId, salaryType);
        return ResponseEntity.ok(teachers);
    }
}
