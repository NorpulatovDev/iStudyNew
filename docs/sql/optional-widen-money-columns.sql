-- =====================================================================
-- IXTIYORIY — bu skript oylikni muzlatish ishiga KIRMAYDI.
-- Uni ishga tushirmasangiz ham hamma narsa avvalgidek ishlaydi.
--
-- Muammo: pul ustunlari NUMERIC(10,2) — maksimal 99 999 999.99.
-- So'm uchun bu past chegara (~$8k). Hibernate ddl-auto=update mavjud
-- ustun turini o'zgartirmaydi, shuning uchun qo'lda bajarish kerak.
--
-- Ishlatishdan oldin: BAZANING ZAXIRA NUSXASINI OLING.
-- Ishlatish vaqti: kam yuklama paytida (ALTER TABLE jadvalni qulflaydi).
-- =====================================================================

BEGIN;

ALTER TABLE teacher_salary_payments ALTER COLUMN amount        TYPE NUMERIC(19, 2);
ALTER TABLE payments                ALTER COLUMN amount        TYPE NUMERIC(19, 2);
ALTER TABLE groups                  ALTER COLUMN price         TYPE NUMERIC(19, 2);
ALTER TABLE teachers                ALTER COLUMN base_salary   TYPE NUMERIC(19, 2);
ALTER TABLE expenses                ALTER COLUMN amount        TYPE NUMERIC(19, 2);

-- Sotuv moduli (agar mavjud bo'lsa)
ALTER TABLE sales                   ALTER COLUMN unit_price    TYPE NUMERIC(19, 2);
ALTER TABLE sales                   ALTER COLUMN total_amount  TYPE NUMERIC(19, 2);

COMMIT;

-- =====================================================================
-- Skript bajarilgandan keyin entity larda ham precision = 19 qilib
-- qo'yish kerak, aks holda ular hujjat sifatida noto'g'ri qoladi:
--   Payment.amount, Group.price, Teacher.baseSalary,
--   Expense.amount, Sale.unitPrice, Sale.totalAmount,
--   TeacherSalaryPayment.amount
--
-- Tekshirish:
--   SELECT table_name, column_name, numeric_precision, numeric_scale
--   FROM information_schema.columns
--   WHERE numeric_precision = 10 AND numeric_scale = 2;
-- =====================================================================
