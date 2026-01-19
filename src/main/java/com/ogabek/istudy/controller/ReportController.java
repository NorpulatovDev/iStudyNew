package com.ogabek.istudy.controller;

import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReportController {

    private final ReportService reportService;
    private final BranchAccessControl branchAccessControl;

    @GetMapping("/payments/daily")
    public ResponseEntity<Map<String, Object>> getDailyPaymentReport(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> report = reportService.getDailyPaymentReport(branchId, date);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/payments/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyPaymentReport(
            @RequestParam Long branchId,
            @RequestParam Integer year,
            @RequestParam Integer month) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> report = reportService.getMonthlyPaymentReport(branchId, year, month);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/payments/range")
    public ResponseEntity<Map<String, Object>> getPaymentRangeReport(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> report = reportService.getPaymentRangeReport(branchId, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/expenses/daily")
    public ResponseEntity<Map<String, Object>> getDailyExpenseReport(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> report = reportService.getDailyExpenseReport(branchId, date);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/expenses/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyExpenseReport(
            @RequestParam Long branchId,
            @RequestParam Integer year,
            @RequestParam Integer month) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> report = reportService.getMonthlyExpenseReport(branchId, year, month);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/expenses/range")
    public ResponseEntity<Map<String, Object>> getExpenseRangeReport(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> report = reportService.getExpenseRangeReport(branchId, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/expenses/all-time")
    public ResponseEntity<Map<String, Object>> getAllTimeExpenseReport(@RequestParam Long branchId) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> report = reportService.getAllTimeExpenseReport(branchId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/financial/summary")
    public ResponseEntity<Map<String, Object>> getFinancialSummary(
            @RequestParam Long branchId,
            @RequestParam Integer year,
            @RequestParam Integer month) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> summary = reportService.getFinancialSummary(branchId, year, month);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/financial/summary-range")
    public ResponseEntity<Map<String, Object>> getFinancialSummaryRange(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> summary = reportService.getFinancialSummaryRange(branchId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }
}
