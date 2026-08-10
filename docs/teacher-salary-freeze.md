# O'qituvchi oyligini muzlatish (salary period freeze)

> Sana: 2026-08-10
> Talab: *"to'langan oylik admin to'laganidan keyin o'zgarmasligi kerak"*

---

## 1. Muammo

`TeacherSalaryPayment` jadvalida faqat `teacher / year / month / amount` saqlanardi —
**hisoblangan oylikning nusxasi yo'q edi**. Har safar o'tgan oy ko'rilganda summa
`calculateTeacherSalary()` orqali **joriy** ma'lumotlardan qaytadan hisoblanardi.

Natijada to'langan, yopilgan oy quyidagi har bir amaldan keyin o'zgarib ketardi:

| Amal | O'tgan, to'langan oyga ta'siri |
|------|-------------------------------|
| `PUT /api/teachers/{id}` — `baseSalary` / `paymentPercentage` / `salaryType` | Barcha o'tgan oylar yangi tarif bo'yicha qayta hisoblanardi |
| `PUT /api/groups/{id}` — guruhga boshqa o'qituvchi tayinlash | Guruhning butun to'lov tarixi yangi o'qituvchiga o'tib ketardi |
| `DELETE /api/groups/{id}/students/{studentId}` | Chiqarilgan o'quvchining eski to'lovlari hisobdan yo'qolardi |
| `DELETE /api/groups/{id}` (soft delete) | Guruh hissasi barcha o'tgan oylardan yo'qolardi |
| `PUT /api/payments/{id}` / `DELETE /api/payments/{id}` | O'tgan oy oyligi o'zgarardi |
| O'tgan oyga kech kiritilgan o'quvchi to'lovi | O'tgan oy oyligi oshib ketardi |

Ustiga-ustak `remainingAmount` 0 dan pastga tushmasligi uchun qirqilardi — ya'ni
oylik kamayib ketsa, **ortiqcha to'langani umuman ko'rinmasdi**.

---

## 2. Yechim

### Davr yopilishi (period close)

Yangi `teacher_salary_periods` jadvali har bir (o'qituvchi, yil, oy) uchun bitta
satr saqlaydi. Davr yopilganda o'sha paytdagi hisob-kitob shu yerga nusxalanadi:
tarif, foiz, guruhlar kesimidagi tafsilot va jami summa.

**Yopilish qoidasi — ikkala shart ham bajarilganda avtomatik:**

1. oy tugagan (joriy oydan oldin), **va**
2. o'sha oy uchun kamida bitta to'lov (`PAYOUT`) qilingan.

> Joriy oy hech qachon muzlatilmaydi. Ya'ni oy o'rtasida avans berilsa ham oylik
> o'quvchilar to'lovi tushgani sayin avvalgidek o'sib boraveradi — bu xatti-harakat
> o'zgarmagan.

Yopilgandan keyin `GET /calculate/...` va `GET /history/...` summani **qayta
hisoblamaydi**, muzlatilgan nusxadan o'qiydi.

### Daftar (ledger)

`teacher_salary_payments` endi haqiqiy daftar. Har bir satrda `type` bor:

| `type` | Ishorasi | Ma'nosi |
|--------|----------|---------|
| `PAYOUT` | `+` | O'qituvchiga berilgan pul. Hisobotlarda **xarajat**. |
| `REVERSAL` | `−` | Xato `PAYOUT` ni bekor qilish. Hisobotlarda **xarajat** (minus). |
| `BONUS` | `+` | Ishlab topilgan summani oshiradi. Kassa harakati **emas**. |
| `DEDUCTION` | `−` | Jarima — ishlab topilgan summani kamaytiradi. Kassa harakati **emas**. |

Yopilgan oyni ataylab o'zgartirish kerak bo'lsa — ikki yo'l bor, ikkalasi ham
auditga tushadi: `BONUS`/`DEDUCTION` yozuvi qo'shish yoki davrni `reopen` qilish.

---

## 3. Baza o'zgarishlari

`spring.jpa.hibernate.ddl-auto=update` bo'lgani uchun **qo'lda SQL kerak emas** —
Hibernate o'zi qo'shadi:

**Yangi jadvallar:** `teacher_salary_periods`, `teacher_salary_period_lines`.

**`teacher_salary_payments` ga qo'shiladigan ustunlar** (hammasi `NULL` bo'lishi
mumkin — mavjud satrlarga `NOT NULL` ustun qo'shib bo'lmaydi):

```
type, signed_amount, reason, reverses_payment_id,
reversed_at, created_by_user_id, created_by_username
```

Mavjud ustunlar va jadvallarga **tegilmagan** — hech narsa o'chirilmagan yoki
qayta nomlanmagan.

### Migratsiya (avtomatik, ishga tushishda)

`SalaryLedgerBackfillRunner` ikki bosqichni bajaradi. **Idempotent** — qayta
ishga tushsa ham xavfsiz, xato bo'lsa ilova baribir ko'tariladi:

