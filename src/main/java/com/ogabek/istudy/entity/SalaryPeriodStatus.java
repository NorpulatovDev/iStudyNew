package com.ogabek.istudy.entity;

/**
 * O'qituvchining bir oylik davri holati.
 */
public enum SalaryPeriodStatus {

    /**
     * Davr ochiq — oylik har safar joriy ma'lumotlardan qayta hisoblanadi.
     * Oy hali tugamagan yoki hech qanday to'lov qilinmagan.
     */
    OPEN,

    /**
     * Davr yopiq — hisoblangan oylik "muzlatilgan". Guruh, o'quvchi, tarif yoki
     * to'lovlar keyinchalik o'zgarsa ham bu davr summasi o'zgarmaydi.
     */
    CLOSED
}
