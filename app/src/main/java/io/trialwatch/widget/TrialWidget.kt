package io.trialwatch.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.trialwatch.data.DURATIONS
import io.trialwatch.data.Dur
import io.trialwatch.data.Trial
import io.trialwatch.data.TrialStore
import io.trialwatch.ui.QuickAddActivity
import io.trialwatch.ui.countdown

private val BG = ColorProvider(Color(0xFF0B0B0C))
private val CELL = ColorProvider(Color(0xFF1A1A1F))
private val INK = ColorProvider(Color(0xFFEDEDF0))
private val DIM = ColorProvider(Color(0xFF8B8B95))
private val LIME = ColorProvider(Color(0xFFC8FF4D))
private val AMBER = ColorProvider(Color(0xFFFFB648))

class TrialWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val trials = TrialStore(context).active()
        provideContent { Body(trials) }
    }

    @Composable
    private fun Body(trials: List<Trial>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(22.dp)
                .background(BG)
                .padding(12.dp)
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    "TRIALWATCH",
                    style = TextStyle(color = LIME, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    if (trials.isEmpty()) "idle" else "${trials.size} active",
                    style = TextStyle(color = DIM, fontSize = 10.sp)
                )
            }

            Spacer(GlanceModifier.height(8.dp))

            DURATIONS.chunked(3).forEachIndexed { i, row ->
                if (i > 0) Spacer(GlanceModifier.height(6.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    row.forEachIndexed { j, d ->
                        if (j > 0) Spacer(GlanceModifier.width(6.dp))
                        Cell(d)
                    }
                }
            }

            if (trials.isNotEmpty()) {
                Spacer(GlanceModifier.height(10.dp))
                trials.take(2).forEach { MiniRow(it) }
            }
        }
    }

    @Composable
    private fun Cell(d: Dur) {
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .height(44.dp)
                .cornerRadius(11.dp)
                .background(CELL)
                .clickable(
                    actionStartActivity<QuickAddActivity>(
                        actionParametersOf(QuickAddActivity.KEY_DUR to d.key)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    d.label,
                    style = TextStyle(color = INK, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    d.unit,
                    style = TextStyle(color = DIM, fontSize = 8.sp, textAlign = TextAlign.Center)
                )
            }
        }
    }

    @Composable
    private fun MiniRow(t: Trial) {
        Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp)) {
            Text(
                t.name,
                maxLines = 1,
                style = TextStyle(color = INK, fontSize = 11.sp)
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                countdown(t.end - System.currentTimeMillis()),
                style = TextStyle(color = AMBER, fontSize = 11.sp)
            )
        }
    }
}