1. `type IS NULL` bo'lgan barcha eski satrlar → `type = PAYOUT`, `signed_amount = amount`.
2. To'lovi bor va tugagan har bir oy → `CLOSED` davr sifatida muzlatiladi,
   `backfilled = true` bilan belgilanadi.

> **Cheklov:** eski oylar migratsiya paytidagi ma'lumot bo'yicha muzlatiladi —
> bazada haqiqiy to'lov paytidagi holat saqlanmagan. Shuning uchun ular
> `backfilled = true` deb belgilanadi. Deploydan keyingi barcha oylar aniq bo'ladi.

O'chirish kerak bo'lsa: `istudy.salary.backfill.enabled=false`.

---

## 4. Frontend: o'zgarmagan narsalar

Quyidagi endpointlarning **URL va mavjud maydonlari o'zgarmagan** — eski frontend
hech qanday o'zgartirishsiz ishlashda davom etadi:

```
GET    /api/teacher-salaries/calculate/teacher/{teacherId}?year&month
GET    /api/teacher-salaries/calculate/branch/{branchId}?year&month
POST   /api/teacher-salaries/payments
POST   /api/teacher-salaries/subtract
GET    /api/teacher-salaries/payments/branch/{branchId}
GET    /api/teacher-salaries/payments/teacher/{teacherId}
GET    /api/teacher-salaries/payments/teacher/{teacherId}/month?year&month
GET    /api/teacher-salaries/history/teacher/{teacherId}
GET    /api/teacher-salaries/remaining/teacher/{teacherId}?year&month
DELETE /api/teacher-salaries/payments/{paymentId}
```

`remainingAmount` avvalgidek hech qachon manfiy bo'lmaydi.

---

## 5. Frontend: yangi maydonlar (qo'shimcha, majburiy emas)

### `SalaryCalculationDto`

```json
{
  "...": "eski maydonlar o'zgarmagan",

  "status": "CLOSED",
  "frozen": true,
  "closedAt": "2026-08-01T10:15:00",
  "closedBy": "admin",
  "reopenCount": 0,

  "earningsAdjustments": -100000.00,
  "balance": -100000.00,
  "overpaid": true,

  "driftDetected": null,
  "driftAmount": null
}
```

| Maydon | Izoh |
|--------|------|
| `status` | `OPEN` — jonli hisoblanadi, `CLOSED` — muzlatilgan |
| `frozen` | `true` bo'lsa summa endi o'zgarmaydi |
| `earningsAdjustments` | `BONUS − DEDUCTION`. `totalSalary` ichiga allaqachon qo'shilgan |
| `balance` | `totalSalary − alreadyPaid`. **Manfiy bo'lishi mumkin** |
| `overpaid` | `true` — o'qituvchiga keragidan ko'p berilgan |
| `driftDetected` / `driftAmount` | Faqat `?includeDrift=true` bilan to'ladi |

> **Tavsiya:** oylik kartasida `frozen = true` bo'lsa 🔒 belgisi ko'rsatilsin, va
> `overpaid = true` bo'lsa `balance` qizil rangda chiqarilsin — hozir bu holat
> umuman ko'rinmayapti.

**Yangi so'rov parametri:**

```
GET /api/teacher-salaries/calculate/teacher/{teacherId}?year=2026&month=6&includeDrift=true
```

Yopilgandan keyin o'quvchi to'lovlari yoki guruhlar o'zgargan bo'lsa,
`driftAmount` "hozir hisoblansa qancha farq bo'lardi" ni ko'rsatadi. Summaga
ta'sir qilmaydi — faqat admin uchun signal.

### `TeacherSalaryHistoryDto`

Qo'shildi: `status`, `frozen`, `closedAt`, `earningsAdjustments`, `balance`, `overpaid`.

### `TeacherSalaryPaymentDto`

Qo'shildi: `type`, `signedAmount`, `settlement`, `reason`, `reversesPaymentId`,
`reversedAt`, `reversed`, `createdByUsername`.

> **Tavsiya:** to'lovlar ro'yxatida `type` bo'yicha rang/belgi berilsin va
> `reversed = true` bo'lgan satrlar ustidan chizilsin.

### `CreateSalaryPaymentRequest` — ixtiyoriy maydonlar

```json
{
  "teacherId": 1, "year": 2026, "month": 6,
  "amount": 500000.00, "branchId": 1,
  "description": "Iyun oyligi",

  "type": "PAYOUT",
  "reason": "Darsga kelmagani uchun"
}
```

`type` yuborilmasa: `/payments` → `PAYOUT`, `/subtract` → `DEDUCTION`.

---

## 6. Yangi endpointlar

```
POST /api/teacher-salaries/payments/{paymentId}/reverse
```
```json
{ "reason": "Xato summa kiritilgan" }
```
To'lovni **o'chirmaydi** — teskari yozuv qo'shadi. Javob: `TeacherSalaryPaymentDto`.

