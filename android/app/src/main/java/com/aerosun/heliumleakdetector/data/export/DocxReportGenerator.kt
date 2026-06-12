package com.aerosun.heliumleakdetector.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 氦检漏检测报告 DOCX 生成器。
 *
 * 纯 Kotlin/ZIP 实现，零外部依赖。生成的 DOCX 可在 Microsoft Word / WPS 中编辑。
 *
 * DOCX 文件实质是 ZIP 包，包含 XML 文件，本类通过 WordprocessingML 标准生成。
 */
object DocxReportGenerator {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // WML 命名空间常量
    private const val WML = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    private const val RELS = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val CT = "http://schemas.openxmlformats.org/package/2006/content-types"

    // 颜色
    private const val CLR_TITLE = "1565C0"
    private const val CLR_HEADER_BG = "EEF2F7"
    private const val CLR_PASS = "2E7D32"
    private const val CLR_FAIL = "C62828"
    private const val CLR_LIGHT = "808080"
    private const val CLR_BORDER = "B0B0B0"
    private const val CLR_WHITE = "FFFFFF"

    /**
     * 生成 DOCX 报告并返回分享 Intent。
     */
    fun exportDocx(context: Context, record: DetectionRecordEntity): Intent {
        // 模板填充 (设备数据由 filler 内部查询)
        val file = DocxTemplateFiller.fill(context, record)
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun createDocxFile(context: Context, record: DetectionRecordEntity): File {
        val safeName = record.reportNo.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val timestamp = dateFormat.format(Date()).replace(":", "-").replace(" ", "_")
        val file = File(context.cacheDir, "report_${safeName}_$timestamp.docx")

        ZipOutputStream(file.outputStream()).use { zip ->
            // 1. [Content_Types].xml
            zip.putEntry("[Content_Types].xml", contentTypesXml())
            // 2. _rels/.rels
            zip.putEntry("_rels/.rels", relsXml())
            // 3. word/_rels/document.xml.rels
            zip.putEntry("word/_rels/document.xml.rels", docRelsXml())
            // 4. word/document.xml (正文)
            zip.putEntry("word/document.xml", documentXml(record))
            zip.closeEntry()
        }
        return file
    }

    // ====== ZIP Entry 工具 ======

    private fun ZipOutputStream.putEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    // ====== XML 模板 ======

    private fun xmlHeader(standalone: Boolean = true): String {
        val sa = if (standalone) """ standalone="yes"""" else ""
        return """<?xml version="1.0" encoding="UTF-8"$sa?>"""
    }

    private fun contentTypesXml(): String = buildString {
        append(xmlHeader())
        append("""<Types xmlns="$CT">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>""")
        append("""</Types>""")
    }

    private fun relsXml(): String = buildString {
        append(xmlHeader())
        append("""<Relationships xmlns="$RELS">""")
        append("""<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>""")
        append("""</Relationships>""")
    }

    private fun docRelsXml(): String = buildString {
        append(xmlHeader())
        append("""<Relationships xmlns="$RELS">""")
        append("""</Relationships>""")
    }

    // ====== 文档正文 ======

    private fun documentXml(r: DetectionRecordEntity): String = buildString {
        append(xmlHeader(false))
        append("""<w:document xmlns:w="$WML">""")
        append("<w:body>")

        // ======== 标题 ========
        p(align = "center") {
            r { t("氦检漏试验检测报告", sz = 36, bold = true, color = CLR_TITLE) }
        }
        p(align = "center") {
            r { t("Helium Leak Test Report", sz = 20, color = CLR_LIGHT) }
        }
        spacer()

        // ======== 判定标签 ========
        val (verdict, vColor) = if (r.isAcceptable) "✓ 合格 PASS" to CLR_PASS else "✗ 不合格 FAIL" to CLR_FAIL
        p(align = "center") {
            r { t(verdict, sz = 32, bold = true, color = vColor) }
        }
        p(align = "center") {
            r { t("实测漏率: ${"%.4e".format(r.qMeasured)} Pa·m³/s", sz = 22, color = vColor) }
        }
        p(align = "center") {
            r { t("验收标准: < ${"%.1e".format(r.acceptanceLimitValue())} Pa·m³/s", sz = 20, color = CLR_LIGHT) }
        }
        spacer()

        // ======== 一、项目信息 ========
        heading("一、项目信息")
        tbl(
            "报告编号" to r.reportNo,
            "合同号" to r.contractNo,
            "试验日期" to r.testDate,
            "产品代码" to r.productCode,
            "产品名称" to r.productName,
            "焊缝名称" to r.weldName,
            "检测区域" to r.inspectionArea,
            "试验方法" to r.testMethod,
            "环境条件" to "${r.temperature} °C / ${r.humidity}%",
        )

        // ======== 二、标准漏孔 ========
        heading("二、标准漏孔参数")
        tbl(
            "Q0 标定漏率" to "${r.q0Mantissa} ×10^(${r.q0Exponent}) = ${"%.4e".format(r.q0Value)} Pa·m³/s",
            "标漏检定温度" to "${r.tCal} °C",
            "Qt 试验温度下标准漏率" to "${"%.4e".format(r.qtValue)} Pa·m³/s",
        )

        // ======== 三、检漏仪校准 ========
        heading("三、检漏仪校准数据")
        tbl(
            "I0 仪器本底" to "${"%.3e".format(r.i0Value)} Pa·m³/s",
            "In 本底噪声" to "${"%.3e".format(r.inValue)} Pa·m³/s  [来源: ${r.inSource}]",
            "I 校准读数" to "${"%.3e".format(r.iValue)} Pa·m³/s",
            "Qmin 仪器最小可检漏率" to "${"%.4e".format(r.qminValue)} Pa·m³/s",
        )

        // ======== 四、系统校准 ========
        heading("四、系统校准与测试数据")
        tbl(
            "M0 系统本底" to "${"%.3e".format(r.m0Value)} Pa·m³/s",
            "Mn 本底噪声" to "${"%.3e".format(r.mnValue)} Pa·m³/s  [来源: ${r.mnSource}]",
            "M1 系统校准读数" to "${"%.3e".format(r.m1Value)} Pa·m³/s",
            "Qeim 系统最小可检漏率" to "${"%.4e".format(r.qeimValue)} Pa·m³/s",
            "M2 喷氦后读数" to "${"%.3e".format(r.m2Value)} Pa·m³/s",
            "反应时间 / 氦浓度" to "${r.tResponse} s / ${r.tgPercent}%",
        )

        // ======== 五、判定 ========
        heading("五、判定结果")
        tbl(
            "实测漏率 Q" to "${"%.4e".format(r.qMeasured)} Pa·m³/s",
            "验收标准" to "< ${"%.1e".format(r.acceptanceLimitValue())} Pa·m³/s",
        )
        if (r.qMeasured > 0) {
            val margin = r.acceptanceLimitValue() / r.qMeasured
            val db = 10.0 * kotlin.math.log10(margin)
            tbl("安全裕度" to "约 ${margin.toLong()} 倍 (${db.toInt()} dB)")
        }

        // ======== 六、警告 ========
        val warnings = parseWarnings(r)
        if (warnings.isNotEmpty()) {
            heading("六、验证警告 (${warnings.size} 条)")
            warnings.forEach { w ->
                p { r { t("• $w", sz = 20, color = CLR_FAIL) } }
            }
        }

        // ======== 附录 ========
        heading("附录：计算公式")
        val formulas = listOf(
            "① Qt = Q0 × [1 - (T_cal - T_test) × 0.03]",
            "② In = max(In_measured, I0 / 10)",
            "③ Qmin = In × Qt / (I - I0)",
            "④ Mn = max(Mn_measured, M0 / 10)",
            "⑤ Qeim = Mn × Qt / (M1 - M0)",
            "⑥ Q = Qt × (M2 - M0) / (M1 - M0) × (100 / TG%)",
        )
        formulas.forEach { f ->
            p { r { t(f, sz = 18, color = CLR_LIGHT) } }
        }

        // ======== 页脚 ========
        spacer()
        tblBorder()
        p {
            r { t("报告生成时间: ${dateFormat.format(Date())}    应用: 氦检漏计算器 V1.0", sz = 16, color = CLR_LIGHT) }
        }
        p {
            r { t("本报告由 Android 应用自动生成，可在 Microsoft Word 或 WPS 中编辑。", sz = 16, color = CLR_LIGHT) }
        }

        // body/document 闭合
        append("</w:body></w:document>")
    }

    // ====== WML Builder ======

    private fun StringBuilder.p(align: String? = null, block: StringBuilder.() -> Unit) {
        append("<w:p>")
        if (align != null) append("""<w:pPr><w:jc w:val="$align"/></w:pPr>""")
        block()
        append("</w:p>")
    }

    private fun StringBuilder.r(block: StringBuilder.() -> Unit) {
        append("<w:r>")
        block()
        append("</w:r>")
    }

    private fun StringBuilder.t(text: String, sz: Int = 21, bold: Boolean = false, color: String? = null) {
        append("<w:rPr>")
        append("""<w:sz w:val="$sz"/>""")
        append("""<w:szCs w:val="$sz"/>""")
        if (bold) append("""<w:b/><w:bCs/>""")
        if (color != null) append("""<w:color w:val="$color"/>""")
        append("""<w:rFonts w:eastAsia="Microsoft YaHei"/>""")
        append("</w:rPr>")
        append("<w:t xml:space=\"preserve\">").append(escapeXml(text)).append("</w:t>")
    }

    private fun StringBuilder.spacer() {
        p { r { t("", sz = 10) } }
    }

    private fun StringBuilder.heading(text: String) {
        p {
            r { t(text, sz = 24, bold = true, color = CLR_TITLE) }
        }
        // 标题背景线 (通过下边框实现)
        append("""<w:p><w:pPr><w:pBdr><w:bottom w:val="single" w:sz="4" w:space="1" w:color="$CLR_TITLE"/></w:pBdr></w:pPr></w:p>""")
    }

    private fun StringBuilder.tbl(vararg rows: Pair<String, String>) {
        append("""<w:tbl><w:tblPr><w:tblW w:w="9000" w:type="dxa"/><w:tblBorders><w:top w:val="single" w:sz="4" w:color="$CLR_BORDER"/><w:bottom w:val="single" w:sz="4" w:color="$CLR_BORDER"/><w:left w:val="single" w:sz="4" w:color="$CLR_BORDER"/><w:right w:val="single" w:sz="4" w:color="$CLR_BORDER"/><w:insideH w:val="single" w:sz="4" w:color="$CLR_BORDER"/><w:insideV w:val="single" w:sz="4" w:color="$CLR_BORDER"/></w:tblBorders></w:tblPr>""")
        rows.forEachIndexed { i, (label, value) ->
            val bg = if (i % 2 == 0) CLR_HEADER_BG else CLR_WHITE
            append("<w:tr>")
            append("""<w:tc><w:tcPr><w:tcW w:w="2400" w:type="dxa"/><w:shd w:val="clear" w:fill="$bg"/></w:tcPr>""")
            p { r { t(label, sz = 20, bold = true, color = "444444") } }
            append("</w:tc>")
            append("""<w:tc><w:tcPr><w:tcW w:w="6600" w:type="dxa"/><w:shd w:val="clear" w:fill="$bg"/></w:tcPr>""")
            p { r { t(value, sz = 20) } }
            append("</w:tc></w:tr>")
        }
        append("</w:tbl>")
        spacer()
    }

    private fun StringBuilder.tblBorder() {
        append("""<w:p><w:pPr><w:pBdr><w:bottom w:val="single" w:sz="4" w:space="1" w:color="$CLR_BORDER"/></w:pBdr></w:pPr></w:p>""")
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

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
