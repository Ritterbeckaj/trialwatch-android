package io.trialwatch.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.trialwatch.alarm.AlarmScheduler
import io.trialwatch.alarm.Notify
import io.trialwatch.data.CancelUrls
import io.trialwatch.data.DURATIONS
import io.trialwatch.data.Trial
import io.trialwatch.data.TrialStore
import io.trialwatch.data.endForDuration

/**
 * Opened by the home-screen widget buttons. Translucent, keyboard up,
 * one field. Save schedules the alarms and closes.
 */
class QuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notify.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val key = intent.getStringExtra(KEY_DUR) ?: "7d"
        setContent {
            TrialwatchTheme {
                Sheet(initialKey = key) { finish() }
            }
        }
    }

    companion object {
        const val KEY_DUR = "dur"
    }
}

private val LIME = Color(0xFFC8FF4D)
private val DIM = Color(0xFF8B8B95)
private val FIELD = Color(0xFF0C0C0E)

@Composable
private fun Sheet(initialKey: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var key by remember { mutableStateOf(initialKey) }
    var service by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val end = remember(key) { endForDuration(key) }

    Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF121215),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("New trial", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = service,
                    onValueChange = { service = it },
                    label = { Text("Service") },
                    placeholder = { Text("Netflix, Adobe…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { v -> price = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("First charge") },
                    prefix = { Text("$") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))

                DURATIONS.chunked(3).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { d ->
                            val on = d.key == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (on) LIME else Color(0xFF1A1A1F),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clickable { key = d.key }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "${d.label} ${d.unit}",
                                        fontSize = 12.sp,
                                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                        color = if (on) Color(0xFF0B0B0C) else Color(0xFFEDEDF0)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                val resolved = remember(service) {
                    if (service.isBlank()) "" else CancelUrls.resolve(service)
                }
                Text(
                    "Charges ${stamp(end)}",
                    fontSize = 12.sp,
                    color = DIM
                )
                if (resolved.isNotBlank() && !resolved.startsWith("https://www.google.com/search")) {
                    Text("Cancel page: ${hostOf(resolved)}", fontSize = 12.sp, color = LIME)
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = DIM)
                    }
                    Button(
                        onClick = {
                            val name = service.trim()
                            val trial = Trial(
                                name = name,
                                url = CancelUrls.resolve(name),
                                price = price.toDoubleOrNull() ?: 0.0,
                                end = end
                            )
                            val store = TrialStore(ctx)
                            store.add(trial)
                            AlarmScheduler.schedule(ctx, trial)
                            AlarmScheduler.refreshWidget(ctx)
                            onClose()
                        },
                        enabled = service.isNotBlank(),
                        modifier = Modifier.weight(2f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LIME,
                            contentColor = Color(0xFF0B0B0C),
                            disabledContainerColor = Color(0xFF26262A),
                            disabledContentColor = DIM
                        )
                    ) {
                        Text("Arm alarm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun hostOf(url: String): String =
    runCatching { java.net.URI(url).host ?: url }.getOrDefault(url)
