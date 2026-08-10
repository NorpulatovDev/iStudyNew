package com.ogabek.istudy.service;

import com.ogabek.istudy.dto.request.CreateSalaryPaymentRequest;
import com.ogabek.istudy.dto.response.SalaryCalculationDto;
import com.ogabek.istudy.dto.response.TeacherSalaryPaymentDto;
import com.ogabek.istudy.entity.*;
import com.ogabek.istudy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * "To'langan oylik keyinchalik o'zgarmasligi kerak" talabining tekshiruvi.
 *
 * <p>Testlar ataylab {@code @Transactional} emas: {@code SalaryPeriodService}
 * davrni {@code REQUIRES_NEW} tranzaksiyada muzlatadi va u faqat commit qilingan
 * ma'lumotni ko'radi — production dagi kabi.
 */
@SpringBootTest
@ActiveProfiles("test")
class TeacherSalaryFreezeTest {

    @Autowired private TeacherSalaryService salaryService;
    @Autowired private SalaryPeriodService periodService;
    @Autowired private SalaryLedgerBackfillService backfillService;

    @Autowired private TeacherSalaryPeriodRepository periodRepository;
    @Autowired private TeacherSalaryPaymentRepository salaryPaymentRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private BranchRepository branchRepository;

    private Branch branch;
    private Teacher teacher;
    private Group group;
    private Student student;

    private static final YearMonth LAST_MONTH = YearMonth.now().minusMonths(1);
    private static final YearMonth THIS_MONTH = YearMonth.now();

    @BeforeEach
    void setUp() {
        periodRepository.deleteAll();
        salaryPaymentRepository.deleteAll();
        paymentRepository.deleteAll();
        groupRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();

        branch = new Branch();
        branch.setName("Test filiali");
        branch.setAddress("Toshkent");
        branch = branchRepository.save(branch);

        teacher = newTeacher("Ali", "Valiyev", SalaryType.PERCENTAGE, BigDecimal.ZERO, new BigDecimal("50.00"));

        student = new Student();
        student.setFirstName("Bobur");
        student.setLastName("Toshmatov");
        student.setBranch(branch);
        student = studentRepository.save(student);

        group = new Group();
        group.setName("Matematika A");
        group.setPrice(new BigDecimal("1000000.00"));
        group.setTeacher(teacher);
        group.setBranch(branch);
        group.setStudents(new HashSet<>(List.of(student)));
        group = groupRepository.save(group);
    }

    // ==================================================================
    // Asosiy talab
    // ==================================================================

