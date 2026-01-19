package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.CreateBranchRequest;
import com.ogabek.istudy.dto.response.BranchDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class BranchController {

    private final BranchService branchService;
    private final BranchAccessControl branchAccessControl;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<BranchDto>> getAllBranches() {
        List<BranchDto> branches = branchService.getAllBranches();
        return ResponseEntity.ok(branches);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchDto> getBranchById(@PathVariable Long id) {
        if (!branchAccessControl.hasAccessToBranch(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        BranchDto branch = branchService.getBranchById(id);
        return ResponseEntity.ok(branch);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BranchDto> createBranch(@Valid @RequestBody CreateBranchRequest request) {
        BranchDto branch = branchService.createBranch(request);
        return ResponseEntity.ok(branch);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BranchDto> updateBranch(@PathVariable Long id,
                                                  @Valid @RequestBody CreateBranchRequest request) {
        BranchDto branch = branchService.updateBranch(id, request);
        return ResponseEntity.ok(branch);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok().build();
    }
}
