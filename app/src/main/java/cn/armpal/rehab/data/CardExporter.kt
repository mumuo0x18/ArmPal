package cn.armpal.rehab.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import java.time.LocalDate

/** 家属卡片数据(报告窗汇总)。 */
data class DailyCardData(
    val date: String = LocalDate.now().toString(),
    val totalReps: Int,
    val maxElevationDeg: Int,
    val streakDays: Int,
    val activeDaysThisWeek: Int,
    val demoSessions: Int = 0,
)

/**
 * 家属分享卡片导出(PRD P1):
 * - android.graphics 原生绘制(1080×1440),不依赖 Compose 离屏窗口
 * - 写入 MediaStore Pictures;分享走系统分享面板(覆盖微信)
 * - 合规:卡片含免责声明;数据仅在本机,导出是用户主动行为
 */
object CardExporter {

    private const val W = 1080
    private const val H = 1440

    fun export(context: Context, data: DailyCardData): Uri? = runCatching {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawCard(canvas, data)

        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "armpal_card_${data.date}.jpg")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ArmPal")
        }
        val uri = context.contentResolver.insert(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ) ?: return@runCatching null
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bmp.recycle()
        uri
    }.getOrNull()

    /** 系统分享面板(用户选择微信/相册等)。 */
    fun share(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "我家人的今日空间康复训练打卡 ${LocalDate.now()} #臂伴ArmPal",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享训练卡片"))
    }

    private fun drawCard(canvas: Canvas, d: DailyCardData) {
        // 背景渐变(品牌深色)
        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, W.toFloat(), H.toFloat(),
                Color.rgb(20, 23, 29), Color.rgb(35, 48, 70),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)

        val title = Paint().apply {
            color = Color.WHITE
            textSize = 64f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("臂伴 · 今日训练报告", 80f, 170f, title)
        val date = Paint(title).apply { textSize = 36f; color = Color.rgb(160, 170, 190) }
        canvas.drawText(d.date, 82f, 230f, date)

        fun metric(label: String, value: String, y: Float) {
            val lp = Paint().apply {
                color = Color.rgb(160, 170, 190); textSize = 40f; isAntiAlias = true
            }
            val vp = Paint().apply {
                color = Color.rgb(74, 222, 128); textSize = 110f
                typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
            }
            canvas.drawText(label, 90f, y, lp)
            canvas.drawText(value, 90f, y + 130f, vp)
        }

        metric("完成次数", "${d.totalReps} 次", 360f)
        metric("最高抬举", "${d.maxElevationDeg}°", 650f)
        metric("连续打卡", "${d.streakDays} 天", 940f)

        val foot = Paint().apply {
            color = Color.rgb(150, 160, 180); textSize = 30f; isAntiAlias = true
        }
        canvas.drawText("本周活跃 ${d.activeDaysThisWeek} 天 · 数据仅存本机", 90f, 1190f, foot)
        canvas.drawText("本产品不替代专业医疗建议,急性期或疼痛请遵医嘱", 90f, 1250f, foot)
        canvas.drawText("臂伴 ArmPal · PICO 空间康复陪伴", 90f, 1330f, foot)
        // 底部装饰条
        val bar = Paint().apply { color = Color.rgb(74, 222, 128) }
        canvas.drawRoundRect(RectF(80f, 1360f, 340f, 1372f), 6f, 6f, bar)
    }
}
