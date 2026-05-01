package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateSaleRequest;
import com.ogabek.istudy.dto.response.SaleDto;
import com.ogabek.istudy.entity.Branch;
import com.ogabek.istudy.entity.Sale;
import com.ogabek.istudy.repository.BranchRepository;
import com.ogabek.istudy.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final BranchRepository branchRepository;

    public List<SaleDto> getSalesByBranch(Long branchId) {
        return saleRepository.findByBranchIdOrderByCreatedAtDesc(branchId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<SaleDto> getSalesByMonth(Long branchId, int year, int month) {
        return saleRepository.findByBranchIdAndSaleYearAndSaleMonthOrderByCreatedAtDesc(branchId, year, month).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<SaleDto> getSalesByDate(Long branchId, LocalDate date) {
        return saleRepository.findByBranchIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        branchId, date.atStartOfDay(), date.atTime(LocalTime.MAX)).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public BigDecimal getMonthlyTotal(Long branchId, int year, int month) {
        return saleRepository.sumMonthly(branchId, year, month);
    }

    public BigDecimal getDailyTotal(Long branchId, LocalDate date) {
        return saleRepository.sumDaily(branchId, date.atStartOfDay());
    }

    public SaleDto getSaleById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sotuv topilmadi: " + id));
        return convertToDto(sale);
    }

    public SaleDto createSale(CreateSaleRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Filial topilmadi: " + request.getBranchId()));

        LocalDate now = LocalDate.now();
        Sale sale = new Sale();
        sale.setItemName(request.getItemName());
        sale.setQuantity(request.getQuantity());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setTotalAmount(request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setDescription(request.getDescription());
        sale.setBranch(branch);
        sale.setSaleYear(now.getYear());
        sale.setSaleMonth(now.getMonthValue());

        return convertToDto(saleRepository.save(sale));
    }

    public SaleDto updateSale(Long id, CreateSaleRequest request) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sotuv topilmadi: " + id));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Filial topilmadi: " + request.getBranchId()));

        sale.setItemName(request.getItemName());
        sale.setQuantity(request.getQuantity());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setTotalAmount(request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        sale.setPaymentMethod(request.getPaymentMethod());
        sale.setDescription(request.getDescription());
        sale.setBranch(branch);

        return convertToDto(saleRepository.save(sale));
    }

    public void deleteSale(Long id) {
        if (!saleRepository.existsById(id)) {
            throw new RuntimeException("Sotuv topilmadi: " + id);
        }
        saleRepository.deleteById(id);
    }

    private SaleDto convertToDto(Sale sale) {
        SaleDto dto = new SaleDto();
        dto.setId(sale.getId());
        dto.setItemName(sale.getItemName());
        dto.setQuantity(sale.getQuantity());
        dto.setUnitPrice(sale.getUnitPrice());
        dto.setTotalAmount(sale.getTotalAmount());
        dto.setPaymentMethod(sale.getPaymentMethod().name());
        dto.setDescription(sale.getDescription());
        dto.setBranchId(sale.getBranch().getId());
        dto.setBranchName(sale.getBranch().getName());
        dto.setSaleYear(sale.getSaleYear());
        dto.setSaleMonth(sale.getSaleMonth());
        dto.setCreatedAt(sale.getCreatedAt());
        return dto;
    }
}
