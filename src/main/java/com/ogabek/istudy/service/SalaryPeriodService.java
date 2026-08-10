package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.response.GroupSalaryInfo;
import com.ogabek.istudy.entity.*;
import com.ogabek.istudy.repository.BranchRepository;
import com.ogabek.istudy.repository.TeacherRepository;
import com.ogabek.istudy.repository.TeacherSalaryPaymentRepository;
import com.ogabek.istudy.repository.TeacherSalaryPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Oylik davrining hayot sikli: muzlatish (close), qayta ochish (reopen).
 *
 * <p><b>Qoida:</b> davr quyidagi ikkala shart bajarilganda avtomatik yopiladi:
 * <ol>
 *   <li>oy tugagan (joriy oydan oldin), va</li>
 *   <li>o'sha oy uchun kamida bitta kassa yozuvi (PAYOUT) bor.</li>
 * </ol>
 *
 * <p>Ya'ni joriy oy davomida hech narsa o'zgarmaydi — avans berilsa ham oylik
 * o'quvchilar to'lovi tushgani sayin o'sib boraveradi (eski xatti-harakat).
 * Oy tugagach esa summa muzlatiladi va tarif, guruh yoki to'lov o'zgarishi
 * o'tgan oyga ta'sir qilmaydi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryPeriodService {

    private final TeacherSalaryPeriodRepository periodRepository;
    private final TeacherSalaryPaymentRepository salaryPaymentRepository;
    private final TeacherRepository teacherRepository;
    private final BranchRepository branchRepository;
    private final TeacherSalaryCalculator calculator;

    // ------------------------------------------------------------------
    // O'qish
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<TeacherSalaryPeriod> find(Long teacherId, int year, int month) {
        return periodRepository.findWithLines(teacherId, year, month);
    }

    @Transactional(readOnly = true)
    public List<TeacherSalaryPeriod> findByTeacher(Long teacherId) {
        return periodRepository.findByTeacherIdOrderByPeriodDesc(teacherId);
    }

    /** Oy tugaganmi (joriy oy va kelajak oylar uchun {@code false}). */
    public boolean isElapsed(int year, int month) {
        return YearMonth.of(year, month).isBefore(YearMonth.now());
    }

    public boolean hasSettlements(Long teacherId, int year, int month) {
        return salaryPaymentRepository.countSettlements(teacherId, year, month) > 0;
    }

    /** Avtomatik muzlatish sharti bajarilganmi. */
    public boolean shouldAutoClose(Long teacherId, int year, int month) {
        return isElapsed(year, month) && hasSettlements(teacherId, year, month);
    }

    // ------------------------------------------------------------------
    // Muzlatish
    // ------------------------------------------------------------------

    /**
     * Yozuv tranzaksiyasi ichidan chaqiriladi (to'lov qo'shish, qo'lda yopish).
     * Chaqiruvchi tranzaksiyaga qo'shiladi — to'lov va muzlatish atomik bo'ladi.
     */
    @Transactional
    public TeacherSalaryPeriod closeInCurrentTransaction(Teacher teacher, int year, int month,
                                                         boolean autoClosed, User actor) {
        return doClose(teacher, year, month, autoClosed, false, actor);
    }

    /**
     * O'qish (readOnly) tranzaksiyasi ichidan chaqiriladi — o'z tranzaksiyasini
     * ochadi. Poyga holatida (bir vaqtda ikki so'rov) unique constraint ishlaydi,
     * ichki tranzaksiya rollback bo'ladi va tashqi so'rov buzilmaydi.
     *
     * @return muzlatilgan davr, yoki boshqa so'rov ulgurgan bo'lsa — o'sha davr;
     *         muzlatib bo'lmasa {@code Optional.empty()}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<TeacherSalaryPeriod> closeInNewTransaction(Teacher teacher, int year, int month,
                                                               boolean autoClosed, User actor) {
        try {
            return Optional.of(doClose(teacher, year, month, autoClosed, false, actor));
        } catch (DataIntegrityViolationException e) {
            log.debug("Davr allaqachon yopilgan (parallel so'rov): teacher={} {}/{}", teacher.getId(), month, year);
            return Optional.empty();
        }
    }

    /** Migratsiya uchun — {@code backfilled = true} bilan yopadi. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<TeacherSalaryPeriod> closeAsBackfill(Teacher teacher, int year, int month) {
        try {
            return Optional.of(doClose(teacher, year, month, true, true, null));
        } catch (DataIntegrityViolationException e) {
            return Optional.empty();
        }
    }

    private TeacherSalaryPeriod doClose(Teacher teacher, int year, int month,
                                        boolean autoClosed, boolean backfilled, User actor) {
        TeacherSalaryPeriod period = periodRepository
                .findByTeacherIdAndYearAndMonth(teacher.getId(), year, month)
                .orElseGet(() -> {
                    TeacherSalaryPeriod fresh = new TeacherSalaryPeriod();
                    // Joriy (ehtimol yangi) sessiyaga bog'langan proxy — chaqiruvchi
                    // boshqa tranzaksiyadan kelgan bo'lishi mumkin.
                    fresh.setTeacher(teacherRepository.getReferenceById(teacher.getId()));
                    fresh.setBranch(branchRepository.getReferenceById(teacher.getBranch().getId()));
                    fresh.setYear(year);
                    fresh.setMonth(month);
                    return fresh;
                });

        if (period.isClosed()) {
            return period;
        }

        writeSnapshot(period, teacher, year, month);

        period.setStatus(SalaryPeriodStatus.CLOSED);
        period.setClosedAt(LocalDateTime.now());
        period.setAutoClosed(autoClosed);
        period.setBackfilled(backfilled);
        if (actor != null) {
            period.setClosedByUserId(actor.getId());
            period.setClosedByUsername(actor.getUsername());
        }

        TeacherSalaryPeriod saved = periodRepository.save(period);
        log.info("Oylik davri muzlatildi: teacher={} {}/{} summa={} (auto={}, backfill={})",
                teacher.getId(), month, year, saved.getTotalSalarySnapshot(), autoClosed, backfilled);
        return saved;
    }

    private void writeSnapshot(TeacherSalaryPeriod period, Teacher teacher, int year, int month) {
        TeacherSalaryCalculator.LiveSalary live = calculator.compute(teacher, year, month);

        period.setSalaryTypeSnapshot(teacher.getSalaryType());
        period.setBaseSalarySnapshot(live.getBaseSalary());
        period.setPaymentPercentageSnapshot(
                teacher.getPaymentPercentage() != null ? teacher.getPaymentPercentage() : BigDecimal.ZERO);
        period.setPaymentBasedSalarySnapshot(live.getPaymentBasedSalary());
        period.setStudentPaymentsSnapshot(live.getTotalStudentPayments());
        period.setTotalSalarySnapshot(live.getTotalSalary());
        period.setPaidStudentCountSnapshot(live.getTotalPaidStudents());

        List<TeacherSalaryPeriodLine> lines = new ArrayList<>();
        for (GroupSalaryInfo info : live.getGroups()) {
            TeacherSalaryPeriodLine line = new TeacherSalaryPeriodLine();
            line.setGroupId(info.getGroupId());
            line.setGroupName(info.getGroupName());
            line.setGroupPrice(info.getGroupPrice());
            line.setPaidStudentCount(info.getStudentCount());
            line.setTotalStudentsInGroup(info.getTotalStudentsInGroup());
            line.setGroupPayments(info.getTotalGroupPayments());
            lines.add(line);
        }
        period.replaceLines(lines);
    }

    // ------------------------------------------------------------------
    // Qayta ochish
    // ------------------------------------------------------------------

    /**
     * Yopilgan davrni qayta ochadi — faqat ataylab qilingan admin amali sifatida.
     * Keyingi o'qishda summa jonli qayta hisoblanadi va yana muzlatiladi.
     */
    @Transactional
    public TeacherSalaryPeriod reopen(Teacher teacher, int year, int month, String reason, User actor) {
        TeacherSalaryPeriod period = periodRepository
                .findByTeacherIdAndYearAndMonth(teacher.getId(), year, month)
                .orElseThrow(() -> new RuntimeException(
                        "Bu oy uchun yopilgan davr topilmadi: " + month + "/" + year));

        if (!period.isClosed()) {
            throw new RuntimeException("Bu davr allaqachon ochiq!");
        }

        period.setStatus(SalaryPeriodStatus.OPEN);
        period.setReopenCount(period.getReopenCount() + 1);
        period.setLastReopenedAt(LocalDateTime.now());
        period.setLastReopenReason(reason);
        if (actor != null) {
            period.setLastReopenedByUsername(actor.getUsername());
        }

        TeacherSalaryPeriod saved = periodRepository.save(period);
        log.warn("Oylik davri qayta ochildi: teacher={} {}/{} sabab='{}' kim='{}'",
                teacher.getId(), month, year, reason, actor != null ? actor.getUsername() : "-");
        return saved;
    }
}
