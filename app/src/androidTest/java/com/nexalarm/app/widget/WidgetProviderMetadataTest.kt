package com.nexalarm.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetProviderMetadataTest {

    @Test
    fun packagePublishesFourResizableWidgetsWithPickerPreviews() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val providers = AppWidgetManager.getInstance(context).installedProviders
            .filter { it.provider.packageName == context.packageName }

        assertEquals(
            setOf(
                ClockWidgetProvider::class.java.name,
                VerticalClockWidgetProvider::class.java.name,
                WeatherClockWidgetProvider::class.java.name,
                DateClockWidgetProvider::class.java.name,
            ),
            providers.map { it.provider.className }.toSet(),
        )
        assertEquals(4, providers.size)

        providers.forEach { provider ->
            assertTrue(provider.initialLayout != 0)
            assertTrue(provider.previewImage != 0)
            assertTrue(provider.minWidth > 0)
            assertTrue(provider.minHeight > 0)
            assertEquals(
                AppWidgetProviderInfo.RESIZE_HORIZONTAL or AppWidgetProviderInfo.RESIZE_VERTICAL,
                provider.resizeMode,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                assertTrue(provider.previewLayout != 0)
            }
        }
    }
}
