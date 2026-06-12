package com.aerosun.heliumleakdetector.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 检测记录报告导出工具。
 *
 * 支持 JSON（结构化数据，可重新导入）和 CSV（表格，Excel/Excel兼容）两种格式。
 */
object ReportExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())

    /**
     * 将记录导出为 JSON，并通过系统分享 Intent 发送。
     */
    fun exportJson(context: Context, record: DetectionRecordEntity): Intent {
        val json = toJson(record)
        val file = createTempFile(context, record.reportNo, "json")
        file.writeText(json)
        return createShareIntent(context, file, "application/json", "${record.reportNo}.json")
    }

    /**
     * 将记录导出为 CSV，并通过系统分享 Intent 发送。
     */
    fun exportCsv(context: Context, record: DetectionRecordEntity): Intent {
        val csv = toFlatCsv(record)
        val file = createTempFile(context, record.reportNo, "csv")
        file.writeText(csv)
        return createShareIntent(context, file, "text/csv", "${record.reportNo}.csv")
    }

    /**
     * 将多份记录导出为 CSV（适合批量导出）。
     */
    fun exportBatchCsv(context: Context, records: List<DetectionRecordEntity>): Intent {
        val csv = buildString {
            append(CsvHeader)
            append("\n")
            records.forEach { append(toCsvRow(it)).append("\n") }
        }
        val file = createTempFile(context, "batch_export", "csv")
        file.writeText(csv)
        return createShareIntent(context, file, "text/csv", "helium_records_${dateFormat.format(Date())}.csv")
    }

    // ====== JSON 序列化 ======

    fun toJson(record: DetectionRecordEntity): String {
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Double::class.java, JsonSerializer<Double> { value, _, _ ->
                // 科学计数法保留 4 位有效数字
                JsonPrimitive(String.format(Locale.US, "%.4e", value))
            })
            .create()
        return gson.toJson(record)
    }

    // ====== CSV 序列化 ======

    private const val CsvHeader =
        "report_no,contract_no,test_date,product_name,weld_name," +
        "temperature,humidity," +
        "Q0_mantissa,Q0_exponent,T_cal," +
        "I0_mantissa,I0_exponent,I_mantissa,I_exponent," +
        "M0_mantissa,M0_exponent,M1_mantissa,M1_exponent," +
        "M2_mantissa,M2_exponent,T_response,TG_percent," +
        "Q0_actual,Qt,In,I,Qmin,M0_actual,Mn,M1,Qeim,M2_actual," +
        "Q_measured,is_acceptable,created_at"

    private fun toCsvRow(r: DetectionRecordEntity): String = listOf(
        csvEscape(r.reportNo), csvEscape(r.contractNo), csvEscape(r.testDate),
        csvEscape(r.productName), csvEscape(r.weldName),
        r.temperature, r.humidity,
        r.q0Mantissa, r.q0Exponent, r.tCal,
        r.i0Mantissa, r.i0Exponent, r.iMantissa, r.iExponent,
        r.m0Mantissa, r.m0Exponent, r.m1Mantissa, r.m1Exponent,
        r.m2Mantissa, r.m2Exponent, r.tResponse, r.tgPercent,
        "%.4e".format(r.q0Value), "%.4e".format(r.qtValue),
        "%.3e".format(r.inValue), "%.3e".format(r.iValue), "%.4e".format(r.qminValue),
        "%.3e".format(r.m0Value), "%.3e".format(r.mnValue), "%.3e".format(r.m1Value),
        "%.4e".format(r.qeimValue), "%.3e".format(r.m2Value),
        "%.4e".format(r.qMeasured), r.isAcceptable, r.createdAt,
    ).joinToString(",")

    private fun toFlatCsv(r: DetectionRecordEntity): String =
        "$CsvHeader\n${toCsvRow(r)}"

    private fun csvEscape(value: String): String =
        "\"${value.replace("\"", "\"\"")}\""

    // ====== 文件 & 分享 ======

    private fun createTempFile(context: Context, prefix: String, ext: String): File {
        val safePrefix = prefix.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val dir = context.cacheDir
        return File(dir, "${safePrefix}_${dateFormat.format(Date())}.$ext")
    }

    private fun createShareIntent(
        context: Context,
        file: File,
        mimeType: String,
        displayName: String,
    ): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
