package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.CreateUserRequest;
import com.ogabek.istudy.dto.request.UpdateUserRequest;
import com.ogabek.istudy.dto.response.UserDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;
    private final BranchAccessControl branchAccessControl;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDto user = userService.createUser(request);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<UserDto>> getUsersByBranch(@PathVariable Long branchId) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<UserDto> users = userService.getUsersByBranch(branchId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);

        if (!user.getId().equals(branchAccessControl.getCurrentUser().getId()) &&
            !branchAccessControl.isSuperAdmin() &&
            (user.getBranchId() == null || !branchAccessControl.hasAccessToBranch(user.getBranchId()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (id.equals(branchAccessControl.getCurrentUser().getId())) {
            return ResponseEntity.badRequest().build();
        }

        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id,
                                              @Valid @RequestBody UpdateUserRequest request) {
        if (id.equals(branchAccessControl.getCurrentUser().getId())) {
            return ResponseEntity.badRequest().build();
        }

        UserDto user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }
}
