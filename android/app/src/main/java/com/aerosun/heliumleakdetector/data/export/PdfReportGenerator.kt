package com.aerosun.heliumleakdetector.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 氦检漏检测报告 PDF 生成器。
 *
 * 使用 Android 内置 PdfDocument API，零外部依赖，离线可用。
 * 输出标准 A4 PDF，支持系统分享。
 */
object PdfReportGenerator {

    // A4 尺寸 (points, 72 dpi)
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val LINE_HEIGHT = 18f
    private const val SMALL_LINE = 14f

    // 颜色
    private const val COLOR_TITLE = 0xFF1565C0.toInt()     // 工业蓝
    private const val COLOR_HEADER = 0xFF1A1C1E.toInt()
    private const val COLOR_BODY = 0xFF333333.toInt()
    private const val COLOR_LIGHT = 0xFF666666.toInt()
    private const val COLOR_PASS = 0xFF2E7D32.toInt()
    private const val COLOR_FAIL = 0xFFC62828.toInt()
    private const val COLOR_LINE = 0xFFD0D0D0.toInt()
    private const val COLOR_BG_HEADER = 0xFFEEF2F7.toInt()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 生成 PDF 报告并返回分享 Intent。
     */
    fun exportPdf(context: Context, record: DetectionRecordEntity): Intent {
        val file = createPdfFile(context, record)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * 生成 PDF 文件并返回文件路径。
     */
    fun generatePdf(context: Context, record: DetectionRecordEntity): File {
        return createPdfFile(context, record)
    }

    // ====== 内部实现 ======

    private fun createPdfFile(context: Context, record: DetectionRecordEntity): File {
        val safeName = record.reportNo.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val timestamp = dateFormat.format(Date()).replace(":", "-").replace(" ", "_")
        val file = File(context.cacheDir, "report_${safeName}_$timestamp.pdf")
        file.outputStream().use { out ->
            val doc = PdfDocument()
            writePages(doc, record)
            doc.writeTo(out)
            doc.close()
        }
        return file
    }

    private fun writePages(doc: PdfDocument, r: DetectionRecordEntity) {
        var y = MARGIN
        var page = doc.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
        )
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = COLOR_TITLE; textSize = 22f
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = COLOR_HEADER; textSize = 14f
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = COLOR_BODY; textSize = 11f; isAntiAlias = true
        }
        val smallPaint = Paint().apply {
            color = COLOR_LIGHT; textSize = 9f; isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = COLOR_LINE; strokeWidth = 1f; isAntiAlias = true
        }
        val bgPaint = Paint().apply {
            color = COLOR_BG_HEADER; style = Paint.Style.FILL; isAntiAlias = true
        }
        val passPaint = Paint().apply {
            color = COLOR_PASS; textSize = 18f
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
        }
        val failPaint = Paint().apply {
            color = COLOR_FAIL; textSize = 18f
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
        }

