package com.aerosun.heliumleakdetector.data.export

import android.content.Context
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import com.aerosun.heliumleakdetector.data.local.entity.EquipmentEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
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
        val equipment = loadEquipment(context, record.equipmentIds)
        val data = buildDataMap(record, equipment)
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
        for ((key, value) in data) {
            val escaped = escapeXml(value)
            val marker = "{{$key}}"
            if (result.contains(marker)) {
                result = result.replace(marker, escaped)
                continue
            }
            val pattern = buildCrossTagPattern(key)
            result = pattern.replace(result, escaped)
        }
        result = result.replace(Regex("\\{\\{[^}]*\\}\\}"), "")
        return result
    }

    private fun buildCrossTagPattern(key: String): Regex {
        // 在 key 的每个字符中间插入 (<[^>]*>)*? 以匹配跨标签拆分
        // 例如 LDM → L(<[^>]*>)*?D(<[^>]*>)*?M
        val pattern = key.toList().joinToString("(<[^>]*>)*?") { Regex.escape(it.toString()) }
        val withTags = "\\{\\{(<[^>]*>)*?$pattern(<[^>]*>)*?\\}\\}"
        return try {
            Regex(withTags, RegexOption.DOT_MATCHES_ALL)
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

    // ---- 从 equipmentIds JSON 加载设备数据 ----

    private fun loadEquipment(context: Context, equipmentIdsJson: String): List<EquipmentEntity> {
        if (equipmentIdsJson.isBlank()) return emptyList()
        val ids: List<Long> = try {
            val type = object : TypeToken<List<Long>>() {}.type
            Gson().fromJson(equipmentIdsJson, type) ?: emptyList()
        } catch (_: Exception) { return emptyList() }
        if (ids.isEmpty()) return emptyList()

        return try {
            val db = androidx.room.Room.databaseBuilder(
                context.applicationContext,
                com.aerosun.heliumleakdetector.data.local.HeliumDatabase::class.java,
                "helium_database"
            ).build()
            val all = kotlinx.coroutines.runBlocking {
                db.equipmentDao().getAll()
            }
            db.close()
            all.filter { it.id in ids }
        } catch (_: Exception) { emptyList() }
    }

    // ---- 数据构建 ----

    private fun buildDataMap(r: DetectionRecordEntity, equipment: List<EquipmentEntity> = emptyList()): Map<String, String> {
        fun sci(v: Double) = "%.3E".format(v)
        fun sci4(v: Double) = "%.4E".format(v)
        val accLimit = r.acceptanceLimitValue()
        val passed = r.isAcceptable

        fun eq(type: String): EquipmentEntity? = equipment.firstOrNull { t ->
            t.type.contains(type) || t.name.contains(type)
        }
        val ld = eq("检漏仪")
        val sl = eq("标准漏孔")
        val tm = eq("温度计")
        val hc = eq("氦浓度计")

        return mapOf(
            "Report No." to r.reportNo,
            "Contract No" to r.contractNo,
            "Product Name" to r.productName,
            "Serial No." to r.productSerialNo.ifEmpty { r.productCode },
            "Temperature" to "${r.temperature}",
            "Test Date" to r.testDate,
            "Relative Humidity" to "${r.humidity}",
            "Inspection Area" to r.inspectionArea,
            "Test Procedure No." to r.testProcedureNo,
            "Acceptance" to "<${"%.1E".format(accLimit)} Pa·m³/s",
            "LDM" to (ld?.model ?: ""),
            "LDN" to (ld?.serialNo ?: ""),
            "LDVP" to (ld?.calibrationDueDate?.let { if (it > 0) dateFmt.format(Date(it)) else "" } ?: ""),
            "SLM" to (sl?.model ?: ""),
            "SLN" to (sl?.serialNo ?: ""),
            "SLVP" to (sl?.calibrationDueDate?.let { if (it > 0) dateFmt.format(Date(it)) else "" } ?: ""),
            "TM" to (tm?.model ?: ""),
            "TN" to (tm?.serialNo ?: ""),
            "TVP" to (tm?.calibrationDueDate?.let { if (it > 0) dateFmt.format(Date(it)) else "" } ?: ""),
            "HCM" to (hc?.model ?: ""),
            "HCN" to (hc?.serialNo ?: ""),
            "HCVP" to (hc?.calibrationDueDate?.let { if (it > 0) dateFmt.format(Date(it)) else "" } ?: ""),
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
            "Qcompare_zh" to if (passed) "小于" else "大于",
            "Qcompare_en" to if (passed) "less than" else "greater than",
            "Qresult_zh" to if (passed) "合格" else "不合格",
            "Qresult_en" to if (passed) "Pass" else "Fail",
        )
    }
}
