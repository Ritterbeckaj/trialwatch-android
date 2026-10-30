package io.trialwatch.ui

import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.trialwatch.alarm.AlarmScheduler
import io.trialwatch.alarm.Notify
import io.trialwatch.data.CancelUrls
import io.trialwatch.data.DURATIONS
import io.trialwatch.data.Trial
import io.trialwatch.data.TrialStore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notify.ensureChannel(this)
        askNotifications()
        setContent {
            TrialwatchTheme { App() }
        }
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}

private val LIME = Color(0xFFC8FF4D)
private val AMBER = Color(0xFFFFB648)
private val RED = Color(0xFFFF5A5F)
private val DIM = Color(0xFF8B8B95)
private val CARD = Color(0xFF141416)

@Composable
private fun App() {
    val ctx = LocalContext.current
    var trials by remember { mutableStateOf(TrialStore(ctx).load()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            trials = TrialStore(ctx).load()
        }
    }

    val reload: () -> Unit = {
        trials = TrialStore(ctx).load()
        AlarmScheduler.refreshWidget(ctx)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TRIAL", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("WATCH", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = LIME)
                Spacer(Modifier.weight(1f))
                Text(
                    "${trials.count { !it.done }} active",
                    fontSize = 11.sp,
                    color = DIM
                )
            }

            Spacer(Modifier.height(16.dp))

            DurationButtons()

            Spacer(Modifier.height(18.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = ctx.getSystemService(AlarmManager::class.java)
                if (am != null && !am.canScheduleExactAlarms()) {
                    ExactAlarmBanner()
                    Spacer(Modifier.height(12.dp))
                }
            }

            val list = trials.sortedWith(compareBy({ it.done }, { it.end }))
            if (list.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 30.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No trials tracked.\nTap a duration above the moment you sign up.",
                        color = DIM, fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(list, key = { it.id }) { t ->
                        TrialCard(t) { reload() }
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationButtons() {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DURATIONS.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d ->
                    Button(
                        onClick = {
                            ctx.startActivity(
                                Intent(ctx, QuickAddActivity::class.java)
                                    .putExtra(QuickAddActivity.KEY_DUR, d.key)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1A1F),
                            contentColor = Color(0xFFEDEDF0)
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(d.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text(d.unit, fontSize = 9.sp, color = DIM)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExactAlarmBanner() {
    val ctx = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1A0F))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Exact alarms are off", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AMBER)
            Text(
                "Without this the alarm can slip by up to 10 minutes.",
                fontSize = 12.sp, color = DIM
            )
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = {
                runCatching {
                    ctx.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + ctx.packageName)
                        )
                    )
                }
            }) { Text("Allow exact alarms", color = LIME) }
        }
    }
}

@Composable
private fun TrialCard(t: Trial, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val left = t.end - System.currentTimeMillis()

    val accent = when {
        t.done -> Color(0xFF26262A)
        left <= 86_400_000L -> RED
        left <= 3 * 86_400_000L -> AMBER
        else -> LIME
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CARD)
    ) {
        Row(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(46.dp)
                    .background(accent, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        t.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (t.done) "cancelled" else countdown(left),
                        fontSize = 12.sp,
                        color = accent
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    if (t.done) "was due ${stamp(t.end)}"
                    else "charges ${if (t.price > 0) money(t.price) + " " else ""}on ${stamp(t.end)}",
                    fontSize = 12.sp,
                    color = DIM
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!t.done) {
                        TextButton(onClick = {
                            val url = t.url.ifBlank { CancelUrls.resolve(t.name) }
                            runCatching {
                                ctx.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }) { Text("Cancel page", color = LIME, fontSize = 12.sp) }

                        TextButton(onClick = {
                            TrialStore(ctx).markDone(t.id)
                            AlarmScheduler.cancel(ctx, t)
                            onChanged()
                        }) { Text("Done", color = Color(0xFFEDEDF0), fontSize = 12.sp) }
                    } else {
                        TextButton(onClick = {
                            TrialStore(ctx).markDone(t.id, false)
                            AlarmScheduler.schedule(ctx, t)
                            onChanged()
                        }) { Text("Undo", color = DIM, fontSize = 12.sp) }
                    }

                    Spacer(Modifier.weight(1f))

                    TextButton(onClick = {
                        TrialStore(ctx).remove(t.id)
                        AlarmScheduler.cancel(ctx, t)
                        onChanged()
                    }) { Text("✕", color = DIM, fontSize = 12.sp) }
                }
            }
        }
    }
}

internal fun countdown(ms: Long): String {
    if (ms <= 0) return "due"
    val d = ms / 86_400_000L
    val h = (ms % 86_400_000L) / 3_600_000L
    val m = (ms % 3_600_000L) / 60_000L
    return when {
        d > 0 -> "${d}d ${h}h"
        h > 0 -> "${h}h ${m}m"
        else -> "${m}m"
    }
}

internal fun money(v: Double): String =
    "$" + String.format(Locale.US, "%.2f", v)

internal fun stamp(ms: Long): String =
    SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(Date(ms))