    @Test
    @DisplayName("To'langandan keyin tarif o'zgarsa ham o'tgan oy oyligi o'zgarmaydi")
    void frozenPeriodIgnoresTeacherRateChange() {
        payStudent(LAST_MONTH, "1000000.00");

        // 50% dan 1 000 000 → 500 000
        SalaryCalculationDto before = calculate(LAST_MONTH);
        assertThat(before.getTotalSalary()).isEqualByComparingTo("500000.00");
        assertThat(before.getStatus()).isEqualTo("OPEN");

        paySalary(LAST_MONTH, "500000.00");

        SalaryCalculationDto afterPayout = calculate(LAST_MONTH);
        assertThat(afterPayout.getStatus()).isEqualTo("CLOSED");
        assertThat(afterPayout.isFrozen()).isTrue();
        assertThat(afterPayout.getTotalSalary()).isEqualByComparingTo("500000.00");
        assertThat(afterPayout.getRemainingAmount()).isEqualByComparingTo("0.00");

        // Admin o'qituvchi foizini oshiradi — bu o'tgan, to'langan oyga tegmasligi kerak
        teacher.setPaymentPercentage(new BigDecimal("80.00"));
        teacherRepository.save(teacher);

        SalaryCalculationDto afterRateChange = calculate(LAST_MONTH);
        assertThat(afterRateChange.getTotalSalary())
                .as("to'langan oy summasi tarif o'zgarganda ham o'zgarmasligi kerak")
                .isEqualByComparingTo("500000.00");
        assertThat(afterRateChange.getRemainingAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Guruh boshqa o'qituvchiga o'tkazilsa ham muzlatilgan oy o'zgarmaydi")
    void frozenPeriodIgnoresGroupReassignment() {
        payStudent(LAST_MONTH, "1000000.00");
        paySalary(LAST_MONTH, "500000.00");
        assertThat(calculate(LAST_MONTH).getTotalSalary()).isEqualByComparingTo("500000.00");

        Teacher other = newTeacher("Sardor", "Qodirov", SalaryType.PERCENTAGE, BigDecimal.ZERO, new BigDecimal("50.00"));
        group.setTeacher(other);
        groupRepository.save(group);

        assertThat(calculate(LAST_MONTH).getTotalSalary())
                .as("guruh o'tkazilgani eski o'qituvchining to'langan oyiga ta'sir qilmasligi kerak")
                .isEqualByComparingTo("500000.00");

        // Yangi o'qituvchida o'sha o'tgan oy uchun qarz paydo bo'lmasligi kerak edi —
        // lekin uning davri hali muzlatilmagan, shuning uchun jonli hisoblanadi.
        // Muhimi: eski o'qituvchining muzlatilgan summasi buzilmadi.
        assertThat(calculate(LAST_MONTH).getGroups()).hasSize(1);
    }

    @Test
    @DisplayName("O'quvchi guruhdan chiqarilsa ham muzlatilgan oy o'zgarmaydi")
    void frozenPeriodIgnoresStudentRemoval() {
        payStudent(LAST_MONTH, "1000000.00");
        paySalary(LAST_MONTH, "500000.00");

        group.getStudents().clear();
        groupRepository.save(group);

        SalaryCalculationDto after = calculate(LAST_MONTH);
        assertThat(after.getTotalSalary()).isEqualByComparingTo("500000.00");
        assertThat(after.getGroups()).hasSize(1);
        assertThat(after.getGroups().get(0).getGroupName()).isEqualTo("Matematika A");
    }

    @Test
    @DisplayName("Joriy oy muzlatilmaydi — avans berilsa ham oylik o'sib boraveradi")
    void currentMonthKeepsAccruingAfterAdvance() {
        payStudent(THIS_MONTH, "1000000.00");
        paySalary(THIS_MONTH, "200000.00");

        SalaryCalculationDto afterAdvance = calculate(THIS_MONTH);
        assertThat(afterAdvance.getStatus()).isEqualTo("OPEN");
        assertThat(afterAdvance.isFrozen()).isFalse();
        assertThat(afterAdvance.getTotalSalary()).isEqualByComparingTo("500000.00");

        // Oy davomida yana bir o'quvchi to'ladi
        Student second = new Student();
        second.setFirstName("Dilnoza");
        second.setLastName("Karimova");
        second.setBranch(branch);
        second = studentRepository.save(second);
        group.getStudents().add(second);
        group = groupRepository.save(group);
        payStudent(second, THIS_MONTH, "1000000.00");

        assertThat(calculate(THIS_MONTH).getTotalSalary())
                .as("joriy oy hali yopilmagan — oylik o'sishi kerak")
                .isEqualByComparingTo("1000000.00");
    }

    // ==================================================================
    // Daftar
    // ==================================================================

    @Test
    @DisplayName("subtractFromSalary endi haqiqatan ayiradi va kassa xarajati sifatida hisoblanmaydi")
    void deductionReducesEarningsWithoutCashImpact() {
        payStudent(LAST_MONTH, "1000000.00");
        paySalary(LAST_MONTH, "500000.00");

        BigDecimal cashBefore = salaryPaymentRepository.sumMonthlySalaryPayments(
                branch.getId(), LAST_MONTH.getYear(), LAST_MONTH.getMonthValue());
        assertThat(cashBefore).isEqualByComparingTo("500000.00");

        CreateSalaryPaymentRequest fine = request(LAST_MONTH, "100000.00");
        fine.setReason("Darsga kelmagani uchun");
        salaryService.subtractFromSalary(fine);

        SalaryCalculationDto after = calculate(LAST_MONTH);
        assertThat(after.getEarningsAdjustments()).isEqualByComparingTo("-100000.00");
        assertThat(after.getTotalSalary()).isEqualByComparingTo("400000.00");
        assertThat(after.getAlreadyPaid()).isEqualByComparingTo("500000.00");
        assertThat(after.getBalance()).isEqualByComparingTo("-100000.00");
        assertThat(after.isOverpaid()).isTrue();
        assertThat(after.getRemainingAmount())
                .as("eski maydon manfiy bo'lmasligi kerak")
                .isEqualByComparingTo("0.00");

        assertThat(salaryPaymentRepository.sumMonthlySalaryPayments(
                branch.getId(), LAST_MONTH.getYear(), LAST_MONTH.getMonthValue()))
                .as("jarima kassadan pul chiqishi emas")
                .isEqualByComparingTo("500000.00");
    }

    @Test
    @DisplayName("To'lovni bekor qilish tarixni o'chirmaydi — teskari yozuv qo'shadi")
    void reversalKeepsHistory() {
        payStudent(LAST_MONTH, "1000000.00");
        TeacherSalaryPaymentDto payout = paySalary(LAST_MONTH, "500000.00");

        salaryService.reverseSalaryPayment(payout.getId(), "Xato kiritilgan");

        assertThat(calculate(LAST_MONTH).getAlreadyPaid()).isEqualByComparingTo("0.00");
        assertThat(salaryPaymentRepository.findAll()).hasSize(2);

        assertThatThrownBy(() -> salaryService.reverseSalaryPayment(payout.getId(), "yana"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("allaqachon bekor qilingan");
    }

    @Test
    @DisplayName("Yopilgan davrda DELETE yozuvni o'chirmaydi, teskari yozuv qo'shadi")
    void deleteOnClosedPeriodBecomesReversal() {
        payStudent(LAST_MONTH, "1000000.00");
        TeacherSalaryPaymentDto payout = paySalary(LAST_MONTH, "500000.00");
        assertThat(calculate(LAST_MONTH).isFrozen()).isTrue();

        salaryService.deleteSalaryPayment(payout.getId());

        assertThat(salaryPaymentRepository.existsById(payout.getId()))
                .as("yopilgan oyning to'lov tarixi yo'qolmasligi kerak")
                .isTrue();
        assertThat(calculate(LAST_MONTH).getAlreadyPaid()).isEqualByComparingTo("0.00");
    }

    // ==================================================================
    // Davr boshqaruvi
    // ==================================================================

    @Test
    @DisplayName("reopen dan keyin summa qayta hisoblanadi va yana muzlatiladi")
    void reopenRecalculatesThenRefreezes() {
        payStudent(LAST_MONTH, "1000000.00");
        paySalary(LAST_MONTH, "500000.00");

        teacher.setPaymentPercentage(new BigDecimal("80.00"));
        teacherRepository.save(teacher);
        assertThat(calculate(LAST_MONTH).getTotalSalary()).isEqualByComparingTo("500000.00");

        salaryService.reopenPeriod(teacher.getId(), LAST_MONTH.getYear(), LAST_MONTH.getMonthValue(),
                "Tarif noto'g'ri kiritilgan edi");

        SalaryCalculationDto after = calculate(LAST_MONTH);
        assertThat(after.getTotalSalary())
                .as("qayta ochilgach yangi tarif bo'yicha hisoblanadi")
                .isEqualByComparingTo("800000.00");
        assertThat(after.isFrozen()).as("va darhol qayta muzlatiladi").isTrue();
        assertThat(after.getReopenCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("includeDrift yopilgandan keyingi o'zgarishni ko'rsatadi")
    void driftIsReportedButNotApplied() {
        payStudent(LAST_MONTH, "1000000.00");
        paySalary(LAST_MONTH, "500000.00");

        payStudent(LAST_MONTH, "400000.00");

        SalaryCalculationDto withDrift = salaryService.calculateTeacherSalary(
                teacher.getId(), LAST_MONTH.getYear(), LAST_MONTH.getMonthValue(), true);

        assertThat(withDrift.getTotalSalary()).isEqualByComparingTo("500000.00");
        assertThat(withDrift.getDriftDetected()).isTrue();
        assertThat(withDrift.getDriftAmount()).isEqualByComparingTo("200000.00");
    }

    // ==================================================================
    // Migratsiya
    // ==================================================================

    @Test
    @DisplayName("Migratsiya eski satrlarni PAYOUT qiladi va to'langan o'tgan oylarni muzlatadi")
    void backfillTypesLegacyRowsAndFreezesElapsedPeriods() {
        payStudent(LAST_MONTH, "1000000.00");

        // Eski kod yozgan satrni taqlid qilamiz: type va signed_amount yo'q
        TeacherSalaryPayment legacy = new TeacherSalaryPayment();
        legacy.setTeacher(teacher);
        legacy.setBranch(branch);
        legacy.setYear(LAST_MONTH.getYear());
        legacy.setMonth(LAST_MONTH.getMonthValue());
        legacy.setAmount(new BigDecimal("500000.00"));
        legacy.setDescription("Eski to'lov");
        legacy = salaryPaymentRepository.save(legacy);

        assertThat(salaryPaymentRepository.countUntypedRows()).isEqualTo(1);
        // Migratsiyadan oldin ham hisobot to'g'ri (NULL fallback ishlaydi)
        assertThat(salaryPaymentRepository.sumMonthlySalaryPayments(
                branch.getId(), LAST_MONTH.getYear(), LAST_MONTH.getMonthValue()))
                .isEqualByComparingTo("500000.00");

        backfillService.backfillLedgerTypes();
        assertThat(salaryPaymentRepository.countUntypedRows()).isZero();
        assertThat(salaryPaymentRepository.findById(legacy.getId()).orElseThrow().getType())
                .isEqualTo(SalaryTransactionType.PAYOUT);

        int frozen = backfillService.freezeHistoricalPeriods();
        assertThat(frozen).isEqualTo(1);

        TeacherSalaryPeriod period = periodRepository
                .findByTeacherIdAndYearAndMonth(teacher.getId(), LAST_MONTH.getYear(), LAST_MONTH.getMonthValue())
                .orElseThrow();
        assertThat(period.getStatus()).isEqualTo(SalaryPeriodStatus.CLOSED);
        assertThat(period.getBackfilled()).isTrue();
        assertThat(period.getTotalSalarySnapshot()).isEqualByComparingTo("500000.00");

        // Ikkinchi marta ishga tushirish hech narsani o'zgartirmaydi
        assertThat(backfillService.backfillLedgerTypes()).isZero();
        assertThat(backfillService.freezeHistoricalPeriods()).isZero();
    }

    @Test
    @DisplayName("To'lovsiz o'tgan oy ochiq qoladi — muzlatadigan narsa yo'q")
    void elapsedMonthWithoutPayoutStaysOpen() {
        payStudent(LAST_MONTH, "1000000.00");

        SalaryCalculationDto dto = calculate(LAST_MONTH);
        assertThat(dto.getStatus()).isEqualTo("OPEN");
        assertThat(periodService.shouldAutoClose(
                teacher.getId(), LAST_MONTH.getYear(), LAST_MONTH.getMonthValue())).isFalse();
    }

    // ==================================================================
    // Yordamchilar
    // ==================================================================

    private Teacher newTeacher(String first, String last, SalaryType type,
                               BigDecimal baseSalary, BigDecimal percentage) {
        Teacher t = new Teacher();
        t.setFirstName(first);
        t.setLastName(last);
        t.setSalaryType(type);
        t.setBaseSalary(baseSalary);
        t.setPaymentPercentage(percentage);
        t.setBranch(branch);
        return teacherRepository.save(t);
    }

    private void payStudent(YearMonth when, String amount) {
        payStudent(student, when, amount);
    }

    private void payStudent(Student who, YearMonth when, String amount) {
        Payment payment = new Payment();
        payment.setStudent(who);
        payment.setGroup(group);
        payment.setBranch(branch);
        payment.setAmount(new BigDecimal(amount));
        payment.setCategory(PaymentCategory.CASH);
        payment.setPaymentYear(when.getYear());
        payment.setPaymentMonth(when.getMonthValue());
        paymentRepository.save(payment);
    }

    private TeacherSalaryPaymentDto paySalary(YearMonth when, String amount) {
        return salaryService.createSalaryPayment(request(when, amount));
    }

    private CreateSalaryPaymentRequest request(YearMonth when, String amount) {
        CreateSalaryPaymentRequest request = new CreateSalaryPaymentRequest();
        request.setTeacherId(teacher.getId());
        request.setBranchId(branch.getId());
        request.setYear(when.getYear());
        request.setMonth(when.getMonthValue());
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private SalaryCalculationDto calculate(YearMonth when) {
        return salaryService.calculateTeacherSalary(teacher.getId(), when.getYear(), when.getMonthValue());
    }
}
