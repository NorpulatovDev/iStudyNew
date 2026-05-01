# Frontend Changes & New Features

> Sana: 2026-05-01
> Commit: `26eb697`

---

## 1. Yangi: Sotuv moduli `/api/sales`

Kitob, daftar, ruchka va boshqa mahsulotlar sotuvini boshqarish uchun yangi endpoint to'plami.

### So'rov modeli — `CreateSaleRequest`

```json
{
  "itemName": "Matematika kitobi",
  "quantity": 3,
  "unitPrice": 25000.00,
  "paymentMethod": "CASH",
  "description": "5-sinf uchun",
  "branchId": 1
}
```

| Field | Type | Majburiy | Izoh |
|-------|------|----------|------|
| `itemName` | string | ✅ | Max 255 ta belgi |
| `quantity` | integer | ✅ | Kamida 1 |
| `unitPrice` | decimal | ✅ | 0 dan katta |
| `paymentMethod` | enum | ✅ | `CASH` yoki `CARD` |
| `description` | string | ❌ | Max 255 ta belgi |
| `branchId` | long | ✅ | |

### Javob modeli — `SaleDto`

```json
{
  "id": 1,
  "itemName": "Matematika kitobi",
  "quantity": 3,
  "unitPrice": 25000.00,
  "totalAmount": 75000.00,
  "paymentMethod": "CASH",
  "description": "5-sinf uchun",
  "branchId": 1,
  "branchName": "Chilonzor filiali",
  "saleYear": 2026,
  "saleMonth": 5,
  "createdAt": "2026-05-01T10:30:00"
}
```

> `totalAmount` backend tomonidan avtomatik hisoblanadi: `quantity × unitPrice`

---

### Endpointlar

#### Ro'yxat va filtrlar

```
GET /api/sales?branchId={id}
```
Barcha sotuvlar (yangi → eski tartibda).

```
GET /api/sales/monthly?branchId={id}&year={year}&month={month}
```
Oylik sotuvlar ro'yxati.

```
GET /api/sales/daily?branchId={id}&date={YYYY-MM-DD}
```
Kunlik sotuvlar ro'yxati.

#### Summaries (jami + ro'yxat birgalikda)

```
GET /api/sales/monthly/summary?branchId={id}&year={year}&month={month}
```

```json
{
  "sales": [ ...SaleDto array... ],
  "total": 450000.00,
  "count": 12,
  "year": 2026,
  "month": 5,
  "branchId": 1
}
```

```
GET /api/sales/daily/summary?branchId={id}&date={YYYY-MM-DD}
```

```json
{
  "sales": [ ...SaleDto array... ],
  "total": 75000.00,
  "count": 3,
  "date": "2026-05-01",
  "branchId": 1
}
```

#### CRUD

```
GET    /api/sales/{id}           → SaleDto
POST   /api/sales                → SaleDto       (body: CreateSaleRequest)
PUT    /api/sales/{id}           → SaleDto       (body: CreateSaleRequest)
DELETE /api/sales/{id}           → 200 OK
```

---

## 2. O'zgargan: Moliyaviy hisobot

### `GET /api/reports/financial/summary`

```
GET /api/reports/financial/summary?branchId={id}&year={year}&month={month}
```

**Oldingi javob:**
```json
{
  "totalIncome": 5000000.00,
  "regularExpenses": 1000000.00,
  "salaryPayments": 800000.00,
  "totalExpenses": 1800000.00,
  "netProfit": 3200000.00,
  ...
}
```

**Yangi javob** (`salesIncome` va `studentPayments` qo'shildi):
```json
{
  "studentPayments": 4500000.00,
  "salesIncome": 500000.00,
  "totalIncome": 5000000.00,
  "regularExpenses": 1000000.00,
  "salaryPayments": 800000.00,
  "totalExpenses": 1800000.00,
  "netProfit": 3200000.00,
  "year": 2026,
  "month": 5,
  "branchId": 1,
  "type": "FINANCIAL_SUMMARY"
}
```

| Yangi field | Izoh |
|-------------|------|
| `studentPayments` | Faqat o'quvchilar to'lovlari jami (oldingi `totalIncome`) |
| `salesIncome` | Mahsulot sotuvidan tushgan daromad |
| `totalIncome` | `studentPayments + salesIncome` |

> ⚠️ `totalIncome` endi faqat student to'lovlarini emas, sotuv daromadini ham o'z ichiga oladi.
> Agar frontend `totalIncome` ni faqat student to'lovlari sifatida ko'rsatgan bo'lsa — uni `studentPayments` ga almashtirish kerak.

---

### `GET /api/reports/financial/summary-range`

```
GET /api/reports/financial/summary-range?branchId={id}&startDate={YYYY-MM-DD}&endDate={YYYY-MM-DD}
```

Xuddi yuqoridagi kabi — `studentPayments` va `salesIncome` alohida fieldlar bilan keladi.

---

## 3. O'zgarmagan endpointlar

Quyidagi hamma narsa avvalgidek ishlaydi — frontend o'zgartirish talab qilmaydi:

- `/api/students/**`
- `/api/payments/**`
- `/api/groups/**`
- `/api/expenses/**`
- `/api/teachers/**`
- `/api/reports/payments/**`
- `/api/reports/expenses/**`
- `/api/dashboard`
