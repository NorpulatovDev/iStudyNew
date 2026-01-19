package com.ogabek.istudy.security;

import com.ogabek.istudy.entity.User;
import com.ogabek.istudy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BranchAccessControl {

    private final UserRepository userRepository;

    public boolean hasAccessToBranch(Long branchId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Super admin has access to all branches
        if ("SUPER_ADMIN".equals(user.getRole().name())) {
            return true;
        }

        // Regular admin can only access their own branch
        if ("ADMIN".equals(user.getRole().name())) {
            return user.getBranch() != null && user.getBranch().getId().equals(branchId);
        }

        return false;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public Long getCurrentUserBranchId() {
        User user = getCurrentUser();
        return user.getBranch() != null ? user.getBranch().getId() : null;
    }

    public boolean isSuperAdmin() {
        User user = getCurrentUser();
        return "SUPER_ADMIN".equals(user.getRole().name());
    }
}
