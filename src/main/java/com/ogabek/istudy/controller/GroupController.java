package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.CreateGroupRequest;
import com.ogabek.istudy.dto.response.GroupDto;
import com.ogabek.istudy.dto.response.StudentDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class GroupController {

    private final GroupService groupService;
    private final BranchAccessControl branchAccessControl;

    @GetMapping
    public ResponseEntity<List<GroupDto>> getGroupsByBranch(@RequestParam Long branchId) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<GroupDto> groups = groupService.getGroupsByBranch(branchId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupDto> getGroupById(@PathVariable Long id,
                                                 @RequestParam(required = false) Integer year,
                                                 @RequestParam(required = false) Integer month) {
        GroupDto group;
        if (year != null && month != null) {
            group = groupService.getGroupById(id, year, month);
        } else {
            group = groupService.getGroupById(id);
        }
        
        if (!branchAccessControl.hasAccessToBranch(group.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(group);
    }

    @GetMapping("/by-teacher")
    public ResponseEntity<List<GroupDto>> getGroupsByTeacher(@RequestParam Long teacherId) {
        List<GroupDto> groups = groupService.getGroupsByTeacher(teacherId);

        if (!groups.isEmpty() && !branchAccessControl.hasAccessToBranch(groups.get(0).getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}/unpaid-students")
    public ResponseEntity<List<StudentDto>> getUnpaidStudentsByGroup(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        GroupDto group = groupService.getGroupById(id);
        if (!branchAccessControl.hasAccessToBranch(group.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<StudentDto> unpaidStudents = groupService.getUnpaidStudentsByGroup(id, year, month);
        return ResponseEntity.ok(unpaidStudents);
    }

    @PostMapping
    public ResponseEntity<GroupDto> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        GroupDto group = groupService.createGroup(request);
        return ResponseEntity.ok(group);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupDto> updateGroup(@PathVariable Long id,
                                                @Valid @RequestBody CreateGroupRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        GroupDto existingGroup = groupService.getGroupById(id);
        if (!branchAccessControl.hasAccessToBranch(existingGroup.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        GroupDto group = groupService.updateGroup(id, request);
        return ResponseEntity.ok(group);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        GroupDto group = groupService.getGroupById(id);
        if (!branchAccessControl.hasAccessToBranch(group.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        groupService.deleteGroup(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/students/{studentId}")
    public ResponseEntity<GroupDto> addStudentToGroup(
            @PathVariable Long groupId,
            @PathVariable Long studentId) {
        GroupDto updatedGroup = groupService.addStudentToGroup(groupId, studentId);
        return ResponseEntity.ok(updatedGroup);
    }

    @DeleteMapping("/{groupId}/students/{studentId}")
    public ResponseEntity<GroupDto> removeStudentFromGroup(
            @PathVariable Long groupId,
            @PathVariable Long studentId) {
        GroupDto updatedGroup = groupService.removeStudentFromGroup(groupId, studentId);
        return ResponseEntity.ok(updatedGroup);
    }

    @GetMapping("/search")
    public ResponseEntity<List<GroupDto>> searchGroups(@RequestParam Long branchId,
                                                       @RequestParam(required = false) String name) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<GroupDto> groups;
        if (name != null && !name.trim().isEmpty()) {
            groups = groupService.searchGroupsByName(branchId, name);
        } else {
            groups = groupService.getGroupsByBranch(branchId);
        }

        return ResponseEntity.ok(groups);
    }
}