        fun newPage() {
            doc.finishPage(page)
            page = doc.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
            )
            canvas = page.canvas
            y = MARGIN
        }

        fun checkSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN) newPage()
        }

        fun drawText(text: String, x: Float, paint: Paint): Float {
            canvas.drawText(text, x, y, paint)
            y += paint.textSize + 4f
            return y
        }

        fun drawLine() {
            y += 4f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 6f
        }

        fun drawHeaderBg(h: Float) {
            canvas.drawRect(MARGIN, y - 2f, PAGE_WIDTH - MARGIN, y + h, bgPaint)
        }

        // ==================== 标题 ====================
        y = MARGIN
        drawText("氦检漏试验检测报告", MARGIN, titlePaint)
        y += 2f
        drawText("Helium Leak Test Report", MARGIN, Paint(smallPaint).apply { textSize = 10f })
        drawLine()
        y += 4f

        // ==================== 判定标签 ====================
        val verdict = if (r.isAcceptable) "✓ 合格 PASS" else "✗ 不合格 FAIL"
        val vPaint = if (r.isAcceptable) passPaint else failPaint
        val vw = vPaint.measureText(verdict)
        checkSpace(40f)
        canvas.drawText(verdict, (PAGE_WIDTH - vw) / 2, y, vPaint)
        y += 20f
        canvas.drawText(
            "实测漏率: ${"%.4e".format(r.qMeasured)} Pa·m³/s",
            (PAGE_WIDTH - smallPaint.measureText("实测漏率: ${"%.4e".format(r.qMeasured)} Pa·m³/s")) / 2, y, smallPaint
        )
        y += 14f
        canvas.drawText(
            "验收标准: < ${"%.1e".format(r.acceptanceLimitValue())} Pa·m³/s",
            (PAGE_WIDTH - smallPaint.measureText("验收标准: < ${"%.1e".format(r.acceptanceLimitValue())} Pa·m³/s")) / 2, y, smallPaint
        )
        y += 20f
        drawLine()

        // ==================== 1. 项目信息 ====================
        checkSpace(30f); drawHeaderBg(18f)
        drawText("一、项目信息", MARGIN, headerPaint)
        y += 2f
        projectRow(canvas, y, "报告编号", r.reportNo, bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "合同号", r.contractNo, bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "试验日期", r.testDate, bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "产品代码", r.productCode, bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "产品名称", r.productName, bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "焊缝名称", r.weldName, bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "检测区域", r.inspectionArea, bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "试验方法", r.testMethod, bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "环境条件", "${r.temperature} °C / ${r.humidity}%", bodyPaint, smallPaint)
        y += 6f
        drawLine()

        // ==================== 2. 标准漏孔参数 ====================
        checkSpace(30f); drawHeaderBg(18f)
        drawText("二、标准漏孔参数", MARGIN, headerPaint)
        y += 2f
        val q0Str = "${r.q0Mantissa} × 10^(${r.q0Exponent}) Pa·m³/s  (实测 ${"%.4e".format(r.q0Value)})"
        projectRow(canvas, y, "Q0 标定漏率", q0Str, bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "标漏检定温度", "${r.tCal} °C", bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "Qt 试验温度下标准漏率", "${"%.4e".format(r.qtValue)} Pa·m³/s", bodyPaint, smallPaint)
        y += 6f
        drawLine()

        // ==================== 3. 检漏仪校准 ====================
        checkSpace(30f); drawHeaderBg(18f)
        drawText("三、检漏仪校准数据", MARGIN, headerPaint)
        y += 2f
        projectRow(canvas, y, "I0 仪器本底", "${"%.3e".format(r.i0Value)} Pa·m³/s", bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "In 本底噪声", "${"%.3e".format(r.inValue)} Pa·m³/s  [源: ${r.inSource}]", bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "I 校准读数", "${"%.3e".format(r.iValue)} Pa·m³/s", bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "Qmin 仪器最小可检漏率", "${"%.4e".format(r.qminValue)} Pa·m³/s", bodyPaint, smallPaint)
        y += 6f
        drawLine()

        // ==================== 4. 系统校准 ====================
        checkSpace(30f); drawHeaderBg(18f)
        drawText("四、系统校准与测试数据", MARGIN, headerPaint)
        y += 2f
        projectRow(canvas, y, "M0 系统本底", "${"%.3e".format(r.m0Value)} Pa·m³/s", bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "Mn 本底噪声", "${"%.3e".format(r.mnValue)} Pa·m³/s  [源: ${r.mnSource}]", bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "M1 系统校准读数", "${"%.3e".format(r.m1Value)} Pa·m³/s", bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "Qeim 系统最小可检漏率", "${"%.4e".format(r.qeimValue)} Pa·m³/s", bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "M2 喷氦后读数", "${"%.3e".format(r.m2Value)} Pa·m³/s", bodyPaint, smallPaint); y += LINE_HEIGHT
        projectRow(canvas, y, "反应时间 / 氦浓度", "${r.tResponse} s / ${r.tgPercent}%", bodyPaint, smallPaint)
        y += 6f
        drawLine()

        // ==================== 5. 判定 ====================
        checkSpace(30f); drawHeaderBg(18f)
        drawText("五、判定结果", MARGIN, headerPaint)
        y += 4f
        val margin = if (r.qMeasured > 0) r.acceptanceLimitValue() / r.qMeasured else Double.POSITIVE_INFINITY
        val db = if (r.qMeasured > 0) 10.0 * kotlin.math.log10(margin) else Double.POSITIVE_INFINITY
        if (r.qMeasured > 0) {
            projectRow(canvas, y, "安全裕度", "约 ${margin.toLong()} 倍 (${db.toInt()} dB)", bodyPaint, smallPaint)
            y += LINE_HEIGHT
        }
        projectRow(canvas, y, "实测漏率 Q", "${"%.4e".format(r.qMeasured)} Pa·m³/s", bodyPaint, smallPaint)
        y += LINE_HEIGHT
        projectRow(canvas, y, "验收标准", "< ${"%.1e".format(r.acceptanceLimitValue())} Pa·m³/s", bodyPaint, smallPaint)

        // ==================== 6. 警告 ====================
        val warnings = parseWarnings(r)
        if (warnings.isNotEmpty()) {
            y += 10f
            checkSpace(30f); drawHeaderBg(18f)
            drawText("六、验证警告 (${warnings.size} 条)", MARGIN, headerPaint)
            y += 4f
            warnings.forEach { w ->
                checkSpace(SMALL_LINE + 4f)
                val wp = Paint(smallPaint)
                // 文本换行
                val words = "• $w"
                val lines = wrapText(words, CONTENT_WIDTH, wp)
                lines.forEach { line ->
                    checkSpace(SMALL_LINE + 2f)
                    canvas.drawText(line, MARGIN, y, wp)
                    y += SMALL_LINE
                }
            }
        }

        // ==================== 附录：计算公式 ====================
        y += 10f
        checkSpace(30f); drawHeaderBg(18f)
        drawText("附录：计算公式", MARGIN, headerPaint)
        y += 4f
        val formulas = listOf(
            "① Qt = Q0 × [1 - (T_cal - T_test) × 0.03]",
            "② In = max(In_measured, I0 / 10)",
            "③ Qmin = In × Qt / (I - I0)",
            "④ Mn = max(Mn_measured, M0 / 10)",
            "⑤ Qeim = Mn × Qt / (M1 - M0)",
            "⑥ Q = Qt × (M2 - M0) / (M1 - M0) × (100 / TG%)",
        )
        formulas.forEach { f ->
            checkSpace(SMALL_LINE + 2f)
            canvas.drawText(f, MARGIN, y, smallPaint)
            y += SMALL_LINE
        }

        // ==================== 页脚 ====================
        y += 20f
        checkSpace(30f)
        drawLine()
        canvas.drawText(
            "报告生成时间: ${dateFormat.format(Date())}    应用: 氦检漏计算器 V1.0",
            MARGIN, y, smallPaint
        )
        y += SMALL_LINE
        canvas.drawText(
            "本报告由 Android 应用自动生成，数据以原始记录为准。",
            MARGIN, y, smallPaint
        )

        doc.finishPage(page)
    }

    // ====== 辅助绘制 ======

    private fun projectRow(
        canvas: Canvas, y: Float,
        label: String, value: String,
        bodyPaint: Paint, smallPaint: Paint,
    ) {
        val lp = Paint(smallPaint).apply { color = COLOR_LIGHT }
        canvas.drawText(label, MARGIN, y, lp)
        canvas.drawText(value, MARGIN + 140f, y, bodyPaint)
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val lines = mutableListOf<String>()
        var current = ""
        for (char in text) {
            val test = current + char
            if (paint.measureText(test) > maxWidth && current.isNotEmpty()) {
                lines.add(current)
                current = char.toString()
            } else {
                current = test
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines.ifEmpty { listOf(text) }
    }

    private fun parseWarnings(r: DetectionRecordEntity): List<String> {
        return try {
            com.google.gson.Gson()
                .fromJson<List<String>>(
                    com.google.gson.Gson().toJson(r.warnings),
                    object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                )
        } catch (_: Exception) {
            if (r.warnings.isNotEmpty()) listOf(r.warnings.joinToString(", "))
            else emptyList()
        }
    }
}
