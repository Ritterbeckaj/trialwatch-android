package io.trialwatch.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.trialwatch.data.Trial
import androidx.glance.appwidget.updateAll
import io.trialwatch.data.TrialStore
import io.trialwatch.widget.TrialWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AlarmScheduler {

    private const val DAY = 86_400_000L

    private val PLAN = listOf(
        -3 * DAY to Notify.KIND_3D,
        -1 * DAY to Notify.KIND_1D,
        -2 * 3_600_000L to Notify.KIND_2H
    )

    /**
     * Three alarms per trial. The final one is set with setAlarmClock, which is
     * the highest-priority wakeup Android exposes: it survives Doze, shows in the
     * status bar, and rings through Do Not Disturb. That is the "high priority"
     * part; everything else here is bookkeeping.
     */
    fun schedule(context: Context, trial: Trial) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

        for ((offset, kind) in PLAN) {
            val at = trial.end + offset
            if (at <= now) continue
            val pi = pending(context, trial.id, offset, kind)

            when {
                kind == Notify.KIND_2H && exact ->
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent(context, trial)), pi)

                exact ->
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)

                // No exact-alarm permission: land inside a 10 minute window instead
                // of silently never firing.
                else ->
                    am.setWindow(AlarmManager.RTC_WAKEUP, at, 10 * 60_000L, pi)
            }
        }
    }

    fun cancel(context: Context, trial: Trial) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        for ((offset, kind) in PLAN) {
            am.cancel(pending(context, trial.id, offset, kind))
        }
    }

    fun rescheduleAll(context: Context) {
        TrialStore(context).active().forEach { schedule(context, it) }
    }

    fun refreshWidget(context: Context) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { TrialWidget().updateAll(app) }
        }
    }

    private fun pending(context: Context, id: String, offset: Long, kind: String): PendingIntent {
        val i = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ID, id)
            putExtra(AlarmReceiver.EXTRA_KIND, kind)
        }
        val code = id.hashCode() * 31 + offset.toInt()
        return PendingIntent.getBroadcast(
            context,
            code,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showIntent(context: Context, trial: Trial): PendingIntent {
        val i = Intent(context, io.trialwatch.ui.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            trial.id.hashCode() + 13,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
