package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateUserRequest;
import com.ogabek.istudy.dto.request.LoginRequest;
import com.ogabek.istudy.dto.request.UpdateUserRequest;
import com.ogabek.istudy.dto.response.JwtResponse;
import com.ogabek.istudy.dto.response.UserDto;
import com.ogabek.istudy.entity.Branch;
import com.ogabek.istudy.entity.RefreshToken;
import com.ogabek.istudy.entity.Role;
import com.ogabek.istudy.entity.User;
import com.ogabek.istudy.repository.BranchRepository;
import com.ogabek.istudy.repository.UserRepository;
import com.ogabek.istudy.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtUtils.generateJwtToken(userDetails.getUsername());

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        JwtResponse response = new JwtResponse(jwt, refreshToken.getToken());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole().name());

        if (user.getBranch() != null) {
            response.setBranchId(user.getBranch().getId());
            response.setBranchName(user.getBranch().getName());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAllWithBranch().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<UserDto> getUsersByBranch(Long branchId) {
        return userRepository.findByBranchId(branchId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Foydalanuvchi nomi allaqachon mavjud: " + request.getUsername());
        }

        // Validate role
        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Noto'g'ri rol: " + request.getRole() + ". SUPER_ADMIN yoki ADMIN bo'lishi kerak.");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        // Set branch if provided and role is ADMIN
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Filial topilmadi: " + request.getBranchId()));
            user.setBranch(branch);
        } else if (role == Role.ADMIN) {
            throw new RuntimeException("ADMIN roli uchun filial majburiy");
        }

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Foydalanuvchi topilmadi: " + id));

        if (user.getRole() == Role.SUPER_ADMIN) {
            long superAdminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.SUPER_ADMIN && !u.isDeleted())
                    .count();

            if (superAdminCount <= 1) {
                throw new RuntimeException("Oxirgi super admin foydalanuvchisini o'chirish mumkin emas.");
            }
        }

        // Delete refresh token first
        refreshTokenService.deleteByUserId(id);

        // Soft delete by setting deleted flag
        user.setDeleted(true);
        userRepository.save(user);
    }

    public JwtResponse refreshToken(String refreshTokenStr) {
        return refreshTokenService.findByToken(refreshTokenStr)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateJwtToken(user.getUsername());
                    JwtResponse response = new JwtResponse(token, refreshTokenStr);
                    response.setUserId(user.getId());
                    response.setUsername(user.getUsername());
                    response.setRole(user.getRole().name());

                    if (user.getBranch() != null) {
                        response.setBranchId(user.getBranch().getId());
                        response.setBranchName(user.getBranch().getName());
                    }

                    return response;
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    public void logout(Long userId) {
        refreshTokenService.deleteByUserId(userId);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (!user.getUsername().equals(request.getUsername()) &&
                userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        user.setUsername(request.getUsername());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user.setRole(Role.valueOf(request.getRole().toUpperCase()));

        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));
            user.setBranch(branch);
        } else {
            user.setBranch(null);
        }

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole().name());

        if (user.getBranch() != null) {
            dto.setBranchId(user.getBranch().getId());
            dto.setBranchName(user.getBranch().getName());
        }

        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
