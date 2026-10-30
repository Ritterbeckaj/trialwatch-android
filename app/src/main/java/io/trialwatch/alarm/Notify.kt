package io.trialwatch.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.trialwatch.R
import io.trialwatch.data.CancelUrls
import io.trialwatch.data.Trial
import io.trialwatch.ui.MainActivity

object Notify {

    const val CHANNEL = "trialwatch.alarm"

    const val KIND_3D = "3d"
    const val KIND_1D = "1d"
    const val KIND_2H = "2h"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) != null) return

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val channel = NotificationChannel(
            CHANNEL,
            "Trial ending",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Fires before a free trial charges your card"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 900)
            enableLights(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        nm.createNotificationChannel(channel)
    }

    fun show(context: Context, trial: Trial, kind: String) {
        ensureChannel(context)

        val money = if (trial.price > 0) {
            "$" + String.format(java.util.Locale.US, "%.2f", trial.price)
        } else {
            "Your card"
        }

        val title = when (kind) {
            KIND_3D -> "3 days left — ${trial.name}"
            KIND_1D -> "Last day — ${trial.name}"
            else -> "2 hours — cancel ${trial.name} now"
        }
        val body = when (kind) {
            KIND_3D -> "$money is taken when the trial ends. Cancel if you are not keeping it."
            KIND_1D -> "$money is charged tomorrow. One tap to cancel."
            else -> "$money is charged in two hours."
        }

        val target = trial.url.ifBlank { CancelUrls.resolve(trial.name) }
        val openCancel = PendingIntent.getActivity(
            context,
            trial.id.hashCode(),
            Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDone = PendingIntent.getBroadcast(
            context,
            trial.id.hashCode() + 7,
            Intent(context, DoneReceiver::class.java).apply {
                action = DoneReceiver.ACTION_DONE
                putExtra(DoneReceiver.EXTRA_ID, trial.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openApp = PendingIntent.getActivity(
            context,
            91,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_trial)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFC8FF4D.toInt())
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setFullScreenIntent(openApp, true)
            .addAction(0, "Open cancel page", openCancel)
            .addAction(0, "I cancelled it", markDone)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(trial.id.hashCode(), notification)
        }
    }
}
