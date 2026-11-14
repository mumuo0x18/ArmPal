package cn.armpal.rehab.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.armpal.rehab.session.TrainingSessionController
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private const val UPPER_LEN = 0.24f
private const val FORE_LEN = 0.22f

/** 视口内整体缩放(调优参数:坐标系量级待实测,先 0.6 防出界)。 */
private const val K = 0.15f

/**
 * 3D 示范者(PRD:真人比例示范):
 * - MeshResource.createBox 单位立方 × 6 段拼装的几何体人形(头/躯干/双侧上臂+前臂),坐姿比例
 * - 纯运动学驱动:每帧由抬举角 e 计算 段方向 (sin e, -cos e) 与各段中心,直接写 TransformComponent
 *   ——与腕点公式、侧窗仪表完全同源(真实/演示模式一致,诚实原则:不做假数据)
 * - 官方范式:实体必须经 SpatialView content.addEntity() 才渲染(Stage 内容层不渲染裸实体,
 *   实测踩坑);面板放屏幕底部可见区(训练主窗下方)
 * - 升级路径:Tripo 生成的人形模型(Entity.load/AssetBundle)换皮,驱动逻辑不变
 * - 装配失败静默禁用(语音/文案/仪表仍是完整反馈通道)
 */
@Composable
fun DemoAvatarInline(
    width: androidx.compose.ui.unit.Dp = 460.dp,
    height: androidx.compose.ui.unit.Dp = 330.dp,
) {
    com.pico.spatial.ui.foundation.content.SpatialView(
        modifier = Modifier
            .width(width)
            .height(height),
    ) { content, _ ->
        val rig = runCatching { AvatarParts.load(Vector3(0f, -0.06f, 0f)) { content.addEntity(it) } }
            .onFailure { android.util.Log.w("DemoAvatar", "load failed, avatar disabled", it) }
            .getOrNull() ?: return@SpatialView

        try {
            var eR = 5f
            var eL = 5f
            while (true) {
                val state = TrainingSessionController.current
                val targetR = state.elevationR ?: 5f
                val targetL = state.elevationL ?: 5f
                eR += (targetR - eR) * 0.12f
                eL += (targetL - eL) * 0.12f
                rig.setArms(eR, eL)
                delay(33)
            }
        } finally {
            rig.destroy()
        }
    }
}

/** 单臂两个段的实体(上臂/前臂),位置+姿态逐帧重算。 */
private class ArmSegments(
    val upper: Entity,
    val fore: Entity,
    val shoulder: Vector3,
    val sideSign: Float,
)

/** 全部实体 + 每帧运动学更新。 */
private class AvatarParts(
    val entities: List<Entity>,
    private val armR: ArmSegments,
    private val armL: ArmSegments,
) {
    fun setArms(eRightDeg: Float, eLeftDeg: Float) {
        updateArm(armR, eRightDeg)
        updateArm(armL, eLeftDeg)
    }

    private fun updateArm(arm: ArmSegments, elevationDeg: Float) {
        val rad = Math.toRadians(elevationDeg.toDouble()).toFloat()
        // 段方向:0°=垂下 (0,-1),90°=侧平举 (±1,0),180°=过头 (0,1)
        val dx = arm.sideSign * sin(rad)
        val dy = -cos(rad)
        val s = arm.shoulder
        setTransform(
            arm.upper,
            Vector3(s.x + dx * UPPER_LEN * K / 2f, s.y + dy * UPPER_LEN * K / 2f, s.z),
            quatZ(rad * arm.sideSign),
            Vector3(0.08f * K, UPPER_LEN * K, 0.08f * K),
        )
        setTransform(
            arm.fore,
            Vector3(s.x + dx * (UPPER_LEN * K + FORE_LEN * K / 2f), s.y + dy * (UPPER_LEN * K + FORE_LEN * K / 2f), s.z),
            quatZ(rad * arm.sideSign),
            Vector3(0.07f * K, FORE_LEN * K, 0.07f * K),
        )
    }

    /** 绕 Z 轴旋转(sideSign 已并入角度符号)。 */
    private fun quatZ(rad: Float): Quat = Quat(0f, 0f, sin(rad / 2f), cos(rad / 2f))

    private fun setTransform(e: Entity, position: Vector3, q: Quat, scale: Vector3) {
        runCatching {
            e.components[TransformComponent::class.java] =
                TransformComponent(position, q, scale)
        }.onFailure { android.util.Log.w("DemoAvatar", "transform set failed", it) }
    }

    fun destroy() {
        entities.forEach { runCatching { it.destroy(recursively = true) } }
    }

    companion object {
        /**
         * 装配:origin = hip 中心(米,相对面板中心);坐姿比例。
         * 单位立方网格共享一份,段尺寸由 TransformComponent.scaleVector 控制。
         */
        fun load(origin: Vector3, register: (Entity) -> Unit): AvatarParts {
            val mesh = MeshResource.createBox(Vector3(1f, 1f, 1f), 0f)
            val material = UnlitMaterial.Companion.create(BlendingMode.OPAQUE)

            fun part(position: Vector3, scale: Vector3): Entity {
                val e = Entity()
                e.components[ModelComponent::class.java] = ModelComponent(mesh, material)
                e.components[TransformComponent::class.java] =
                    TransformComponent(
                        Vector3(
                            origin.x + position.x * K,
                            origin.y + position.y * K,
                            origin.z + position.z * K,
                        ),
                        EulerAngles(0f, 0f, 0f),
                        scale * K,
                    )
                register(e)
                return e
            }

            // 诊断立方:视图原点处一个明显可见的小块(若它都不渲染,问题在注册而非摆放)
            part(Vector3(0f, 0.9f, 0f), Vector3(0.18f, 0.18f, 0.18f))

            val torso = part(Vector3(0f, 0.22f, 0f), Vector3(0.30f, 0.44f, 0.16f))
            val head = part(Vector3(0f, 0.56f, 0f), Vector3(0.20f, 0.20f, 0.20f))

            fun arm(sideSign: Float): ArmSegments {
                val shoulder = Vector3(sideSign * 0.19f * K, 0.40f * K, 0f)
                val upper = part(
                    Vector3(
                        shoulder.x + sideSign * sin(0.1f) * UPPER_LEN * K / 2f,
                        shoulder.y - cos(0.1f) * UPPER_LEN * K / 2f,
                        shoulder.z,
                    ),
                    Vector3(0.08f * K, UPPER_LEN * K, 0.08f * K),
                )
                val fore = part(
                    Vector3(
                        shoulder.x + sideSign * sin(0.1f) * (UPPER_LEN * K + FORE_LEN * K / 2f),
                        shoulder.y - cos(0.1f) * (UPPER_LEN * K + FORE_LEN * K / 2f),
                        shoulder.z,
                    ),
                    Vector3(0.07f * K, FORE_LEN * K, 0.07f * K),
                )
                return ArmSegments(upper, fore, shoulder, sideSign)
            }

            val armR = arm(1f)
            val armL = arm(-1f)
            return AvatarParts(listOf(torso, head, armR.upper, armR.fore, armL.upper, armL.fore), armR, armL)
        }
    }
}
