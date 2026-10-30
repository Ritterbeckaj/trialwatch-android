package io.trialwatch

import android.app.Application
import io.trialwatch.alarm.AlarmScheduler
import io.trialwatch.alarm.Notify

class TrialWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notify.ensureChannel(this)
        // Timers are cleared by the OS on reboot — rebuild them from storage.
        AlarmScheduler.rescheduleAll(this)
    }
}
