package com.ogabek.istudy.config;

import com.ogabek.istudy.entity.Branch;
import com.ogabek.istudy.entity.Role;
import com.ogabek.istudy.entity.User;
import com.ogabek.istudy.repository.BranchRepository;
import com.ogabek.istudy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        initializeData();
    }

    private void initializeData() {
        // Create default super admin if not exists
        if (!userRepository.existsByUsername("superadmin")) {
            User superAdmin = new User();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword(passwordEncoder.encode("admin123"));
            superAdmin.setRole(Role.SUPER_ADMIN);
            userRepository.save(superAdmin);
            log.info("Created default super admin user: superadmin/admin123");
        }

        // Create default branch if not exists
        if (branchRepository.count() == 0) {
            Branch defaultBranch = new Branch();
            defaultBranch.setName("Main Branch");
            defaultBranch.setAddress("Tashkent, Uzbekistan");
            Branch savedBranch = branchRepository.save(defaultBranch);
            log.info("Created default branch: {}", defaultBranch.getName());

            // Create default branch admin
            if (!userRepository.existsByUsername("admin")) {
                User branchAdmin = new User();
                branchAdmin.setUsername("admin");
                branchAdmin.setPassword(passwordEncoder.encode("admin123"));
                branchAdmin.setRole(Role.ADMIN);
                branchAdmin.setBranch(savedBranch);
                userRepository.save(branchAdmin);
                log.info("Created default branch admin user: admin/admin123");
            }
        }
    }
}
