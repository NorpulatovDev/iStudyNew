package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.CreateExpenseRequest;
import com.ogabek.istudy.dto.response.ExpenseDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExpenseController {

    private final ExpenseService expenseService;
    private final BranchAccessControl branchAccessControl;

    @GetMapping
    public ResponseEntity<List<ExpenseDto>> getExpensesByBranch(@RequestParam Long branchId) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<ExpenseDto> expenses = expenseService.getExpensesByBranch(branchId);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<ExpenseDto>> getExpensesByMonth(
            @RequestParam Long branchId,
            @RequestParam int year,
            @RequestParam int month) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<ExpenseDto> expenses = expenseService.getExpensesByMonth(branchId, year, month);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/daily")
    public ResponseEntity<List<ExpenseDto>> getExpensesByDate(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<ExpenseDto> expenses = expenseService.getExpensesByDate(branchId, date);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/monthly/summary")
    public ResponseEntity<Map<String, Object>> getMonthlyExpensesSummary(
            @RequestParam Long branchId,
            @RequestParam int year,
            @RequestParam int month) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<ExpenseDto> expenses = expenseService.getExpensesByMonth(branchId, year, month);
        BigDecimal total = expenseService.getMonthlyExpensesTotal(branchId, year, month);

        Map<String, Object> summary = new HashMap<>();
        summary.put("expenses", expenses);
        summary.put("total", total);
        summary.put("count", expenses.size());
        summary.put("year", year);
        summary.put("month", month);
        summary.put("branchId", branchId);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/daily/summary")
    public ResponseEntity<Map<String, Object>> getDailyExpensesSummary(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<ExpenseDto> expenses = expenseService.getExpensesByDate(branchId, date);
        BigDecimal total = expenseService.getDailyExpensesTotal(branchId, date);

        Map<String, Object> summary = new HashMap<>();
        summary.put("expenses", expenses);
        summary.put("total", total);
        summary.put("count", expenses.size());
        summary.put("date", date);
        summary.put("branchId", branchId);

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/monthly/total")
    public ResponseEntity<Map<String, Object>> getMonthlyExpensesTotal(
            @RequestParam Long branchId,
            @RequestParam int year,
            @RequestParam int month) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BigDecimal total = expenseService.getMonthlyExpensesTotal(branchId, year, month);

        Map<String, Object> response = new HashMap<>();
        response.put("total", total);
        response.put("year", year);
        response.put("month", month);
        response.put("branchId", branchId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/daily/total")
    public ResponseEntity<Map<String, Object>> getDailyExpensesTotal(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BigDecimal total = expenseService.getDailyExpensesTotal(branchId, date);

        Map<String, Object> response = new HashMap<>();
        response.put("total", total);
        response.put("date", date);
        response.put("branchId", branchId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDto> getExpenseById(@PathVariable Long id) {
        ExpenseDto expense = expenseService.getExpenseById(id);
        if (!branchAccessControl.hasAccessToBranch(expense.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(expense);
    }

    @PostMapping
    public ResponseEntity<ExpenseDto> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ExpenseDto expense = expenseService.createExpense(request);
        return ResponseEntity.ok(expense);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDto> updateExpense(@PathVariable Long id,
                                                    @Valid @RequestBody CreateExpenseRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        ExpenseDto expense = expenseService.updateExpense(id, request);
        return ResponseEntity.ok(expense);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        ExpenseDto expense = expenseService.getExpenseById(id);
        if (!branchAccessControl.hasAccessToBranch(expense.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        expenseService.deleteExpense(id);
        return ResponseEntity.ok().build();
    }
}
