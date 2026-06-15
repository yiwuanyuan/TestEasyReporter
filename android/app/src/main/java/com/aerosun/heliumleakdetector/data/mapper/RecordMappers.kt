package com.aerosun.heliumleakdetector.data.mapper

import com.aerosun.heliumleakdetector.data.local.entity.DetectionRecordEntity
import com.aerosun.heliumleakdetector.domain.model.TestInput
import com.aerosun.heliumleakdetector.domain.model.TestResult

/**
 * TestInput → Entity 映射（保存前转换）。
 */
fun TestInput.toEntity(
    result: TestResult,
    id: Long = 0,
    createdAt: Long = System.currentTimeMillis(),
    equipmentIds: String = "",
): DetectionRecordEntity = DetectionRecordEntity(
    id = id,
    reportNo = reportNo, contractNo = contractNo, testDate = testDate,
    productCode = productCode, productName = productName,
    weldName = weldName, inspectionArea = inspectionArea,
    productSerialNo = productSerialNo,
    equipmentIds = equipmentIds,
    testMethod = testMethod, testProcedureNo = testProcedureNo,
    temperature = temperature, humidity = humidity,
    q0Mantissa = q0Mantissa, q0Exponent = q0Exponent, tCal = tCal,
    i0Mantissa = i0Mantissa, i0Exponent = i0Exponent,
    iMantissa = iMantissa, iExponent = iExponent,
    m0Mantissa = m0Mantissa, m0Exponent = m0Exponent,
    m1Mantissa = m1Mantissa, m1Exponent = m1Exponent,
    m2Mantissa = m2Mantissa, m2Exponent = m2Exponent,
    tResponse = tResponse, tgPercent = tgPercent,
    inMeasuredMantissa = inMeasuredMantissa, inMeasuredExponent = inMeasuredExponent,
    mnMeasuredMantissa = mnMeasuredMantissa, mnMeasuredExponent = mnMeasuredExponent,
    acceptanceLimitMantissa = acceptanceLimitMantissa, acceptanceLimitExponent = acceptanceLimitExponent,
    tempCoefficient = tempCoefficient,
    // 计算结果冗余
    q0Value = result.q0, qtValue = result.qt,
    i0Value = result.i0, inValue = result.inValue, inSource = result.inSource,
    iValue = result.i, qminValue = result.qmin,
    m0Value = result.m0, mnValue = result.mnValue, mnSource = result.mnSource,
    m1Value = result.m1, qeimValue = result.qeim, m2Value = result.m2,
    qMeasured = result.qMeasured, isAcceptable = result.isAcceptable,
    warnings = result.warnings,
    createdAt = createdAt, updatedAt = System.currentTimeMillis(),
)

/**
 * Entity → TestInput 映射（读取后还原）。
 */
fun DetectionRecordEntity.toTestInput(): TestInput = TestInput(
    reportNo = reportNo, contractNo = contractNo, testDate = testDate,
    productCode = productCode, productName = productName,
    weldName = weldName, inspectionArea = inspectionArea,
    productSerialNo = productSerialNo,
    testMethod = testMethod, testProcedureNo = testProcedureNo,
    temperature = temperature, humidity = humidity,
    q0Mantissa = q0Mantissa, q0Exponent = q0Exponent, tCal = tCal,
    i0Mantissa = i0Mantissa, i0Exponent = i0Exponent,
    iMantissa = iMantissa, iExponent = iExponent,
    m0Mantissa = m0Mantissa, m0Exponent = m0Exponent,
    m1Mantissa = m1Mantissa, m1Exponent = m1Exponent,
    m2Mantissa = m2Mantissa, m2Exponent = m2Exponent,
    tResponse = tResponse, tgPercent = tgPercent,
    inMeasuredMantissa = inMeasuredMantissa, inMeasuredExponent = inMeasuredExponent,
    mnMeasuredMantissa = mnMeasuredMantissa, mnMeasuredExponent = mnMeasuredExponent,
    acceptanceLimitMantissa = acceptanceLimitMantissa, acceptanceLimitExponent = acceptanceLimitExponent,
    tempCoefficient = tempCoefficient,
)

/**
 * Entity → TestResult 映射（从冗余计算字段还原）。
 */
fun DetectionRecordEntity.toTestResult(): TestResult = TestResult(
    q0 = q0Value, qt = qtValue,
    i0 = i0Value, inValue = inValue, inSource = inSource,
    i = iValue, qmin = qminValue,
    m0 = m0Value, mnValue = mnValue, mnSource = mnSource,
    m1 = m1Value, qeim = qeimValue, m2 = m2Value,
    qMeasured = qMeasured, isAcceptable = isAcceptable,
    warnings = warnings,
)
