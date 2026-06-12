package com.aerosun.heliumleakdetector.data.export

import android.content.Context
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DocxTemplateFiller {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun fill(context: Context, record: DetectionRecordEntity): File {
        val templateBytes = context.assets.open("report_template.docx").use { it.readBytes() }
        val entries = LinkedHashMap<String, ByteArray>()

        ZipInputStream(templateBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes()
                entry = zip.nextEntry
            }
        }

        val docXml = String(entries["word/document.xml"]!!, Charsets.UTF_8)
        val data = buildDataMap(record)
        val filledXml = fillMarkers(docXml, data)
        entries["word/document.xml"] = filledXml.toByteArray(Charsets.UTF_8)

        val safeName = record.reportNo.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val file = File(context.cacheDir, "report_${safeName}_${System.currentTimeMillis()}.docx")

        ZipOutputStream(file.outputStream()).use { out ->
            entries.forEach { (name, bytes) ->
                out.putNextEntry(ZipEntry(name))
                out.write(bytes)
                out.closeEntry()
            }
        }
        return file
    }

    // ---- 替换引擎 ----

    private fun fillMarkers(xml: String, data: Map<String, String>): String {
        var result = xml
        // 简单替换: 先尝试直接替换 (适合大多数情况，{{key}} 在同一个 <w:t> 内)
        for ((key, value) in data) {
            val escaped = escapeXml(value)
            // 尝试直接替换 {{key}} 为 value
            val marker = "{{$key}}"
            if (result.contains(marker)) {
                result = result.replace(marker, escaped)
                continue
            }
            // 跨标签拆分: 构建正则匹配 {{ 和 }} 之间可能有 XML 标签的情况
            val pattern = buildCrossTagPattern(key)
            result = pattern.replace(result, escaped)
        }
        // 清理残留的孤立 {{ 和 }}（模板中多处的大括号标记）
        result = result.replace(Regex("\\{\\{[^}]*\\}\\}"), "")
        return result
    }

    /**
     * 构建跨标签匹配模式: 匹配 {{ 和 key 和 }} 之间包含任意 XML 标签的情况。
     * 例如 key="Report No." 时, 匹配:
     *   {{Report No.}}
     *   {{</w:t></w:r><w:r><w:t>Report</w:t></w:r><w:r><w:t> No.}}
     */
    private fun buildCrossTagPattern(key: String): Regex {
        val chars = "\\{\\{${Regex.escape(key)}\\}\\}"
        val withTags = "\\{\\{(<[^>]*>)*${Regex.escape(key)}(<[^>]*>)*\\}\\}"
        return try {
            Regex(withTags)
        } catch (e: Exception) {
            Regex(Regex.escape("{{$key}}"))
        }
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    // ---- 数据构建 ----

    private fun buildDataMap(r: DetectionRecordEntity): Map<String, String> {
        fun sci(v: Double) = "%.3E".format(v)
        fun sci4(v: Double) = "%.4E".format(v)

        val accLimit = r.acceptanceLimitValue()
        val passed = r.isAcceptable

        return mapOf(
            "Report No." to r.reportNo,
            "Contract No" to r.contractNo,
            "Product Name" to r.productName,
            "Serial No." to r.productCode,
            "Temperature" to "${r.temperature}",
            "Test Date" to r.testDate,
            "Relative Humidity" to "${r.humidity}",
            "Inspection Area" to r.inspectionArea,
            "Test Procedure No." to r.testProcedureNo,
            "Acceptance" to "<${"%.1E".format(accLimit)} Pa·m³/s",
            "LDM" to "", "LDN" to "", "LDVP" to "",
            "SLM" to "", "SLN" to "", "SLVP" to "",
            "TM" to "", "TN" to "", "TVP" to "",
            "HCM" to "", "HCN" to "", "HCVP" to "",
            "Q0" to sci(r.q0Value),
            "QT" to sci4(r.qtValue),
            "I0" to sci(r.i0Value),
            "In" to sci(r.inValue),
            "I" to sci(r.iValue),
            "Qmin" to sci4(r.qminValue),
            "M0" to sci(r.m0Value),
            "Mn" to sci(r.mnValue),
            "M1" to sci(r.m1Value),
            "M2" to sci(r.m2Value),
            "Qemin" to sci4(r.qeimValue),
            "TS" to "${r.tResponse}",
            "TG" to "${r.tgPercent}",
            "Qs" to sci4(r.qMeasured),
            "Qcompare" to if (passed) "less than" else "greater than",
            "Qresult" to if (passed) "Pass" else "Fail",
        )
    }
}
