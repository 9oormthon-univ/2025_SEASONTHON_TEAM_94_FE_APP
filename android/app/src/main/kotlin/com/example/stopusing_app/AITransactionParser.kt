package com.example.stopusing_app

import android.content.Context
import android.util.Log
import java.util.regex.Pattern

/**
 * AI-Enhanced Transaction Parser
 * 하이브리드 시스템: 규칙 기반 + 경량 AI 모델
 */
class AITransactionParser(private val context: Context) {
    
    companion object {
        private const val TAG = "AITransactionParser"
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.90
        private const val AI_MODEL_THRESHOLD = 0.70
    }
    
    // 단계별 파싱 결과
    data class ParseResult(
        val amount: Long?,
        val merchant: String?,
        val transactionType: String?,
        val confidence: Double,
        val method: String // "RULE_BASED", "AI_MODEL", "HYBRID"
    )
    
    /**
     * 메인 파싱 함수 - 3단계 하이브리드 처리
     */
    fun parseTransaction(text: String, packageName: String): ParseResult {
        Log.d(TAG, "🧠 Starting AI-enhanced parsing for: $packageName")
        
        // Phase 1: 고신뢰도 규칙 기반 파싱 시도
        val ruleBasedResult = tryRuleBasedParsing(text, packageName)
        if (ruleBasedResult.confidence >= HIGH_CONFIDENCE_THRESHOLD) {
            Log.d(TAG, "✅ High confidence rule-based result: ${ruleBasedResult.confidence}")
            return ruleBasedResult
        }
        
        // Phase 2: AI 모델 파싱 (향후 구현)
        val aiResult = tryAIModelParsing(text, packageName)
        if (aiResult.confidence >= AI_MODEL_THRESHOLD) {
            Log.d(TAG, "🤖 AI model result: ${aiResult.confidence}")
            return aiResult
        }
        
        // Phase 3: 하이브리드 결합
        val hybridResult = combineResults(ruleBasedResult, aiResult)
        Log.d(TAG, "🔄 Hybrid result: ${hybridResult.confidence}")
        
        return hybridResult
    }
    
