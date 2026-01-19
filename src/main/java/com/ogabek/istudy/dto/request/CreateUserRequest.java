package com.ogabek.istudy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    @NotBlank(message = "Foydalanuvchi nomi majburiy!")
    @Size(min = 3, max = 50, message = "Foydalanuvchi nomi 3-50 ta belgidan iborat bo'lishi kerak")
    private String username;
    
    @NotBlank(message = "Parol majburiy!")
    @Size(min = 6, message = "Parol kamida 6 ta belgidan iborat bo'lishi kerak")
    private String password;

    @NotBlank(message = "Rol majburiy!")
    @Pattern(regexp = "^(SUPER_ADMIN|ADMIN)$", message = "Rol SUPER_ADMIN yoki ADMIN bo'lishi kerak")
    private String role;
    
    private Long branchId;
}