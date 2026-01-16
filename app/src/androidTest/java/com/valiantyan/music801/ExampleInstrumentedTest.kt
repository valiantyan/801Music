package com.valiantyan.music801

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext(): Unit {
        // 获取应用上下文，验证包名一致性
        val appContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(expected = "com.valiantyan.music801", actual = appContext.packageName)
    }
}
