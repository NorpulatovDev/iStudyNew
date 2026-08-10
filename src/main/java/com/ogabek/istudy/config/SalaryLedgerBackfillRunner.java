package com.ogabek.istudy.config;

import com.ogabek.istudy.service.SalaryLedgerBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ishga tushishda oylik daftarini yangi modelga moslashtiradi.
 *
 * <p>Idempotent — hech narsa qilinmasa ham xavfsiz. O'chirish uchun:
 * {@code istudy.salary.backfill.enabled=false}.
 *
 * <p>Migratsiya xatosi ilovaning ishga tushishini to'xtatmaydi: xato log ga
 * yoziladi va ilova ishlashda davom etadi (yopilmagan davrlar avvalgidek
 * jonli hisoblanadi).
 */
@Component
@Order(10)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "istudy.salary.backfill.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class SalaryLedgerBackfillRunner implements CommandLineRunner {

    private final SalaryLedgerBackfillService backfillService;

    @Override
    public void run(String... args) {
        try {
            backfillService.backfillLedgerTypes();
        } catch (Exception e) {
            log.error("Oylik daftari turlarini migratsiya qilib bo'lmadi", e);
        }

        try {
            backfillService.freezeHistoricalPeriods();
        } catch (Exception e) {
            log.error("O'tgan oylarni muzlatib bo'lmadi", e);
        }
    }
}
