package cn.armpal.rehab.windows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.armpal.rehab.audio.SpatialVoiceGuide
import cn.armpal.rehab.data.CardExporter
import cn.armpal.rehab.data.DailyCardData
import cn.armpal.rehab.data.SettingsStore
import cn.armpal.rehab.data.TrainingRepository
import cn.armpal.rehab.data.WeekAggregator
import cn.armpal.rehab.session.TrainingSessionController
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Text
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 报告窗:今日实时结果 + 持久化历史(Room:今日记录合计 / 周报 / streak)+ 合规免责声明。
 * 数据在本机处理,不上传云端(PRD §5)。
 */
@Composable
fun ReportWindow() {
    val state by TrainingSessionController.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recent by TrainingRepository.recent.collectAsState()

    var voiceOn by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        voiceOn = SettingsStore.voiceEnabled(context).first()
        SpatialVoiceGuide.enabled = voiceOn
    }

    var weekly by remember { mutableStateOf<WeekAggregator.WeeklyReport?>(null) }
    var streak by remember { mutableStateOf(0) }
    LaunchedEffect(recent) {
        weekly = WeekAggregator.aggregate(
            TrainingRepository.sessionsSince(WeekAggregator.weekStartDate().toString()),
        )
        streak = SettingsStore.streak(context)
    }

    val today = LocalDate.now().toString()
    val todaySessions = remember(recent) { recent.filter { it.date == today } }
    val todayPersistedReps = todaySessions.filter { !it.demoMode }.sumOf { it.reps }

    RehabPanel(title = "今日报告") {
        // —— 本场实时 ——
        Text(text = "完成次数", fontSize = 22.sp, color = TextSecondary)
        Text(
            text = "${state.totalReps} / ${state.targetReps * state.totalSets} 次",
            fontSize = 54.sp,
            color = if (state.sessionCompleted) AccentGreen else TextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "最高抬举 ${state.peakDeg.toInt()}°  ·  目标 ${state.targetDeg.toInt()}°",
            fontSize = 22.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "组数 ${(state.setIndex + 1).coerceAtMost(state.totalSets)} / ${state.totalSets}",
            fontSize = 20.sp,
            color = TextSecondary,
        )
        if (state.sessionAvgQuality >= 0f) {
            Text(
                text = "动作质量 ${(state.sessionAvgQuality * 100).toInt()} 分",
                fontSize = 20.sp,
                color = AccentBlue,
            )
        }
        if (state.restingSecLeft > 0) {
            Text(
                text = "休息 ${state.restingSecLeft}s,下一组马上开始",
                fontSize = 20.sp,
                color = AccentAmber,
            )
        }

        Spacer(Modifier.height(16.dp))

        // —— 持久化历史(Room)——
        Text(
            text = "本周:共 ${weekly?.weekReps ?: 0} 次 · 活跃 ${weekly?.activeDays ?: 0} 天 · 连续 $streak 天",
            fontSize = 20.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        val demoCount = todaySessions.count { it.demoMode }
        val realCount = todaySessions.size - demoCount
        Text(
            text = when {
                todaySessions.isEmpty() -> "今天还没有训练记录"
                realCount == 0 -> "今天有 ${demoCount} 场演示训练(不计入统计)"
                demoCount > 0 -> "今天已记录 $todayPersistedReps 次(${realCount} 场,另有 ${demoCount} 场演示)"
                else -> "今天已记录 $todayPersistedReps 次(${realCount} 场训练)"
            },
            fontSize = 18.sp,
            color = TextSecondary,
        )

        Spacer(Modifier.height(16.dp))
        // 语音提示开关(适老:声音是主反馈通道,可关)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    voiceOn = !voiceOn
                    SpatialVoiceGuide.enabled = voiceOn
                    scope.launch { SettingsStore.setVoiceEnabled(context, voiceOn) }
                },
            ) {
                Text(text = if (voiceOn) "语音提示:开" else "语音提示:关", fontSize = 18.sp)
            }
            // 家属卡片:导出到相册 + 系统分享面板(微信等),用户主动触发
            Button(
                onClick = {
                    scope.launch {
                        val weekly = WeekAggregator.aggregate(
                            TrainingRepository.sessionsSince(
                                WeekAggregator.weekStartDate().toString(),
                            ),
                        )
                        val todayReps = recent
                            .filter { it.date == LocalDate.now().toString() && !it.demoMode }
                            .sumOf { it.reps } + if (state.demoMode) 0 else state.totalReps
                        val card = DailyCardData(
                            totalReps = todayReps,
                            maxElevationDeg = state.peakDeg.toInt()
                                .coerceAtLeast(weekly.maxElevationDeg.toInt()),
                            streakDays = weekly.streakDays,
                            activeDaysThisWeek = weekly.activeDays,
                            demoSessions = recent.count {
                                it.date == LocalDate.now().toString() && it.demoMode
                            },
                        )
                        CardExporter.export(context, card)?.let { uri ->
                            CardExporter.share(context, uri)
                        }
                    }
                },
            ) {
                Text(text = "生成家属卡片", fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "训练数据在本机处理,不上传云端",
            fontSize = 17.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "本产品不替代专业医疗建议,急性期或疼痛请遵医嘱",
            fontSize = 16.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "家属分享卡片将在后续版本上线",
            fontSize = 15.sp,
            color = TextSecondary,
        )
    }
}
