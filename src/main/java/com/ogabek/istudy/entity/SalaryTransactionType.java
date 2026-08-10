package com.ogabek.istudy.entity;

import java.math.BigDecimal;

/**
 * Oylik daftaridagi yozuv turi.
 *
 * <p>Ikki xil yozuv bor:
 * <ul>
 *   <li><b>SETTLEMENT</b> — kassadan chiqqan pul. Qarzni kamaytiradi va hisobotlarda
 *       xarajat sifatida ko'rinadi ({@link #PAYOUT}, {@link #REVERSAL}).</li>
 *   <li><b>EARNINGS ADJUSTMENT</b> — ishlab topilgan summani o'zgartiradi, lekin pul
 *       harakati emas. Hisobotlarda xarajat sifatida ko'rinmaydi ({@link #BONUS},
 *       {@link #DEDUCTION}).</li>
 * </ul>
 *
 * <p>Eski yozuvlarda bu ustun {@code NULL} bo'ladi va hamma joyda {@link #PAYOUT}
 * sifatida talqin qilinadi.
 */
public enum SalaryTransactionType {

    /** O'qituvchiga berilgan pul. */
    PAYOUT(1, true),

    /** Xato kiritilgan PAYOUT ni bekor qilish (teskari yozuv). */
    REVERSAL(-1, true),

    /** Qo'shimcha mukofot — ishlab topilgan summani oshiradi. */
    BONUS(1, false),

    /** Jarima / ushlab qolish — ishlab topilgan summani kamaytiradi. */
    DEDUCTION(-1, false);

    private final int sign;
    private final boolean settlement;

    SalaryTransactionType(int sign, boolean settlement) {
        this.sign = sign;
        this.settlement = settlement;
    }

    public int getSign() {
        return sign;
    }

    /** {@code true} — kassa harakati (to'langan summaga ta'sir qiladi). */
    public boolean isSettlement() {
        return settlement;
    }

    /** {@code true} — ishlab topilgan (hisoblangan) oylikka ta'sir qiladi. */
    public boolean isEarningsAdjustment() {
        return !settlement;
    }

    public BigDecimal applySign(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return sign < 0 ? amount.negate() : amount;
    }

    /** Eski (NULL turli) yozuvlar uchun standart tur. */
    public static SalaryTransactionType orDefault(SalaryTransactionType type) {
        return type != null ? type : PAYOUT;
    }
}
