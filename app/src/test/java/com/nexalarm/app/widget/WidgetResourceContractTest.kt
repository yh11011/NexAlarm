package com.nexalarm.app.widget

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class WidgetResourceContractTest {

    private val mainDirectory = sequenceOf(File("src/main"), File("app/src/main"))
        .first { it.isDirectory }

    @Test
    fun manifestDeclaresExactlyFourIndependentWidgetsWithoutConfigurationActivity() {
        val manifest = xml(File(mainDirectory, "AndroidManifest.xml"))
        val receivers = manifest.getElementsByTagName("receiver")
        val widgetProviders = buildList {
            repeat(receivers.length) { index ->
                val receiver = receivers.item(index) as Element
                val metadata = receiver.getElementsByTagName("meta-data")
                if ((0 until metadata.length).any {
                        (metadata.item(it) as Element).androidAttribute("name") ==
                            "android.appwidget.provider"
                    }
                ) {
                    add(receiver.androidAttribute("name"))
                }
            }
        }

        assertEquals(
            setOf(
                ".widget.ClockWidgetProvider",
                ".widget.VerticalClockWidgetProvider",
                ".widget.WeatherClockWidgetProvider",
                ".widget.DateClockWidgetProvider",
            ),
            widgetProviders.toSet(),
        )
        assertEquals(4, widgetProviders.size)
        assertFalse(File(mainDirectory, "java/com/nexalarm/app/widget/WidgetConfigActivity.kt").exists())
        val actions = manifest.getElementsByTagName("action")
        assertFalse((0 until actions.length).any {
            (actions.item(it) as Element).androidAttribute("name") ==
                "android.appwidget.action.APPWIDGET_CONFIGURE"
        })
    }

    @Test
    fun everyWidgetHasLayoutPreviewAndUniqueStaticFallbackPreview() {
        val expected = mapOf(
            "clock_widget_info.xml" to Pair("@layout/widget_clock", "@drawable/widget_preview_classic"),
            "widget_vertical_info.xml" to Pair("@layout/widget_clock_vertical", "@drawable/widget_preview_vertical"),
            "widget_weather_info.xml" to Pair("@layout/widget_clock_weather", "@drawable/widget_preview_weather"),
            "widget_date_info.xml" to Pair("@layout/widget_clock_date", "@drawable/widget_preview_date"),
        )

        expected.forEach { (fileName, resources) ->
            val provider = xml(File(mainDirectory, "res/xml/$fileName")).documentElement
            assertEquals("0", provider.androidAttribute("updatePeriodMillis"))
            assertEquals("horizontal|vertical", provider.androidAttribute("resizeMode"))
            assertEquals(resources.first, provider.androidAttribute("initialLayout"))
            assertEquals(resources.first, provider.androidAttribute("previewLayout"))
            assertEquals(resources.second, provider.androidAttribute("previewImage"))
            val previewName = resources.second.substringAfter("@drawable/")
            assertTrue(File(mainDirectory, "res/drawable-nodpi/$previewName.png").isFile)
        }

        assertEquals(4, expected.values.map { it.second }.toSet().size)
    }

    @Test
    fun widgetLayoutsAreTransparentClickableAndAccessible() {
        val layouts = listOf(
            "widget_clock.xml",
            "widget_clock_vertical.xml",
            "widget_clock_weather.xml",
            "widget_clock_date.xml",
        )

        layouts.forEach { fileName ->
            val document = xml(File(mainDirectory, "res/layout/$fileName"))
            val root = document.documentElement
            assertEquals("@+id/widget_root", root.androidAttribute("id"))
            assertEquals("true", root.androidAttribute("clickable"))
            assertEquals("@string/open_nex_alarm", root.androidAttribute("contentDescription"))
            assertFalse("$fileName must have a transparent root", root.hasAttributeNS(ANDROID_NS, "background"))

            val images = document.getElementsByTagName("ImageView")
            repeat(images.length) { index ->
                val image = images.item(index) as Element
                assertNotNull(image.getAttributeNodeNS(ANDROID_NS, "contentDescription"))
            }
        }
    }

    @Test
    fun horizontalWidgetsExposeSecondaryContentForCompactMode() {
        listOf(
            "widget_clock.xml",
            "widget_clock_weather.xml",
            "widget_clock_date.xml",
        ).forEach { fileName ->
            val document = xml(File(mainDirectory, "res/layout/$fileName"))
            val ids = document.getElementsByTagName("*")
            assertTrue(
                "$fileName must identify details that are hidden at compact width",
                (0 until ids.length).any {
                    (ids.item(it) as Element).androidAttribute("id") == "@+id/widget_details"
                },
            )
        }
    }

    private fun xml(file: File) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(file)

    private fun Element.androidAttribute(name: String): String = getAttributeNS(ANDROID_NS, name)

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
