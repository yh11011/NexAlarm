package com.nexalarm.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import com.nexalarm.app.MainActivity
import com.nexalarm.app.R

enum class WidgetDefinition(
    @param:LayoutRes val layoutResource: Int,
    val usesVerticalTime: Boolean,
    val hasTemperature: Boolean,
) {
    CLASSIC(R.layout.widget_clock, usesVerticalTime = false, hasTemperature = true),
    VERTICAL(R.layout.widget_clock_vertical, usesVerticalTime = true, hasTemperature = true),
    WEATHER(R.layout.widget_clock_weather, usesVerticalTime = false, hasTemperature = true),
    DATE(R.layout.widget_clock_date, usesVerticalTime = false, hasTemperature = false),
}

abstract class BaseClockWidgetProvider(
    private val definition: WidgetDefinition,
) : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        WidgetPreviewPublisher.publishAll(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId, definition)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId, definition, newOptions)
    }

    companion object {
        internal fun createRemoteViews(
            context: Context,
            definition: WidgetDefinition,
            layoutMode: WidgetLayoutMode,
            appWidgetId: Int? = null,
        ): RemoteViews = RemoteViews(context.packageName, definition.layoutResource).also { views ->
            if (definition.usesVerticalTime) {
                val textSize = if (layoutMode == WidgetLayoutMode.EXPANDED) 64f else 48f
                views.setTextViewTextSize(
                    R.id.widget_text_clock_hour,
                    TypedValue.COMPLEX_UNIT_SP,
                    textSize,
                )
                views.setTextViewTextSize(
                    R.id.widget_text_clock_minute,
                    TypedValue.COMPLEX_UNIT_SP,
                    textSize,
                )
            } else {
                views.setTextViewTextSize(
                    R.id.widget_text_clock,
                    TypedValue.COMPLEX_UNIT_SP,
                    layoutMode.timeTextSizeSp,
                )
                views.setViewVisibility(
                    R.id.widget_details,
                    if (layoutMode == WidgetLayoutMode.EXPANDED) View.VISIBLE else View.GONE,
                )
            }

            if (definition.hasTemperature) {
                views.setViewVisibility(
                    R.id.widget_weather_temperature,
                    if (layoutMode.showsTemperature) View.VISIBLE else View.GONE,
                )
            }

            if (appWidgetId != null) {
                val openApp = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    openApp,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            definition: WidgetDefinition,
            options: Bundle = appWidgetManager.getAppWidgetOptions(appWidgetId),
        ) {
            val layoutMode = WidgetLayoutMode.from(
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
            )
            val views = createRemoteViews(context, definition, layoutMode, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class VerticalClockWidgetProvider : BaseClockWidgetProvider(WidgetDefinition.VERTICAL)

class WeatherClockWidgetProvider : BaseClockWidgetProvider(WidgetDefinition.WEATHER)

class DateClockWidgetProvider : BaseClockWidgetProvider(WidgetDefinition.DATE)
