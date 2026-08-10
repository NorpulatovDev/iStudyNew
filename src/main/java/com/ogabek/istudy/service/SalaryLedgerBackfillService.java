package com.ogabek.istudy.service;

import com.ogabek.istudy.entity.Teacher;
import com.ogabek.istudy.repository.TeacherRepository;
import com.ogabek.istudy.repository.TeacherSalaryPaymentRepository;
import com.ogabek.istudy.repository.TeacherSalaryPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Mavjud (production) ma'lumotlarni yangi model bilan moslashtiradi.
 *
 * <p>Ikki bosqich, ikkalasi ham <b>idempotent</b> — har safar ishga tushganda
 * qayta bajarilishi xavfsiz:
 * <ol>
 *   <li>Eski daftar satrlariga {@code type = PAYOUT} va {@code signed_amount = amount}
 *       yoziladi.</li>
 *   <li>Tugagan oylar uchun daftar yozuvi bo'lgan har bir (o'qituvchi, yil, oy)
 *       uchligi {@code CLOSED} davr sifatida muzlatiladi.</li>
 * </ol>
 *
 * <p><b>Cheklov:</b> migratsiya paytida muzlatilgan eski oylar o'sha lahzadagi
 * ma'lumotlar bo'yicha hisoblanadi — bazada haqiqiy to'lov paytidagi holat
 * saqlanmagan. Shuning uchun bunday davrlar {@code backfilled = true} bilan
 * belgilanadi. Deploydan keyingi barcha oylar aniq bo'ladi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryLedgerBackfillService {

    private final TeacherSalaryPaymentRepository salaryPaymentRepository;
    private final TeacherSalaryPeriodRepository periodRepository;
    private final TeacherRepository teacherRepository;
    private final SalaryPeriodService periodService;

    /** 1-bosqich: turi yo'q eski satrlarni PAYOUT deb belgilash. */
    @Transactional
    public int backfillLedgerTypes() {
        long untyped = salaryPaymentRepository.countUntypedRows();
        if (untyped == 0) {
            return 0;
        }

        int updated = salaryPaymentRepository.backfillLegacyRowsAsPayout();
        log.info("Oylik daftari migratsiyasi: {} ta eski satr PAYOUT deb belgilandi", updated);
        return updated;
    }

    /** 2-bosqich: to'langan va tugagan oylarni muzlatish. */
    public int freezeHistoricalPeriods() {
        List<Object[]> triples = salaryPaymentRepository.findAllDistinctTeacherYearMonth();
        int frozen = 0;

        for (Object[] triple : triples) {
            Long teacherId = ((Number) triple[0]).longValue();
            int year = ((Number) triple[1]).intValue();
            int month = ((Number) triple[2]).intValue();

            // Joriy va kelajak oylar ochiq qoladi — ular hali "ishlab turibdi".
            if (!periodService.isElapsed(year, month)) {
                continue;
            }
            if (periodRepository.existsByTeacherIdAndYearAndMonth(teacherId, year, month)) {
                continue;
            }

            Optional<Teacher> teacher = teacherRepository.findByIdWithBranch(teacherId);
            if (teacher.isEmpty()) {
                log.warn("Migratsiya: o'qituvchi topilmadi yoki o'chirilgan, davr o'tkazib yuborildi (teacherId={}, {}/{})",
                        teacherId, month, year);
                continue;
            }

            try {
                if (periodService.closeAsBackfill(teacher.get(), year, month).isPresent()) {
                    frozen++;
                }
            } catch (Exception e) {
                log.error("Migratsiya: davrni muzlatib bo'lmadi (teacherId={}, {}/{}): {}",
                        teacherId, month, year, e.getMessage());
            }
        }

        if (frozen > 0) {
            log.info("Oylik daftari migratsiyasi: {} ta o'tgan oy muzlatildi", frozen);
        }
        return frozen;
    }
}
