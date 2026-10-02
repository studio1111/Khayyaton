package com.example

import com.example.util.PersianUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class PersianCalendarAndDecimalTest {

    @Test
    fun esfandHas30DaysInLeapYears() {
        assertEquals(30, PersianUtils.getDaysInJalaliMonth(1395, 12))
        assertEquals(30, PersianUtils.getDaysInJalaliMonth(1399, 12))
        assertEquals(30, PersianUtils.getDaysInJalaliMonth(1403, 12))
        assertEquals(30, PersianUtils.getDaysInJalaliMonth(1408, 12))
    }

    @Test
    fun esfandHas29DaysInCommonYears() {
        assertEquals(29, PersianUtils.getDaysInJalaliMonth(1402, 12))
        assertEquals(29, PersianUtils.getDaysInJalaliMonth(1404, 12))
        assertEquals(29, PersianUtils.getDaysInJalaliMonth(1405, 12))
    }

    @Test
    fun firstHalfAndSecondHalfMonthLengthsAreUnchanged() {
        assertEquals(31, PersianUtils.getDaysInJalaliMonth(1404, 1))
        assertEquals(31, PersianUtils.getDaysInJalaliMonth(1404, 6))
        assertEquals(30, PersianUtils.getDaysInJalaliMonth(1404, 7))
        assertEquals(30, PersianUtils.getDaysInJalaliMonth(1404, 11))
    }

    @Test
    fun persianDecimalSeparatorIsParsed() {
        assertEquals(0.5, PersianUtils.evaluateCountFormula("۰\u066B۵"), 0.0001)
        assertEquals(2.5, PersianUtils.evaluateCountFormula("۲ + ۰\u066B۵"), 0.0001)
    }

    @Test
    fun existingDecimalFormsStillWork() {
        assertEquals(1.5, PersianUtils.evaluateCountFormula("1.5"), 0.0001)
        assertEquals(1.5, PersianUtils.evaluateCountFormula("۱/۵"), 0.0001)
        assertEquals(6.0, PersianUtils.evaluateCountFormula("3+3"), 0.0001)
    }
}
