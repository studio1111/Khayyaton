package com.example

import com.example.util.PersianUtils
import org.junit.Assert.assertTrue
import org.junit.Test

class PersianUtilsRegressionTest {

    @Test
    fun modelColorNeverCrashesWhenHashIsIntMinValue() {
        // This string has Java String.hashCode() == Int.MIN_VALUE.
        assertTrue(PersianUtils.getModelColor("polygenelubricants").startsWith("#"))
    }
}
