package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateSalaryPaymentRequest;
import com.ogabek.istudy.dto.response.GroupSalaryInfo;
import com.ogabek.istudy.dto.response.SalaryCalculationDto;
import com.ogabek.istudy.dto.response.TeacherSalaryHistoryDto;
import com.ogabek.istudy.dto.response.TeacherSalaryPaymentDto;
import com.ogabek.istudy.dto.response.TeacherSalaryPeriodDto;
import com.ogabek.istudy.entity.*;
import com.ogabek.istudy.repository.*;
import com.ogabek.istudy.security.BranchAccessControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * O'qituvchi oyligi.
 *
 * <p><b>Asosiy qoida:</b> davr yopilgach ({@link SalaryPeriodStatus#CLOSED}) hisoblangan
 * summa muzlatilgan nusxadan o'qiladi. Tarif, guruh egasi, guruh a'zolari yoki
 * o'quvchi to'lovlari keyinchalik o'zgarsa ham to'langan oy summasi o'zgarmaydi.
 * Ochiq davr esa avvalgidek jonli hisoblanadi.
 *
 * <p>Yopilgan oyni ataylab o'zgartirish kerak bo'lsa — {@code BONUS}/{@code DEDUCTION}
 * yozuvi qo'shiladi yoki davr {@code reopen} qilinadi (ikkalasi ham auditga tushadi).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherSalaryService {

    private final TeacherSalaryPaymentRepository salaryPaymentRepository;
    private final TeacherRepository teacherRepository;
    private final BranchRepository branchRepository;
    private final TeacherSalaryCalculator calculator;
    private final SalaryPeriodService periodService;
    private final BranchAccessControl branchAccessControl;

    // ==================================================================
    // Hisoblash
    // ==================================================================

    @Transactional
    public SalaryCalculationDto calculateTeacherSalary(Long teacherId, int year, int month) {
        return calculateTeacherSalary(teacherId, year, month, false);
    }

    /**
     * @param includeDrift yopilgan davr uchun "yopilgandan keyin nima o'zgardi"
     *                     ma'lumotini ham hisoblaydi (qo'shimcha so'rovlar talab qiladi)
     */
    @Transactional
    public SalaryCalculationDto calculateTeacherSalary(Long teacherId, int year, int month, boolean includeDrift) {
        Teacher teacher = teacherRepository.findByIdWithBranch(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + teacherId));

        TeacherSalaryPeriod period = resolvePeriod(teacher, year, month);

        if (period != null && period.isClosed()) {
            return fromSnapshot(teacher, period, includeDrift);
        }
        return fromLiveCalculation(teacher, year, month, period);
    }

    /**
     * Davrni topadi va kerak bo'lsa avtomatik muzlatadi.
     *
     * <p>Muzlatish sharti: oy tugagan <b>va</b> o'sha oy uchun kamida bitta to'lov
     * qilingan. Joriy oy hech qachon muzlatilmaydi — avans berilgan bo'lsa ham
     * oylik oy oxirigacha o'sib boraveradi.
     */
    private TeacherSalaryPeriod resolvePeriod(Teacher teacher, int year, int month) {
        Optional<TeacherSalaryPeriod> existing = periodService.find(teacher.getId(), year, month);

        if (existing.isPresent() && existing.get().isClosed()) {
            return existing.get();
        }

        if (periodService.shouldAutoClose(teacher.getId(), year, month)) {
            // Muzlatish alohida tranzaksiyada bo'ladi. Uni qayta o'qib bo'lmaydi:
            // joriy persistence context da davrning eski (OPEN) nusxasi turibdi va
            // so'rov o'sha eskisini qaytaradi. Shuning uchun natijaning o'zini olamiz.
            try {
                Optional<TeacherSalaryPeriod> closed =
                        periodService.closeInNewTransaction(teacher, year, month, true, currentUserOrNull());
                if (closed.isPresent()) {
                    return closed.get();
                }
            } catch (Exception e) {
                // Muzlatib bo'lmadi (masalan parallel so'rov ulgurdi). O'qish so'rovi
                // shu sababli buzilmasligi kerak — pastda davrni qayta o'qiymiz.
                log.warn("Davrni avtomatik muzlatib bo'lmadi (teacher={} {}/{}): {}",
                        teacher.getId(), month, year, e.getMessage());
            }
            return periodService.find(teacher.getId(), year, month).orElse(null);
        }

        return existing.orElse(null);
    }

    /** Yopilgan davr — muzlatilgan nusxadan o'qiladi, qayta hisoblanmaydi. */
    private SalaryCalculationDto fromSnapshot(Teacher teacher, TeacherSalaryPeriod period, boolean includeDrift) {
        int year = period.getYear();
        int month = period.getMonth();

        BigDecimal adjustments = zeroIfNull(
                salaryPaymentRepository.sumEarningsAdjustments(teacher.getId(), year, month));
        BigDecimal totalSalary = zeroIfNull(period.getTotalSalarySnapshot()).add(adjustments);
        BigDecimal alreadyPaid = zeroIfNull(
                salaryPaymentRepository.sumByTeacherAndYearAndMonth(teacher.getId(), year, month));

        List<GroupSalaryInfo> groups = period.getLines().stream()
                .map(line -> new GroupSalaryInfo(
                        line.getGroupId(),
                        line.getGroupName(),
                        line.getPaidStudentCount(),
                        zeroIfNull(line.getGroupPayments()),
                        line.getTotalStudentsInGroup(),
                        zeroIfNull(line.getGroupPrice())))
                .collect(Collectors.toList());

        SalaryCalculationDto dto = baseDto(teacher, year, month);
        dto.setBaseSalary(zeroIfNull(period.getBaseSalarySnapshot()));
        dto.setPaymentBasedSalary(zeroIfNull(period.getPaymentBasedSalarySnapshot()));
        dto.setTotalSalary(totalSalary);
        dto.setTotalStudentPayments(zeroIfNull(period.getStudentPaymentsSnapshot()));
        dto.setTotalStudents(period.getPaidStudentCountSnapshot());
        dto.setGroups(groups);
        dto.setStatus(SalaryPeriodStatus.CLOSED.name());
        dto.setFrozen(true);
        dto.setClosedAt(period.getClosedAt());
        dto.setClosedBy(period.getClosedByUsername());
        dto.setReopenCount(period.getReopenCount());
        applyBalance(dto, totalSalary, alreadyPaid, adjustments);

        if (includeDrift) {
            BigDecimal liveTotal = calculator.compute(teacher, year, month).getTotalSalary();
            BigDecimal drift = liveTotal.subtract(zeroIfNull(period.getTotalSalarySnapshot()));
            dto.setDriftAmount(drift);
            dto.setDriftDetected(drift.compareTo(BigDecimal.ZERO) != 0);
        }

        return dto;
    }

    /** Ochiq davr — avvalgidek joriy ma'lumotlardan hisoblanadi. */
    private SalaryCalculationDto fromLiveCalculation(Teacher teacher, int year, int month,
                                                     TeacherSalaryPeriod openPeriod) {
        TeacherSalaryCalculator.LiveSalary live = calculator.compute(teacher, year, month);

        BigDecimal adjustments = zeroIfNull(
                salaryPaymentRepository.sumEarningsAdjustments(teacher.getId(), year, month));
        BigDecimal totalSalary = live.getTotalSalary().add(adjustments);
        BigDecimal alreadyPaid = zeroIfNull(
                salaryPaymentRepository.sumByTeacherAndYearAndMonth(teacher.getId(), year, month));

        SalaryCalculationDto dto = baseDto(teacher, year, month);
        dto.setBaseSalary(live.getBaseSalary());
        dto.setPaymentBasedSalary(live.getPaymentBasedSalary());
        dto.setTotalSalary(totalSalary);
        dto.setTotalStudentPayments(live.getTotalStudentPayments());
        dto.setTotalStudents(live.getTotalPaidStudents());
        dto.setGroups(live.getGroups());
        dto.setStatus(SalaryPeriodStatus.OPEN.name());
        dto.setFrozen(false);
        dto.setReopenCount(openPeriod != null ? openPeriod.getReopenCount() : 0);
        applyBalance(dto, totalSalary, alreadyPaid, adjustments);

        return dto;
    }

    private SalaryCalculationDto baseDto(Teacher teacher, int year, int month) {
        SalaryCalculationDto dto = new SalaryCalculationDto();
        dto.setTeacherId(teacher.getId());
        dto.setTeacherName(teacher.getFirstName() + " " + teacher.getLastName());
        dto.setYear(year);
        dto.setMonth(month);
        dto.setBranchId(teacher.getBranch().getId());
        dto.setBranchName(teacher.getBranch().getName());
        return dto;
    }

    private void applyBalance(SalaryCalculationDto dto, BigDecimal totalSalary,
                              BigDecimal alreadyPaid, BigDecimal adjustments) {
        BigDecimal balance = totalSalary.subtract(alreadyPaid);

        dto.setAlreadyPaid(alreadyPaid);
        dto.setEarningsAdjustments(adjustments);
        dto.setBalance(balance);
        dto.setOverpaid(balance.compareTo(BigDecimal.ZERO) < 0);
        // Eski maydon — hech qachon manfiy bo'lmaydi (frontend shunga tayangan).
        dto.setRemainingAmount(balance.compareTo(BigDecimal.ZERO) > 0 ? balance : BigDecimal.ZERO);
    }

    @Transactional
    public List<SalaryCalculationDto> calculateSalariesForBranch(Long branchId, int year, int month) {
        List<Teacher> teachers = teacherRepository.findByBranchIdWithBranch(branchId);

        return teachers.stream()
                .map(teacher -> calculateTeacherSalary(teacher.getId(), year, month))
                .collect(Collectors.toList());
    }

    // ==================================================================
    // Daftar yozuvlari
    // ==================================================================

    /** Oylik to'lash. {@code request.type} yuborilmasa — {@code PAYOUT}. */
    @Transactional
    public TeacherSalaryPaymentDto createSalaryPayment(CreateSalaryPaymentRequest request) {
        SalaryTransactionType type = resolveType(request.getType(), SalaryTransactionType.PAYOUT);
        if (type == SalaryTransactionType.DEDUCTION) {
            throw new RuntimeException("Ushlab qolish uchun /subtract dan foydalaning!");
        }
        return recordTransaction(request, type, null);
    }

    /**
     * Oylikdan ushlab qolish (jarima).
     *
     * <p><b>Diqqat — o'zgargan xatti-harakat:</b> ilgari bu metod {@code createSalaryPayment}
     * bilan bir xil ishlagan va "to'langan" summani <i>oshirgan</i>. Endi u haqiqiy
     * {@code DEDUCTION} yozuvi qo'shadi: ishlab topilgan oylikni kamaytiradi va
     * hisobotlarda kassa xarajati sifatida ko'rinmaydi.
     */
    @Transactional
    public TeacherSalaryPaymentDto subtractFromSalary(CreateSalaryPaymentRequest request) {
        SalaryTransactionType type = resolveType(request.getType(), SalaryTransactionType.DEDUCTION);
        if (type.isSettlement()) {
            throw new RuntimeException("Bu endpoint faqat DEDUCTION yoki BONUS uchun!");
        }
        return recordTransaction(request, type, "Manual subtraction");
    }

    private TeacherSalaryPaymentDto recordTransaction(CreateSalaryPaymentRequest request,
                                                      SalaryTransactionType type,
                                                      String defaultDescription) {
        Teacher teacher = teacherRepository.findByIdWithBranch(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + request.getTeacherId()));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + request.getBranchId()));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(type.isSettlement()
                    ? "To'lov miqdori 0 dan katta bo'lishi kerak!"
                    : "Miqdor 0 dan katta bo'lishi kerak!");
        }

        User actor = currentUserOrNull();

        TeacherSalaryPayment payment = new TeacherSalaryPayment();
        payment.setTeacher(teacher);
        payment.setBranch(branch);
        payment.setYear(request.getYear());
        payment.setMonth(request.getMonth());
        payment.setAmount(request.getAmount());
        payment.setDescription(request.getDescription() != null ? request.getDescription() : defaultDescription);
        payment.setType(type);
        payment.setSignedAmount(type.applySign(request.getAmount()));
        payment.setReason(request.getReason() != null ? request.getReason() : request.getDescription());
        if (actor != null) {
            payment.setCreatedByUserId(actor.getId());
            payment.setCreatedByUsername(actor.getUsername());
        }

        TeacherSalaryPayment saved = salaryPaymentRepository.save(payment);

        // To'lov yozilgandan keyin — oy tugagan bo'lsa davrni shu yerda muzlatamiz,
        // ya'ni to'langan oy summasi keyinchalik hech qachon qayta hisoblanmaydi.
        if (periodService.shouldAutoClose(teacher.getId(), request.getYear(), request.getMonth())) {
            periodService.closeInCurrentTransaction(teacher, request.getYear(), request.getMonth(), true, actor);
        }

        return convertPaymentToDto(saved);
    }

    /**
     * To'lovni bekor qiladi — o'chirmaydi, teskari yozuv qo'shadi.
     * Daftar hech qachon ma'lumot yo'qotmaydi.
     */
    @Transactional
    public TeacherSalaryPaymentDto reverseSalaryPayment(Long paymentId, String reason) {
        TeacherSalaryPayment original = salaryPaymentRepository.findByIdWithDetails(paymentId)
                .orElseThrow(() -> new RuntimeException("Salary payment not found with id: " + paymentId));

        return reverse(original, reason, currentUserOrNull());
    }

    private TeacherSalaryPaymentDto reverse(TeacherSalaryPayment original, String reason, User actor) {
        if (original.isReversed()) {
            throw new RuntimeException("Bu yozuv allaqachon bekor qilingan!");
        }
        if (original.resolvedType() == SalaryTransactionType.REVERSAL) {
            throw new RuntimeException("Teskari yozuvni bekor qilib bo'lmaydi!");
        }

        SalaryTransactionType reversalType = oppositeOf(original.resolvedType());

        TeacherSalaryPayment reversal = new TeacherSalaryPayment();
        reversal.setTeacher(original.getTeacher());
        reversal.setBranch(original.getBranch());
        reversal.setYear(original.getYear());
        reversal.setMonth(original.getMonth());
        reversal.setAmount(original.getAmount());
        reversal.setDescription("Bekor qilindi: #" + original.getId());
        reversal.setType(reversalType);
        reversal.setSignedAmount(reversalType.applySign(original.getAmount()));
        reversal.setReason(reason);
        reversal.setReversesPaymentId(original.getId());
        if (actor != null) {
            reversal.setCreatedByUserId(actor.getId());
            reversal.setCreatedByUsername(actor.getUsername());
        }

        TeacherSalaryPayment saved = salaryPaymentRepository.save(reversal);

        original.setReversedAt(LocalDateTime.now());
        salaryPaymentRepository.save(original);

        log.info("Oylik yozuvi bekor qilindi: id={} tur={} summa={} sabab='{}'",
                original.getId(), original.resolvedType(), original.getAmount(), reason);

        return convertPaymentToDto(saved);
    }

    private SalaryTransactionType oppositeOf(SalaryTransactionType type) {
        switch (type) {
            case PAYOUT:
                return SalaryTransactionType.REVERSAL;
            case BONUS:
                return SalaryTransactionType.DEDUCTION;
            case DEDUCTION:
                return SalaryTransactionType.BONUS;
            default:
                throw new RuntimeException("Bu turdagi yozuvni bekor qilib bo'lmaydi: " + type);
        }
    }

    /**
     * Eski {@code DELETE /payments/{id}} endpointi.
     *
     * <p>Backward compatible: davr hali ochiq bo'lsa — avvalgidek o'chiradi.
     * Davr yopilgan bo'lsa — o'chirmaydi, teskari yozuv qo'shadi, chunki yopilgan
     * oyning to'lov tarixi yo'qolmasligi kerak.
     */
    @Transactional
    public void deleteSalaryPayment(Long paymentId) {
        TeacherSalaryPayment payment = salaryPaymentRepository.findByIdWithDetails(paymentId)
                .orElseThrow(() -> new RuntimeException("Salary payment not found with id: " + paymentId));

        if (payment.isReversed()) {
            throw new RuntimeException("Bu yozuv allaqachon bekor qilingan!");
        }

        boolean periodClosed = periodService.find(payment.getTeacher().getId(), payment.getYear(), payment.getMonth())
                .map(TeacherSalaryPeriod::isClosed)
                .orElse(false);

        if (periodClosed) {
            reverse(payment, "DELETE so'rovi orqali bekor qilindi", currentUserOrNull());
            return;
        }

        salaryPaymentRepository.deleteById(paymentId);
    }

    // ==================================================================
    // Davr boshqaruvi
    // ==================================================================

    /** Oyni qo'lda yopish — summani shu paytdan boshlab muzlatadi. */
    @Transactional
    public TeacherSalaryPeriodDto closePeriod(Long teacherId, int year, int month) {
        Teacher teacher = teacherRepository.findByIdWithBranch(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + teacherId));

        TeacherSalaryPeriod period = periodService.closeInCurrentTransaction(
                teacher, year, month, false, currentUserOrNull());

        return convertPeriodToDto(period);
    }

    /** Yopilgan oyni qayta ochish — auditga yoziladi. */
    @Transactional
    public TeacherSalaryPeriodDto reopenPeriod(Long teacherId, int year, int month, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("Qayta ochish sababi majburiy!");
        }

        Teacher teacher = teacherRepository.findByIdWithBranch(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + teacherId));

        TeacherSalaryPeriod period = periodService.reopen(teacher, year, month, reason, currentUserOrNull());
        return convertPeriodToDto(period);
    }

    @Transactional(readOnly = true)
    public List<TeacherSalaryPeriodDto> getPeriodsByTeacher(Long teacherId) {
        return periodService.findByTeacher(teacherId).stream()
                .map(this::convertPeriodToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeacherSalaryPeriodDto getPeriod(Long teacherId, int year, int month) {
        TeacherSalaryPeriod period = periodService.find(teacherId, year, month)
                .orElseThrow(() -> new RuntimeException(
                        "Bu oy uchun davr topilmadi: " + month + "/" + year));
        return convertPeriodToDto(period);
    }

    // ==================================================================
    // O'qish
    // ==================================================================

    @Transactional(readOnly = true)
    public List<TeacherSalaryPaymentDto> getSalaryPaymentsByBranch(Long branchId) {
        return salaryPaymentRepository.findByBranchIdWithDetails(branchId).stream()
                .map(this::convertPaymentToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherSalaryPaymentDto> getSalaryPaymentsByTeacher(Long teacherId) {
        return salaryPaymentRepository.findByTeacherIdWithDetails(teacherId).stream()
                .map(this::convertPaymentToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeacherSalaryPaymentDto> getPaymentsForTeacherAndMonth(Long teacherId, int year, int month) {
        return salaryPaymentRepository.findByTeacherAndYearAndMonthWithDetails(teacherId, year, month).stream()
                .map(this::convertPaymentToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<TeacherSalaryHistoryDto> getTeacherSalaryHistory(Long teacherId) {
        Teacher teacher = teacherRepository.findByIdWithBranch(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with id: " + teacherId));

        List<TeacherSalaryHistoryDto> history = new ArrayList<>();

        for (int[] yearMonth : distinctPeriodsFor(teacherId)) {
            int year = yearMonth[0];
            int month = yearMonth[1];

            SalaryCalculationDto calculation = calculateTeacherSalary(teacherId, year, month);
            LocalDateTime lastPaymentDate = salaryPaymentRepository.getLastPaymentDate(teacherId, year, month);
            int paymentCount = salaryPaymentRepository.countPaymentsByTeacherAndYearAndMonth(teacherId, year, month);

            TeacherSalaryHistoryDto item = new TeacherSalaryHistoryDto();
            item.setTeacherId(teacherId);
            item.setTeacherName(teacher.getFirstName() + " " + teacher.getLastName());
            item.setYear(year);
            item.setMonth(month);
            item.setTotalSalary(calculation.getTotalSalary());
            item.setTotalPaid(calculation.getAlreadyPaid());
            item.setRemainingAmount(calculation.getRemainingAmount());
            item.setFullyPaid(calculation.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0);
            item.setLastPaymentDate(lastPaymentDate);
            item.setPaymentCount(paymentCount);
            item.setStatus(calculation.getStatus());
            item.setFrozen(calculation.isFrozen());
            item.setClosedAt(calculation.getClosedAt());
            item.setEarningsAdjustments(calculation.getEarningsAdjustments());
            item.setBalance(calculation.getBalance());
            item.setOverpaid(calculation.isOverpaid());

            history.add(item);
        }

        history.sort((a, b) -> {
            int yearCompare = Integer.compare(b.getYear(), a.getYear());
            if (yearCompare != 0)
                return yearCompare;
            return Integer.compare(b.getMonth(), a.getMonth());
        });

        return history;
    }

    /** Daftarda yoki davrlar jadvalida uchraydigan barcha (yil, oy) juftliklari. */
    private List<int[]> distinctPeriodsFor(Long teacherId) {
        Set<String> seen = new LinkedHashSet<>();
        List<int[]> result = new ArrayList<>();

        List<Object[]> fromPayments = salaryPaymentRepository.findDistinctYearMonthByTeacherId(teacherId);
        List<Object[]> fromPeriods = periodService.findByTeacher(teacherId).stream()
                .map(p -> new Object[] { p.getYear(), p.getMonth() })
                .collect(Collectors.toList());

        for (List<Object[]> source : List.of(fromPayments, fromPeriods)) {
            for (Object[] pair : source) {
                int year = ((Number) pair[0]).intValue();
                int month = ((Number) pair[1]).intValue();
                if (seen.add(year + "-" + month)) {
                    result.add(new int[] { year, month });
                }
            }
        }
        return result;
    }

    @Transactional
    public BigDecimal getRemainingAmountForTeacher(Long teacherId, int year, int month) {
        return calculateTeacherSalary(teacherId, year, month).getRemainingAmount();
    }

    // ==================================================================
    // Yordamchilar
    // ==================================================================

    private SalaryTransactionType resolveType(String raw, SalaryTransactionType fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return SalaryTransactionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Noma'lum yozuv turi: " + raw);
        }
    }

    /** Auth konteksti bo'lmasa (masalan migratsiya) {@code null} qaytaradi. */
    private User currentUserOrNull() {
        try {
            return branchAccessControl.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private TeacherSalaryPaymentDto convertPaymentToDto(TeacherSalaryPayment payment) {
        SalaryTransactionType type = payment.resolvedType();

        TeacherSalaryPaymentDto dto = new TeacherSalaryPaymentDto();
        dto.setId(payment.getId());
        dto.setTeacherId(payment.getTeacher().getId());
        dto.setTeacherName(payment.getTeacher().getFirstName() + " " + payment.getTeacher().getLastName());
        dto.setYear(payment.getYear());
        dto.setMonth(payment.getMonth());
        dto.setAmount(payment.getAmount());
        dto.setDescription(payment.getDescription());
        dto.setBranchId(payment.getBranch().getId());
        dto.setBranchName(payment.getBranch().getName());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setType(type.name());
        dto.setSignedAmount(payment.resolvedSignedAmount());
        dto.setSettlement(type.isSettlement());
        dto.setReason(payment.getReason());
        dto.setReversesPaymentId(payment.getReversesPaymentId());
        dto.setReversedAt(payment.getReversedAt());
        dto.setReversed(payment.isReversed());
        dto.setCreatedByUsername(payment.getCreatedByUsername());
        return dto;
    }

    private TeacherSalaryPeriodDto convertPeriodToDto(TeacherSalaryPeriod period) {
        Long teacherId = period.getTeacher().getId();
        int year = period.getYear();
        int month = period.getMonth();

        BigDecimal adjustments = zeroIfNull(salaryPaymentRepository.sumEarningsAdjustments(teacherId, year, month));
        BigDecimal alreadyPaid = zeroIfNull(salaryPaymentRepository.sumByTeacherAndYearAndMonth(teacherId, year, month));
        BigDecimal totalSalary = zeroIfNull(period.getTotalSalarySnapshot()).add(adjustments);

        TeacherSalaryPeriodDto dto = new TeacherSalaryPeriodDto();
        dto.setId(period.getId());
        dto.setTeacherId(teacherId);
        dto.setTeacherName(period.getTeacher().getFirstName() + " " + period.getTeacher().getLastName());
        dto.setBranchId(period.getBranch().getId());
        dto.setBranchName(period.getBranch().getName());
        dto.setYear(year);
        dto.setMonth(month);
        dto.setStatus(period.getStatus().name());
        dto.setFrozen(period.isClosed());
        dto.setSalaryType(period.getSalaryTypeSnapshot() != null ? period.getSalaryTypeSnapshot().name() : null);
        dto.setBaseSalary(zeroIfNull(period.getBaseSalarySnapshot()));
        dto.setPaymentPercentage(zeroIfNull(period.getPaymentPercentageSnapshot()));
        dto.setPaymentBasedSalary(zeroIfNull(period.getPaymentBasedSalarySnapshot()));
        dto.setTotalStudentPayments(zeroIfNull(period.getStudentPaymentsSnapshot()));
        dto.setTotalSalary(totalSalary);
        dto.setPaidStudentCount(period.getPaidStudentCountSnapshot());
        dto.setGroups(period.getLines().stream()
                .map(line -> new GroupSalaryInfo(
                        line.getGroupId(),
                        line.getGroupName(),
                        line.getPaidStudentCount(),
                        zeroIfNull(line.getGroupPayments()),
                        line.getTotalStudentsInGroup(),
                        zeroIfNull(line.getGroupPrice())))
                .collect(Collectors.toList()));
        dto.setAlreadyPaid(alreadyPaid);
        dto.setEarningsAdjustments(adjustments);
        dto.setBalance(totalSalary.subtract(alreadyPaid));
        dto.setClosedAt(period.getClosedAt());
        dto.setClosedBy(period.getClosedByUsername());
        dto.setAutoClosed(period.getAutoClosed());
        dto.setBackfilled(period.getBackfilled());
        dto.setReopenCount(period.getReopenCount());
        dto.setLastReopenedAt(period.getLastReopenedAt());
        dto.setLastReopenedBy(period.getLastReopenedByUsername());
        dto.setLastReopenReason(period.getLastReopenReason());
        return dto;
    }
}
