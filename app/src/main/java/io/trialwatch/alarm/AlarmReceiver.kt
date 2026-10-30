package io.trialwatch.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.trialwatch.data.TrialStore

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val kind = intent.getStringExtra(EXTRA_KIND) ?: Notify.KIND_2H

        val trial = TrialStore(context).load().firstOrNull { it.id == id } ?: return
        if (trial.done) return

        Notify.show(context, trial, kind)
        AlarmScheduler.refreshWidget(context)
    }

    companion object {
        const val EXTRA_ID = "trial_id"
        const val EXTRA_KIND = "trial_kind"
    }
}
