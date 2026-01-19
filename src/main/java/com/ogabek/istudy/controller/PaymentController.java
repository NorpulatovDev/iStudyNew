package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.CreatePaymentRequest;
import com.ogabek.istudy.dto.request.UpdatePaymentRequest;
import com.ogabek.istudy.dto.response.PaymentDto;
import com.ogabek.istudy.dto.response.UnpaidStudentDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.PaymentService;
import com.ogabek.istudy.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentController {

    private final PaymentService paymentService;
    private final StudentService studentService;
    private final BranchAccessControl branchAccessControl;

    @GetMapping
    public ResponseEntity<List<PaymentDto>> getPaymentsByBranch(@RequestParam Long branchId) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<PaymentDto> payments = paymentService.getPaymentsByBranch(branchId);
        return ResponseEntity.ok(payments);
    }

    // NEW: Get payments by category
    @GetMapping("/by-category")
    public ResponseEntity<List<PaymentDto>> getPaymentsByCategory(
            @RequestParam Long branchId,
            @RequestParam String category) {
        
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PaymentDto> payments = paymentService.getPaymentsByBranchAndCategory(branchId, category);
        return ResponseEntity.ok(payments);
    }

    // NEW: Get payments by category and month
    @GetMapping("/by-category-and-month")
    public ResponseEntity<List<PaymentDto>> getPaymentsByCategoryAndMonth(
            @RequestParam Long branchId,
            @RequestParam String category,
            @RequestParam int year,
            @RequestParam int month) {
        
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PaymentDto> payments = paymentService.getPaymentsByBranchAndCategoryAndMonth(branchId, category, year, month);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/unpaid")
    public ResponseEntity<List<UnpaidStudentDto>> getUnpaidStudents(
            @RequestParam Long branchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(403).build();
        }
        List<UnpaidStudentDto> students = studentService.getUnpaidStudents(branchId, year, month);
        return ResponseEntity.ok(students);
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<List<PaymentDto>> getPaymentsByDateRange(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PaymentDto> payments = paymentService.getPaymentsByDateRange(branchId, startDate, endDate);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/by-month")
    public ResponseEntity<List<PaymentDto>> getPaymentsByMonth(
            @RequestParam Long branchId,
            @RequestParam int year,
            @RequestParam int month) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PaymentDto> payments = paymentService.getPaymentsByMonth(branchId, year, month);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<PaymentDto>> getRecentPayments(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "20") int limit) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PaymentDto> payments = paymentService.getRecentPayments(branchId, limit);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<PaymentDto>> getPaymentsByStudent(@PathVariable Long studentId) {
        List<PaymentDto> payments = paymentService.getPaymentsByStudent(studentId);
        if (!payments.isEmpty() && !branchAccessControl.hasAccessToBranch(payments.get(0).getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPaymentById(@PathVariable Long id) {
        PaymentDto payment = paymentService.getPaymentById(id);
        if (!branchAccessControl.hasAccessToBranch(payment.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(payment);
    }

    @PostMapping
    public ResponseEntity<PaymentDto> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        PaymentDto payment = paymentService.createPayment(request);
        return ResponseEntity.ok(payment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentDto> updatePaymentAmount(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentRequest request) {

        PaymentDto existingPayment = paymentService.getPaymentById(id);
        if (!branchAccessControl.hasAccessToBranch(existingPayment.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PaymentDto updatedPayment = paymentService.updatePaymentAmount(id, request.getAmount());
        return ResponseEntity.ok(updatedPayment);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PaymentDto>> searchPayments(
            @RequestParam Long branchId,
            @RequestParam(required = false) String studentName) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<PaymentDto> payments;
        if (studentName != null && !studentName.trim().isEmpty()) {
            payments = paymentService.searchPaymentsByStudentName(branchId, studentName);
        } else {
            payments = paymentService.getPaymentsByBranch(branchId);
        }

        return ResponseEntity.ok(payments);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        PaymentDto payment = paymentService.getPaymentById(id);
        if (!branchAccessControl.hasAccessToBranch(payment.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        paymentService.deletePayment(id);
        return ResponseEntity.ok().build();
    }
}