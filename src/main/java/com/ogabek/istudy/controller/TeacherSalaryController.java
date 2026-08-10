package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.CreateSalaryPaymentRequest;
import com.ogabek.istudy.dto.request.ReverseSalaryPaymentRequest;
import com.ogabek.istudy.dto.request.SalaryPeriodActionRequest;
import com.ogabek.istudy.dto.response.SalaryCalculationDto;
import com.ogabek.istudy.dto.response.TeacherSalaryHistoryDto;
import com.ogabek.istudy.dto.response.TeacherSalaryPaymentDto;
import com.ogabek.istudy.dto.response.TeacherSalaryPeriodDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.TeacherSalaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // ==================================================================
    // Hisoblash (o'zgarmagan endpointlar)
    // ==================================================================

    @GetMapping("/calculate/teacher/{teacherId}")
    public ResponseEntity<SalaryCalculationDto> calculateTeacherSalary(
            @PathVariable Long teacherId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false, defaultValue = "false") boolean includeDrift) {

        SalaryCalculationDto calculation = teacherSalaryService.calculateTeacherSalary(teacherId, year, month,
                includeDrift);

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

    // ==================================================================
    // Daftar yozuvlari (o'zgarmagan endpointlar)
    // ==================================================================

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

    /**
     * Eskicha ishlaydi, lekin davr yopilgan bo'lsa yozuv o'chirilmaydi —
     * o'rniga teskari yozuv qo'shiladi (tarix saqlanadi).
     */
    @DeleteMapping("/payments/{paymentId}")
    public ResponseEntity<Void> deleteSalaryPayment(@PathVariable Long paymentId) {
        teacherSalaryService.deleteSalaryPayment(paymentId);
        return ResponseEntity.ok().build();
    }

    // ==================================================================
    // Yangi: teskari yozuv
    // ==================================================================

    /** To'lovni bekor qiladi — o'chirmasdan, sababi bilan. */
    @PostMapping("/payments/{paymentId}/reverse")
    public ResponseEntity<TeacherSalaryPaymentDto> reverseSalaryPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody ReverseSalaryPaymentRequest request) {

        TeacherSalaryPaymentDto reversal = teacherSalaryService.reverseSalaryPayment(paymentId, request.getReason());

        if (!branchAccessControl.hasAccessToBranch(reversal.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(reversal);
    }

    // ==================================================================
    // Yangi: davr boshqaruvi
    // ==================================================================

    @GetMapping("/periods/teacher/{teacherId}")
    public ResponseEntity<List<TeacherSalaryPeriodDto>> getPeriodsByTeacher(@PathVariable Long teacherId) {
        List<TeacherSalaryPeriodDto> periods = teacherSalaryService.getPeriodsByTeacher(teacherId);

        if (!periods.isEmpty() && !branchAccessControl.hasAccessToBranch(periods.get(0).getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(periods);
    }

    @GetMapping("/periods/teacher/{teacherId}/month")
    public ResponseEntity<TeacherSalaryPeriodDto> getPeriod(
            @PathVariable Long teacherId,
            @RequestParam int year,
            @RequestParam int month) {

        TeacherSalaryPeriodDto period = teacherSalaryService.getPeriod(teacherId, year, month);

        if (!branchAccessControl.hasAccessToBranch(period.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(period);
    }

    /** Oyni qo'lda yopish — summani shu paytdan boshlab muzlatadi. */
    @PostMapping("/periods/close")
    public ResponseEntity<TeacherSalaryPeriodDto> closePeriod(
            @Valid @RequestBody SalaryPeriodActionRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TeacherSalaryPeriodDto period = teacherSalaryService.closePeriod(
                request.getTeacherId(), request.getYear(), request.getMonth());
        return ResponseEntity.ok(period);
    }

    /**
     * Yopilgan oyni qayta ochish. Faqat SUPER_ADMIN, sabab majburiy —
     * amal auditga yoziladi.
     */
    @PostMapping("/periods/reopen")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TeacherSalaryPeriodDto> reopenPeriod(
            @Valid @RequestBody SalaryPeriodActionRequest request) {

        TeacherSalaryPeriodDto period = teacherSalaryService.reopenPeriod(
                request.getTeacherId(), request.getYear(), request.getMonth(), request.getReason());
        return ResponseEntity.ok(period);
    }
}