```
GET  /api/teacher-salaries/periods/teacher/{teacherId}
GET  /api/teacher-salaries/periods/teacher/{teacherId}/month?year&month
```
`TeacherSalaryPeriodDto` — muzlatilgan hisob-kitob, guruhlar kesimi va audit
(`closedAt`, `closedBy`, `autoClosed`, `backfilled`, `reopenCount`,
`lastReopenedAt`, `lastReopenedBy`, `lastReopenReason`).

```
POST /api/teacher-salaries/periods/close
```
```json
{ "teacherId": 1, "year": 2026, "month": 6, "branchId": 1 }
```
Oyni qo'lda yopadi (to'lovsiz ham) — summani shu paytdan muzlatadi.

```
POST /api/teacher-salaries/periods/reopen        ← faqat SUPER_ADMIN
```
```json
{ "teacherId": 1, "year": 2026, "month": 6, "branchId": 1,
  "reason": "Tarif noto'g'ri kiritilgan edi" }
```
`reason` majburiy. Keyingi o'qishda summa qayta hisoblanadi va yana muzlatiladi.

---

## 7. ⚠️ O'zgargan xatti-harakatlar

### `POST /api/teacher-salaries/subtract`

**Ilgari:** `createSalaryPayment` bilan bir xil ishlardi — musbat satr yozib,
"to'langan" summani **oshirardi** va hisobotlarda kassa xarajati sifatida ko'rinardi.
Ya'ni jarima o'qituvchiga pul berilgandek yozilardi.

**Endi:** haqiqiy `DEDUCTION` yozuvi. Ishlab topilgan oylikni kamaytiradi va
moliyaviy hisobotlarda **xarajat sifatida ko'rinmaydi**.

> So'rov formati o'zgarmagan. Frontend'da bu tugma "Ushlab qolish / Jarima" deb
> nomlangan bo'lsa — endi u nomiga mos ishlaydi.

### `DELETE /api/teacher-salaries/payments/{paymentId}`

**Ilgari:** har doim jismonan o'chirardi.

**Endi:**
- davr hali **ochiq** bo'lsa — avvalgidek o'chiradi;
- davr **yopilgan** bo'lsa — o'chirmaydi, teskari yozuv qo'shadi (tarix saqlanadi).

Ikkala holatda ham `200 OK` qaytadi va balans bir xil o'zgaradi, shuning uchun
frontend uchun farqi yo'q.

### Moliyaviy hisobotlar

`GET /api/reports/financial/**` dagi `salaryPayments` endi faqat kassa
harakatlarini (`PAYOUT − REVERSAL`) sanaydi. Eski satrlarning hammasi `PAYOUT`
bo'lgani uchun **mavjud raqamlar o'zgarmaydi**.

---

## 8. Nazorat

`TeacherSalaryFreezeTest` (H2, 11 ta test) quyidagilarni tekshiradi:

- to'langandan keyin tarif o'zgarsa — o'tgan oy summasi o'zgarmaydi;
- guruh boshqa o'qituvchiga o'tkazilsa — o'zgarmaydi;
- o'quvchi guruhdan chiqarilsa — o'zgarmaydi;
- joriy oy avansdan keyin ham o'sib boraveradi (regressiya yo'q);
- `subtract` haqiqatan ayiradi va kassaga tegmaydi;
- bekor qilish tarixni saqlaydi; yopilgan davrda `DELETE` teskari yozuvga aylanadi;
- `reopen` qayta hisoblaydi va yana muzlatadi;
- migratsiya idempotent va migratsiyadan oldin ham hisobotlar to'g'ri.

```bash
./gradlew test --tests 'com.ogabek.istudy.service.TeacherSalaryFreezeTest'
```

---

## 9. Ortga qaytarish (rollback)

Kod eski versiyaga qaytarilsa baza buzilmaydi: yangi jadvallar shunchaki
ishlatilmay qoladi, `teacher_salary_payments` dagi yangi ustunlar esa `NULL`
bo'lishi mumkin va eski kod ularni umuman o'qimaydi.

Faqat migratsiyani qayta ishga tushirmoqchi bo'lsangiz:

```sql
DELETE FROM teacher_salary_period_lines;
DELETE FROM teacher_salary_periods;
UPDATE teacher_salary_payments SET type = NULL, signed_amount = NULL;
```

---

## 10. Keyingi qadam (bu ishga kirmagan)

`teacher_salary_payments.amount`, `groups.price`, `teachers.base_salary` va
boshqa pul ustunlari `precision = 10, scale = 2` — ya'ni maksimal
**99 999 999.99**. So'm uchun bu past chegara. `ddl-auto=update` mavjud ustun
turini o'zgartirmaydi, shuning uchun bu alohida qo'lda SQL talab qiladi:
`docs/sql/optional-widen-money-columns.sql`.
