package com.ogabek.istudy.controller;

import com.ogabek.istudy.dto.request.CreateSaleRequest;
import com.ogabek.istudy.dto.response.SaleDto;
import com.ogabek.istudy.security.BranchAccessControl;
import com.ogabek.istudy.service.SaleService;
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
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class SaleController {

    private final SaleService saleService;
    private final BranchAccessControl branchAccessControl;

    @GetMapping
    public ResponseEntity<List<SaleDto>> getSalesByBranch(@RequestParam Long branchId) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(saleService.getSalesByBranch(branchId));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<SaleDto>> getSalesByMonth(
            @RequestParam Long branchId,
            @RequestParam int year,
            @RequestParam int month) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(saleService.getSalesByMonth(branchId, year, month));
    }

    @GetMapping("/daily")
    public ResponseEntity<List<SaleDto>> getSalesByDate(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(saleService.getSalesByDate(branchId, date));
    }

    @GetMapping("/monthly/summary")
    public ResponseEntity<Map<String, Object>> getMonthlySummary(
            @RequestParam Long branchId,
            @RequestParam int year,
            @RequestParam int month) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<SaleDto> sales = saleService.getSalesByMonth(branchId, year, month);
        BigDecimal total = saleService.getMonthlyTotal(branchId, year, month);

        Map<String, Object> summary = new HashMap<>();
        summary.put("sales", sales);
        summary.put("total", total);
        summary.put("count", sales.size());
        summary.put("year", year);
        summary.put("month", month);
        summary.put("branchId", branchId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/daily/summary")
    public ResponseEntity<Map<String, Object>> getDailySummary(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (!branchAccessControl.hasAccessToBranch(branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<SaleDto> sales = saleService.getSalesByDate(branchId, date);
        BigDecimal total = saleService.getDailyTotal(branchId, date);

        Map<String, Object> summary = new HashMap<>();
        summary.put("sales", sales);
        summary.put("total", total);
        summary.put("count", sales.size());
        summary.put("date", date);
        summary.put("branchId", branchId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleDto> getSaleById(@PathVariable Long id) {
        SaleDto sale = saleService.getSaleById(id);
        if (!branchAccessControl.hasAccessToBranch(sale.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(sale);
    }

    @PostMapping
    public ResponseEntity<SaleDto> createSale(@Valid @RequestBody CreateSaleRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(saleService.createSale(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SaleDto> updateSale(@PathVariable Long id,
                                               @Valid @RequestBody CreateSaleRequest request) {
        if (!branchAccessControl.hasAccessToBranch(request.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(saleService.updateSale(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        SaleDto sale = saleService.getSaleById(id);
        if (!branchAccessControl.hasAccessToBranch(sale.getBranchId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        saleService.deleteSale(id);
        return ResponseEntity.ok().build();
    }
}
