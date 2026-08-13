package com.nexalarm.app.widget

internal enum class WidgetLayoutMode(
    val timeTextSizeSp: Float,
    val showsTemperature: Boolean
) {
    COMPACT(timeTextSizeSp = 44f, showsTemperature = false),
    EXPANDED(timeTextSizeSp = 58f, showsTemperature = true);

    companion object {
        fun from(minWidthDp: Int, minHeightDp: Int): WidgetLayoutMode =
            if (minWidthDp >= EXPANDED_MIN_WIDTH_DP && minHeightDp >= EXPANDED_MIN_HEIGHT_DP) {
                EXPANDED
            } else {
                COMPACT
            }

        private const val EXPANDED_MIN_WIDTH_DP = 180
        private const val EXPANDED_MIN_HEIGHT_DP = 72
    }
}
