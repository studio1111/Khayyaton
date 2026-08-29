package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.PersianUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SheetOn", appName)
  }

  @Test
  fun `test persian formula evaluation`() {
    val result = PersianUtils.evaluateCountFormula("3+3+2+1+1")
    assertEquals(10.0, result, 0.001)

    val fractional = PersianUtils.evaluateCountFormula("3+2+1.5")
    assertEquals(6.5, fractional, 0.001)
  }
}