    /**
     * 개선된 규칙 기반 파싱 (기존 로직 + 확신도 계산)
     */
    private fun tryRuleBasedParsing(text: String, packageName: String): ParseResult {
        var confidence = 0.0
        var amount: Long? = null
        var merchant: String? = null
        var transactionType: String? = null
        
        // 금액 추출 (고신뢰도)
        val amountPatterns = arrayOf(
            Pattern.compile("([0-9,]+)원", Pattern.CASE_INSENSITIVE),
            Pattern.compile("출금\\s*([0-9,]+)원", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([0-9,]+)\\s*원.*출금", Pattern.CASE_INSENSITIVE)
        )
        
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                try {
                    amount = matcher.group(1)?.replace(",", "")?.toLong()
                    confidence += 0.4 // 금액 매칭 시 신뢰도 +0.4
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "Amount parsing error", e)
                }
            }
        }
        
        // 거래 유형 추출
        val transactionPatterns = mapOf(
            "출금" to arrayOf("출금", "스마트폰출금", "ATM출금"),
            "이체" to arrayOf("이체", "송금"),
            "결제" to arrayOf("결제", "승인", "결제완료"),
            "인출" to arrayOf("현금출금", "인출")
        )
        
        for ((type, keywords) in transactionPatterns) {
            for (keyword in keywords) {
                if (text.contains(keyword)) {
                    transactionType = type
                    confidence += 0.3 // 거래유형 매칭 시 신뢰도 +0.3
                    break
                }
            }
            if (transactionType != null) break
        }
        
        // 은행별 특화 상인명 추출
        merchant = extractMerchantByBank(text, packageName)
        if (merchant != null && merchant != "알 수 없음") {
            confidence += 0.3 // 상인명 매칭 시 신뢰도 +0.3
        }
        
        return ParseResult(
            amount = amount,
            merchant = merchant,
            transactionType = transactionType,
            confidence = confidence,
            method = "RULE_BASED"
        )
    }
    
    /**
     * AI 모델 파싱 (향후 TensorFlow Lite 통합)
     */
    private fun tryAIModelParsing(text: String, packageName: String): ParseResult {
        // TODO: TensorFlow Lite 모델 로딩 및 추론
        // 현재는 placeholder로 구현
        
        Log.d(TAG, "🔄 AI model parsing not yet implemented, using enhanced rules")
        
        // 임시로 개선된 패턴 매칭 구현
        return enhancedPatternMatching(text, packageName)
    }
    
    /**
     * 개선된 패턴 매칭 (AI 모델 대체용)
     */
    private fun enhancedPatternMatching(text: String, packageName: String): ParseResult {
        var confidence = 0.0
        var amount: Long? = null
        var merchant: String? = null
        var transactionType = "출금"
        
        // 더 정교한 패턴 매칭
        val enhancedPatterns = arrayOf(
            // KB 패턴: "이*혁님 08/25 19:39 941602-**-***064 이수혁 스마트폰출금 1 잔액73,349"
            Pattern.compile("([가-힣]*님)\\s+\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}\\s+[0-9-*]+\\s+([가-힣]+)\\s+(스마트폰출금|ATM출금|이체)\\s+([0-9,]+)"),
            
            // 일반 패턴: "상인명 금액원 거래유형"
            Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+([0-9,]+)원\\s*(출금|결제|이체|승인)"),
            
            // 역순 패턴: "거래유형 금액원 상인명"
            Pattern.compile("(출금|결제|이체|승인)\\s+([0-9,]+)원\\s+([가-힣a-zA-Z\\s]+)"),
            
            // 괄호 패턴: "[상인명] 금액원"
            Pattern.compile("\\[([가-힣a-zA-Z\\s]+)\\]\\s*([0-9,]+)원")
        )
        
        for (pattern in enhancedPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                try {
                    when (matcher.groupCount()) {
                        4 -> { // KB 패턴
                            merchant = matcher.group(2)?.trim()
                            transactionType = matcher.group(3) ?: "출금"
                            amount = matcher.group(4)?.replace(",", "")?.toLong()
                            confidence = 0.85
                        }
                        3 -> { // 일반 패턴들
                            if (matcher.group(2)?.matches(Regex("[0-9,]+")) == true) {
                                merchant = matcher.group(1)?.trim()
                                amount = matcher.group(2)?.replace(",", "")?.toLong()
                                transactionType = matcher.group(3) ?: "출금"
                            } else {
                                merchant = matcher.group(3)?.trim()
                                amount = matcher.group(2)?.replace(",", "")?.toLong()
                                transactionType = matcher.group(1) ?: "출금"
                            }
                            confidence = 0.75
                        }
                        2 -> { // 괄호 패턴
                            merchant = matcher.group(1)?.trim()
                            amount = matcher.group(2)?.replace(",", "")?.toLong()
                            confidence = 0.70
                        }
                    }
                    
                    if (amount != null && amount > 0) {
                        Log.d(TAG, "🎯 Enhanced pattern matched: merchant=$merchant, amount=$amount")
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Enhanced pattern parsing error", e)
                }
            }
        }
        
        return ParseResult(
            amount = amount,
            merchant = merchant ?: "알 수 없음",
            transactionType = transactionType,
            confidence = confidence,
            method = "AI_MODEL"
        )
    }
    
    /**
     * 결과 결합 및 검증
     */
    private fun combineResults(ruleResult: ParseResult, aiResult: ParseResult): ParseResult {
        val finalAmount = ruleResult.amount ?: aiResult.amount
        val finalMerchant = if (ruleResult.merchant != null && ruleResult.merchant != "알 수 없음") 
            ruleResult.merchant else aiResult.merchant
        val finalType = ruleResult.transactionType ?: aiResult.transactionType
        
        // 결과 일치도에 따른 최종 신뢰도 계산
        var combinedConfidence = (ruleResult.confidence + aiResult.confidence) / 2
        
        // 일치하는 정보가 많을수록 신뢰도 증가
        if (ruleResult.amount == aiResult.amount) combinedConfidence += 0.1
        if (ruleResult.merchant == aiResult.merchant) combinedConfidence += 0.1
        if (ruleResult.transactionType == aiResult.transactionType) combinedConfidence += 0.1
        
        return ParseResult(
            amount = finalAmount,
            merchant = finalMerchant,
            transactionType = finalType,
            confidence = combinedConfidence.coerceAtMost(1.0),
            method = "HYBRID"
        )
    }
    
    /**
     * 은행별 특화 상인명 추출
     */
    private fun extractMerchantByBank(text: String, packageName: String): String? {
        return when {
            packageName.contains("kbstar") || text.contains("KB") -> {
                extractKBMerchant(text)
            }
            packageName.contains("shinhan") -> {
                extractShinhanMerchant(text)
            }
            packageName.contains("toss") -> {
                extractTossMerchant(text)
            }
            else -> {
                extractGenericMerchant(text)
            }
        }
    }
    
    private fun extractKBMerchant(text: String): String? {
        // KB 특화 패턴들
        val kbPatterns = arrayOf(
            Pattern.compile("[0-9]{6}-[*]{2}-[*]{3}[0-9]{3}\\s+([가-힣]+)\\s+(스마트폰출금|ATM출금)"),
            Pattern.compile("([가-힣]{2,6})\\s+(스마트폰출금|ATM출금|이체)")
        )
        
        for (pattern in kbPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)?.trim()
            }
        }
        return null
    }
    
    private fun extractShinhanMerchant(text: String): String? {
        val shinhanPatterns = arrayOf(
            Pattern.compile("\\[([가-힣a-zA-Z\\s]+)\\]"),
            Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+승인")
        )
        
        for (pattern in shinhanPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)?.trim()
            }
        }
        return null
    }
    
    private fun extractTossMerchant(text: String): String? {
        val tossPatterns = arrayOf(
            Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+[0-9,]+원"),
            Pattern.compile("결제.*?([가-힣a-zA-Z\\s]+)")
        )
        
        for (pattern in tossPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)?.trim()
            }
        }
        return null
    }
    
    private fun extractGenericMerchant(text: String): String? {
        val genericPatterns = arrayOf(
            Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+[0-9,]+원"),
            Pattern.compile("[0-9,]+원\\s+([가-힣a-zA-Z\\s]+)")
        )
        
        for (pattern in genericPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (merchant != null && !merchant.matches(Regex("(출금|결제|이체|승인|은행|카드)"))) {
                    return merchant
                }
            }
        }
        return null
    }
}