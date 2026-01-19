package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.response.DashboardStatsDto;
import com.ogabek.istudy.repository.*;
import com.ogabek.istudy.security.BranchAccessControl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final PaymentRepository paymentRepository;
    private final BranchAccessControl branchAccessControl;

    public DashboardStatsDto getDashboardStats() {
        if (branchAccessControl.isSuperAdmin()) {
            return getSuperAdminStats();
        } else {
            return getBranchAdminStats();
        }
    }

    private DashboardStatsDto getSuperAdminStats() {
        Long totalBranches = branchRepository.count();
        Long totalUsers = userRepository.count();
        Long totalStudents = studentRepository.count();
        Long totalTeachers = teacherRepository.count();
        Long totalGroups = groupRepository.count();

        LocalDate now = LocalDate.now();
        BigDecimal monthlyRevenue = getTotalMonthlyRevenue(now.getYear(), now.getMonthValue());
        BigDecimal totalRevenue = getTotalRevenue();

        return new DashboardStatsDto(totalBranches, totalUsers, totalStudents,
                                   totalTeachers, totalGroups, monthlyRevenue, totalRevenue);
    }

    private DashboardStatsDto getBranchAdminStats() {
        Long branchId = branchAccessControl.getCurrentUserBranchId();
        if (branchId == null) {
            throw new RuntimeException("Branch admin must have a branch assigned");
        }

        Long totalBranches = 1L;
        Long totalUsers = (long) userRepository.findByBranchId(branchId).size();
        Long totalStudents = (long) studentRepository.findByBranchId(branchId).size();
        Long totalTeachers = (long) teacherRepository.findByBranchId(branchId).size();
        Long totalGroups = (long) groupRepository.findByBranchId(branchId).size();

        LocalDate now = LocalDate.now();
        BigDecimal monthlyRevenue = paymentRepository.sumMonthlyPayments(branchId, now.getYear(), now.getMonthValue());
        BigDecimal totalRevenue = getBranchTotalRevenue(branchId);

        return new DashboardStatsDto(totalBranches, totalUsers, totalStudents,
                                   totalTeachers, totalGroups, monthlyRevenue, totalRevenue);
    }

    private BigDecimal getTotalMonthlyRevenue(int year, int month) {
        return branchRepository.findAll().stream()
                .map(branch -> paymentRepository.sumMonthlyPayments(branch.getId(), year, month))
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b != null ? b : BigDecimal.ZERO));
    }

    private BigDecimal getTotalRevenue() {
        return branchRepository.findAll().stream()
                .map(branch -> getBranchTotalRevenue(branch.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getBranchTotalRevenue(Long branchId) {
        return paymentRepository.findByBranchId(branchId).stream()
                .map(payment -> payment.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
