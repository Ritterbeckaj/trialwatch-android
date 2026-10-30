package io.trialwatch.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import io.trialwatch.data.TrialStore

class DoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val store = TrialStore(context)
        val trial = store.load().firstOrNull { it.id == id } ?: return

        when (intent.action) {
            ACTION_SNOOZE -> {
                val moved = trial.copy(end = trial.end + 86_400_000L)
                store.save(store.load().map { if (it.id == id) moved else it })
                AlarmScheduler.schedule(context, moved)
            }

            else -> {
                store.markDone(id)
                AlarmScheduler.cancel(context, trial)
                runCatching {
                    NotificationManagerCompat.from(context).cancel(id.hashCode())
                }
            }
        }

        AlarmScheduler.refreshWidget(context)
    }

    companion object {
        const val ACTION_DONE = "io.trialwatch.DONE"
        const val ACTION_SNOOZE = "io.trialwatch.SNOOZE"
        const val EXTRA_ID = "trial_id"
    }
}
