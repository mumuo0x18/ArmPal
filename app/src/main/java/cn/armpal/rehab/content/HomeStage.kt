package cn.armpal.rehab.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cn.armpal.rehab.analysis.ExerciseTemplate
import cn.armpal.rehab.audio.SpatialVoiceGuide
import cn.armpal.rehab.anchor.AnchorManager
import cn.armpal.rehab.data.SettingsStore
import cn.armpal.rehab.data.TrainingRepository
import cn.armpal.rehab.data.toEntity
import cn.armpal.rehab.session.FeedbackLevel
import cn.armpal.rehab.session.TrainingSessionController
import cn.armpal.rehab.tracking.rememberTrackingSource
import cn.armpal.rehab.windows.DisclaimerStore
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 落库/设置写入用的 IO 作用域(sink 回调来自控制器 tick 线程,非 suspend 上下文)。 */
private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Stage(Full Space)内容:
 * - 追踪运行时(手部 + HMD)在这里启动;追踪能力仅 Full Space 可用
 * - 首次启动免责声明 gate(PRD §5):状态在 DisclaimerStore,训练主窗负责渲染,这里负责开窗策略
 * - 加载动作模板 → 绑定训练会话 → 三窗订阅同一份 SessionState
 * - 会话结束经 sessionSink 落库(TrainingRepository,Room)
 * - 三个 WindowContainer 在 mainApp 中声明,这里按顺序显式 open
 *   (6.2 行为:声明 ≠ 打开,参照官方 WelcomeSpace;顺序打开才能用 placement 相对摆放)
 */
@Composable
fun HomeStage() {
    val navigator = LocalSpatialNavigator.current
    val context = LocalContext.current

    // 免责声明:首次启动必须确认(PRD §5)。状态放 DisclaimerStore(训练窗渲染 gate,这里决定开窗策略)
    LaunchedEffect(Unit) {
        DisclaimerStore.accepted = SettingsStore.disclaimerAccepted(context).first()
        // 会话落库钩子(data 层实现;演示数据不计 streak)
        TrainingSessionController.sessionSink = { record ->
            TrainingRepository.record(record.toEntity())
            if (!record.demoMode && record.reps > 0) {
                ioScope.launch {
                    val today = java.time.LocalDate.now()
                    SettingsStore.bumpStreak(
                        context,
                        today.toString(),
                        today.minusDays(1).toString(),
                    )
                }
            }
        }
        // 语音引导 + 训练位锚点找回(均带降级,不阻断主流程)
        SpatialVoiceGuide.init(context)
        SpatialVoiceGuide.enabled = SettingsStore.voiceEnabled(context).first()
        AnchorManager.restore(context)
    }

    // 语音触发:完成提示(逐条)+ 反馈等级变化(限频;演示模式同样出声,路演需要)
    LaunchedEffect(Unit) {
        TrainingSessionController.state.collect { s ->
            if (s.lastMessage.isNotEmpty()) {
                SpatialVoiceGuide.speak(s.lastMessage, throttleMs = 2500L)
            }
            when (s.feedback) {
                FeedbackLevel.TOO_LOW -> SpatialVoiceGuide.speak("抬得有点低,再抬高一点", throttleMs = 8000L)
                FeedbackLevel.PARTIAL -> SpatialVoiceGuide.speak("快到了,再抬高一点", throttleMs = 8000L)
                else -> Unit
            }
        }
    }




    var exercise by remember { mutableStateOf<ExerciseTemplate?>(null) }
    LaunchedEffect(Unit) {
        val all = ExerciseRepository.load(context)
        ExerciseSelection.all = all
        val savedId = SettingsStore.selectedExerciseId(context).first()
        ExerciseSelection.currentId = savedId?.takeIf { id -> all.any { it.id == id } }
        exercise = ExerciseSelection.current
    }

    // 追踪数据源(含权限巡检/授权后重启/无真机自动演示兜底)
    val source = rememberTrackingSource()

    // 用户切换动作:重新绑定会话(rebind 幂等)并持久化选择
    LaunchedEffect(ExerciseSelection.currentId, DisclaimerStore.accepted) {
        val template = ExerciseSelection.current ?: return@LaunchedEffect
        if (DisclaimerStore.accepted != true) return@LaunchedEffect
        if (exercise?.id != template.id) {
            exercise = template
            SettingsStore.setSelectedExerciseId(context, template.id)
            TrainingSessionController.bind(source, template)
        }
    }

    // 绑定训练会话:免责声明确认后才开始(避免确认前演示源已计数/落库)
    LaunchedEffect(source, exercise, DisclaimerStore.accepted) {
        if (DisclaimerStore.accepted != true) return@LaunchedEffect
        val template = exercise ?: return@LaunchedEffect
        TrainingSessionController.bind(source, template)
    }

    DisposableEffect(Unit) {
        onDispose { TrainingSessionController.unbind() }
    }

    // 开窗策略由免责声明确认状态驱动:
    // - 加载中(null):不开窗
    // - 待确认(false):只开训练主窗(渲染使用须知)
    // - 已确认(true):训练主窗保持,再开侧窗/报告窗(顺序打开才能用 placement 相对摆放)
    //   trainingOpened 防止重复 open 产生第二个同名窗口
    var trainingOpened by remember { mutableStateOf(false) }
    LaunchedEffect(DisclaimerStore.accepted) {
        when (DisclaimerStore.accepted) {
            null -> return@LaunchedEffect
            false -> {
                navigator.openWindowContainer("training")
                trainingOpened = true
            }

            true -> {
                if (!trainingOpened) {
                    navigator.openWindowContainer("training")
                    trainingOpened = true
                    delay(600)
                }
                navigator.openWindowContainer("correct")
                delay(600)
                navigator.openWindowContainer("report")
            }
        }
    }
}
