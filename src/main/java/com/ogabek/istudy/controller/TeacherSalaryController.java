package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.CreateSalaryPaymentRequest;
import com.ogabek.istudy.dto.response.SalaryCalculationDto;
import com.ogabek.istudy.dto.response.TeacherSalaryHistoryDto;
import com.ogabek.istudy.dto.response.TeacherSalaryPaymentDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.TeacherSalaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/teacher-salaries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class TeacherSalaryController {

    private final TeacherSalaryService teacherSalaryService;
    private final BranchAccessControl branchAccessControl;

    @GetMapping("/calculate/teacher/{teacherId}")
    public ResponseEntity<SalaryCalculationDto> calculateTeacherSalary(
            @PathVariable Long teacherId,
            @RequestParam int year,
            @RequestParam int month) {

        SalaryCalculationDto calculation = teacherSalaryService.calculateTeacherSalary(teacherId, year, month);

        if (!branchAccessControl.hasAccessToBranch(calculation.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(calculation);
    }

    @GetMapping("/calculate/branch/{branchId}")
    public ResponseEntity<List<SalaryCalculationDto>> calculateSalariesForBranch(
            @PathVariable Long branchId,
            @RequestParam int year,
            @RequestParam int month) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<SalaryCalculationDto> calculations = teacherSalaryService.calculateSalariesForBranch(branchId, year,
                month);
        return ResponseEntity.ok(calculations);
    }

    @PostMapping("/payments")
    public ResponseEntity<TeacherSalaryPaymentDto> createSalaryPayment(
            @Valid @RequestBody CreateSalaryPaymentRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TeacherSalaryPaymentDto payment = teacherSalaryService.createSalaryPayment(request);
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/subtract")
    public ResponseEntity<TeacherSalaryPaymentDto> subtractFromSalary(
            @Valid @RequestBody CreateSalaryPaymentRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TeacherSalaryPaymentDto payment = teacherSalaryService.subtractFromSalary(request);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/payments/branch/{branchId}")
    public ResponseEntity<List<TeacherSalaryPaymentDto>> getSalaryPaymentsByBranch(@PathVariable Long branchId) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<TeacherSalaryPaymentDto> payments = teacherSalaryService.getSalaryPaymentsByBranch(branchId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/payments/teacher/{teacherId}")
    public ResponseEntity<List<TeacherSalaryPaymentDto>> getSalaryPaymentsByTeacher(@PathVariable Long teacherId) {
        List<TeacherSalaryPaymentDto> payments = teacherSalaryService.getSalaryPaymentsByTeacher(teacherId);

        if (!payments.isEmpty() && !branchAccessControl.hasAccessToBranch(payments.get(0).getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(payments);
    }

    @GetMapping("/payments/teacher/{teacherId}/month")
    public ResponseEntity<List<TeacherSalaryPaymentDto>> getPaymentsForTeacherAndMonth(
            @PathVariable Long teacherId,
            @RequestParam int year,
            @RequestParam int month) {

        List<TeacherSalaryPaymentDto> payments = teacherSalaryService.getPaymentsForTeacherAndMonth(teacherId, year,
                month);

        if (!payments.isEmpty() && !branchAccessControl.hasAccessToBranch(payments.get(0).getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(payments);
    }

    @GetMapping("/history/teacher/{teacherId}")
    public ResponseEntity<List<TeacherSalaryHistoryDto>> getTeacherSalaryHistory(@PathVariable Long teacherId) {
        List<TeacherSalaryHistoryDto> history = teacherSalaryService.getTeacherSalaryHistory(teacherId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/remaining/teacher/{teacherId}")
    public ResponseEntity<BigDecimal> getRemainingAmountForTeacher(
            @PathVariable Long teacherId,
            @RequestParam int year,
            @RequestParam int month) {

        BigDecimal remaining = teacherSalaryService.getRemainingAmountForTeacher(teacherId, year, month);
        return ResponseEntity.ok(remaining);
    }

    @DeleteMapping("/payments/{paymentId}")
    public ResponseEntity<Void> deleteSalaryPayment(@PathVariable Long paymentId) {
        teacherSalaryService.deleteSalaryPayment(paymentId);
        return ResponseEntity.ok().build();
    }
}
