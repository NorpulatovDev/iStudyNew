package com.ogabek.istudy.service;

import com.ogabek.istudy.repository.ExpenseRepository;
import com.ogabek.istudy.repository.PaymentRepository;
import com.ogabek.istudy.repository.TeacherSalaryPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final TeacherSalaryPaymentRepository salaryPaymentRepository;

    public Map<String, Object> getDailyExpenseReport(Long branchId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        BigDecimal regularExpenses = expenseRepository.sumDailyExpenses(branchId, startOfDay);
        BigDecimal salaryExpenses = salaryPaymentRepository.sumSalaryPaymentsByDateRange(branchId, startOfDay, endOfDay);
        BigDecimal totalExpenses = (regularExpenses != null ? regularExpenses : BigDecimal.ZERO)
                .add(salaryExpenses != null ? salaryExpenses : BigDecimal.ZERO);

        Map<String, Object> report = new HashMap<>();
        report.put("date", date);
        report.put("branchId", branchId);
        report.put("regularExpenses", regularExpenses != null ? regularExpenses : BigDecimal.ZERO);
        report.put("salaryExpenses", salaryExpenses != null ? salaryExpenses : BigDecimal.ZERO);
        report.put("totalExpenses", totalExpenses);
        report.put("type", "DAILY_EXPENSE");

        return report;
    }

    public Map<String, Object> getMonthlyExpenseReport(Long branchId, int year, int month) {
        BigDecimal regularExpenses = expenseRepository.sumMonthlyExpenses(branchId, year, month);
        BigDecimal salaryExpenses = salaryPaymentRepository.sumMonthlySalaryPayments(branchId, year, month);
        BigDecimal totalExpenses = (regularExpenses != null ? regularExpenses : BigDecimal.ZERO)
                .add(salaryExpenses != null ? salaryExpenses : BigDecimal.ZERO);

        Map<String, Object> report = new HashMap<>();
        report.put("year", year);
        report.put("month", month);
        report.put("branchId", branchId);
        report.put("regularExpenses", regularExpenses != null ? regularExpenses : BigDecimal.ZERO);
        report.put("salaryExpenses", salaryExpenses != null ? salaryExpenses : BigDecimal.ZERO);
        report.put("totalExpenses", totalExpenses);
        report.put("type", "MONTHLY_EXPENSE");

        return report;
    }

    public Map<String, Object> getExpenseRangeReport(Long branchId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        BigDecimal regularExpenses = expenseRepository.sumExpensesByDateRange(branchId, start, end);
        BigDecimal salaryExpenses = salaryPaymentRepository.sumSalaryPaymentsByDateRange(branchId, start, end);
        BigDecimal totalExpenses = (regularExpenses != null ? regularExpenses : BigDecimal.ZERO)
                .add(salaryExpenses != null ? salaryExpenses : BigDecimal.ZERO);

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("branchId", branchId);
        report.put("regularExpenses", regularExpenses != null ? regularExpenses : BigDecimal.ZERO);
        report.put("salaryExpenses", salaryExpenses != null ? salaryExpenses : BigDecimal.ZERO);
        report.put("totalExpenses", totalExpenses);
        report.put("type", "RANGE_EXPENSE");

        return report;
    }

    public Map<String, Object> getAllTimeExpenseReport(Long branchId) {
        BigDecimal regularExpenses = expenseRepository.sumAllTimeExpenses(branchId);
        BigDecimal salaryExpenses = salaryPaymentRepository.sumAllTimeSalaryPayments(branchId);
        BigDecimal totalExpenses = (regularExpenses != null ? regularExpenses : BigDecimal.ZERO)
                .add(salaryExpenses != null ? salaryExpenses : BigDecimal.ZERO);

        Map<String, Object> report = new HashMap<>();
        report.put("branchId", branchId);
        report.put("regularExpenses", regularExpenses != null ? regularExpenses : BigDecimal.ZERO);
        report.put("salaryExpenses", salaryExpenses != null ? salaryExpenses : BigDecimal.ZERO);
        report.put("totalExpenses", totalExpenses);
        report.put("type", "ALL_TIME_EXPENSE");

        return report;
    }

    public Map<String, Object> getFinancialSummary(Long branchId, int year, int month) {
        BigDecimal totalPayments = paymentRepository.sumMonthlyPayments(branchId, year, month);
        BigDecimal regularExpenses = expenseRepository.sumMonthlyExpenses(branchId, year, month);
        BigDecimal salaryPayments = salaryPaymentRepository.sumMonthlySalaryPayments(branchId, year, month);

        BigDecimal totalIncome = totalPayments != null ? totalPayments : BigDecimal.ZERO;
        BigDecimal totalExpenses = (regularExpenses != null ? regularExpenses : BigDecimal.ZERO)
                .add(salaryPayments != null ? salaryPayments : BigDecimal.ZERO);
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);

        Map<String, Object> summary = new HashMap<>();
        summary.put("year", year);
        summary.put("month", month);
        summary.put("branchId", branchId);
        summary.put("totalIncome", totalIncome);
        summary.put("regularExpenses", regularExpenses != null ? regularExpenses : BigDecimal.ZERO);
        summary.put("salaryPayments", salaryPayments != null ? salaryPayments : BigDecimal.ZERO);
        summary.put("totalExpenses", totalExpenses);
        summary.put("netProfit", netProfit);
        summary.put("type", "FINANCIAL_SUMMARY");

        return summary;
    }

    public Map<String, Object> getFinancialSummaryRange(Long branchId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        BigDecimal totalPayments = paymentRepository.sumPaymentsByDateRange(branchId, start, end);
        BigDecimal regularExpenses = expenseRepository.sumExpensesByDateRange(branchId, start, end);
        BigDecimal salaryPayments = salaryPaymentRepository.sumSalaryPaymentsByDateRange(branchId, start, end);

        BigDecimal totalIncome = totalPayments != null ? totalPayments : BigDecimal.ZERO;
        BigDecimal totalExpenses = (regularExpenses != null ? regularExpenses : BigDecimal.ZERO)
                .add(salaryPayments != null ? salaryPayments : BigDecimal.ZERO);
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);

        Map<String, Object> summary = new HashMap<>();
        summary.put("startDate", startDate);
        summary.put("endDate", endDate);
        summary.put("branchId", branchId);
        summary.put("totalIncome", totalIncome);
        summary.put("regularExpenses", regularExpenses != null ? regularExpenses : BigDecimal.ZERO);
        summary.put("salaryPayments", salaryPayments != null ? salaryPayments : BigDecimal.ZERO);
        summary.put("totalExpenses", totalExpenses);
        summary.put("netProfit", netProfit);
        summary.put("type", "FINANCIAL_SUMMARY_RANGE");

        return summary;
    }

    public Map<String, Object> getDailyPaymentReport(Long branchId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        BigDecimal totalPayments = paymentRepository.sumPaymentsByDateRange(branchId, startOfDay, endOfDay);

        Map<String, Object> report = new HashMap<>();
        report.put("date", date);
        report.put("branchId", branchId);
        report.put("totalPayments", totalPayments != null ? totalPayments : BigDecimal.ZERO);
        report.put("type", "DAILY_PAYMENT");

        return report;
    }

    public Map<String, Object> getMonthlyPaymentReport(Long branchId, int year, int month) {
        BigDecimal totalPayments = paymentRepository.sumMonthlyPayments(branchId, year, month);

        Map<String, Object> report = new HashMap<>();
        report.put("year", year);
        report.put("month", month);
        report.put("branchId", branchId);
        report.put("totalPayments", totalPayments != null ? totalPayments : BigDecimal.ZERO);
        report.put("type", "MONTHLY_PAYMENT");

        return report;
    }

    public Map<String, Object> getPaymentRangeReport(Long branchId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        BigDecimal totalPayments = paymentRepository.sumPaymentsByDateRange(branchId, start, end);

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        report.put("branchId", branchId);
        report.put("totalPayments", totalPayments != null ? totalPayments : BigDecimal.ZERO);
        report.put("type", "RANGE_PAYMENT");

        return report;
    }
}
