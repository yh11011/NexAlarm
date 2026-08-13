package com.nexalarm.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log

internal object WidgetPreviewPublisher {

    fun publishAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

        val manager = AppWidgetManager.getInstance(context)
        previews.forEach { (providerClass, definition) ->
            runCatching {
                manager.setWidgetPreview(
                    ComponentName(context, providerClass),
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                    BaseClockWidgetProvider.createRemoteViews(
                        context,
                        definition,
                        WidgetLayoutMode.EXPANDED,
                    ),
                )
            }.onFailure { error ->
                Log.w(TAG, "Unable to publish preview for ${providerClass.simpleName}", error)
            }
        }
    }

    private val previews = listOf(
        ClockWidgetProvider::class.java to WidgetDefinition.CLASSIC,
        VerticalClockWidgetProvider::class.java to WidgetDefinition.VERTICAL,
        WeatherClockWidgetProvider::class.java to WidgetDefinition.WEATHER,
        DateClockWidgetProvider::class.java to WidgetDefinition.DATE,
    )

    private const val TAG = "WidgetPreview"
}
