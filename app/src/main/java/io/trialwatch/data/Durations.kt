package io.trialwatch.data

import java.util.Calendar

/** One entry per button, shared by the widget and the app so they never drift. */
data class Dur(val key: String, val label: String, val unit: String)

val DURATIONS: List<Dur> = listOf(
    Dur("1d", "1", "day"),
    Dur("3d", "3", "days"),
    Dur("7d", "7", "days"),
    Dur("14d", "14", "days"),
    Dur("1m", "1", "month"),
    Dur("3m", "3", "months")
)

fun endForDuration(key: String?, from: Long = System.currentTimeMillis()): Long {
    val day = 86_400_000L
    val cal = Calendar.getInstance().apply { timeInMillis = from }
    return when (key) {
        "1d" -> from + day
        "3d" -> from + 3 * day
        "7d" -> from + 7 * day
        "14d" -> from + 14 * day
        "1m" -> { cal.add(Calendar.MONTH, 1); cal.timeInMillis }
        "3m" -> { cal.add(Calendar.MONTH, 3); cal.timeInMillis }
        else -> from + 7 * day
    }
}
