package com.ogabek.istudy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_salary_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSalaryPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    /** Har doim musbat — yozuvning "kattaligi". Ishorasi {@link #type} dan kelib chiqadi. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // ------------------------------------------------------------------
    // Daftar (ledger) maydonlari.
    //
    // Hammasi NULL bo'lishi mumkin — bazadagi mavjud satrlarga ddl-auto=update
    // NOT NULL ustun qo'sha olmaydi. Eski satrlar ishga tushishda PAYOUT sifatida
    // to'ldiriladi (SalaryLedgerBackfillRunner), to'ldirilmaguncha esa kod NULL ni
    // PAYOUT deb o'qiydi.
    // ------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 16)
    private SalaryTransactionType type;

    /** {@link #amount} ning ishorali qiymati: PAYOUT/BONUS musbat, REVERSAL/DEDUCTION manfiy. */
    @Column(name = "signed_amount", precision = 19, scale = 2)
    private BigDecimal signedAmount;

    /** DEDUCTION / REVERSAL / BONUS uchun majburiy izoh. */
    @Column(name = "reason", length = 500)
    private String reason;

    /** REVERSAL satrida — bekor qilinayotgan to'lov id si. */
    @Column(name = "reverses_payment_id")
    private Long reversesPaymentId;

    /** Asl satrda — qachon bekor qilingani (NULL bo'lsa, kuchda). */
    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_by_username", length = 100)
    private String createdByUsername;

    /** Eski satrlar uchun NULL ni PAYOUT deb o'qiydi. */
    public SalaryTransactionType resolvedType() {
        return SalaryTransactionType.orDefault(type);
    }

    /** Eski satrlar uchun signed_amount hali to'ldirilmagan bo'lsa, amount ga qaytadi. */
    public BigDecimal resolvedSignedAmount() {
        if (signedAmount != null) {
            return signedAmount;
        }
        return resolvedType().applySign(amount);
    }

    public boolean isReversed() {
        return reversedAt != null;
    }
}