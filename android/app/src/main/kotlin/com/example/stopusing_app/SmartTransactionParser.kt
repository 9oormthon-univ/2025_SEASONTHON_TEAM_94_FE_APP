package com.example.stopusing_app

import android.util.Log
import java.util.regex.Pattern

/**
 * Smart Transaction Parser - 개선된 하이브리드 파싱 시스템
 * 규칙 기반 + 패턴 학습 + 신뢰도 기반 검증
 */
class SmartTransactionParser {
    
    companion object {
        private const val TAG = "SmartTransactionParser"
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.85
    }
    
    data class ParsedTransaction(
        val amount: Long?,
        val merchant: String?,
        val transactionType: String?,
        val confidence: Double,
        val details: String = ""
    )
    
    /**
     * 스마트 파싱 - 다단계 검증으로 높은 정확도 달성
     * 출금만 처리, 입금은 제외
     */
    fun parseTransaction(text: String, packageName: String): ParsedTransaction {
        Log.d(TAG, "🧠 Smart parsing started for: ${getAppName(packageName)}")
        Log.d(TAG, "📝 Input text: '$text'")
        
        // 먼저 입출금 구분 - 입금이면 즉시 제외
        if (isDepositTransaction(text)) {
            Log.d(TAG, "💰 Deposit detected - excluding from transactions")
            return ParsedTransaction(null, null, null, 0.0, "Deposit transaction excluded")
        }
        
        // Phase 1: 은행별 특화 패턴 우선 시도
        val bankSpecificResult = tryBankSpecificParsing(text, packageName)
        if (bankSpecificResult.confidence >= HIGH_CONFIDENCE_THRESHOLD) {
            Log.d(TAG, "✅ Bank-specific parsing succeeded: confidence=${bankSpecificResult.confidence}")
            return bankSpecificResult
        }
        
        // Phase 2: 다중 패턴 매칭
        val multiPatternResult = tryMultiPatternParsing(text)
        if (multiPatternResult.confidence >= HIGH_CONFIDENCE_THRESHOLD) {
            Log.d(TAG, "✅ Multi-pattern parsing succeeded: confidence=${multiPatternResult.confidence}")
            return multiPatternResult
        }
        
        // Phase 3: 최적 결과 선택
        val bestResult = if (bankSpecificResult.confidence > multiPatternResult.confidence) 
            bankSpecificResult else multiPatternResult
            
        Log.d(TAG, "🔄 Best result selected: confidence=${bestResult.confidence}")
        return bestResult
    }
    
    /**
     * 입출금 구분 - 입금 거래 감지
     */
    private fun isDepositTransaction(text: String): Boolean {
        val depositKeywords = arrayOf(
            "입금", "송금받기", "이체받기", "받기", "입출금통장입금", 
            "예금입금", "저축입금", "적금입금", "보험금입금",
            "환급", "상환", "지급", "급여", "연금", "배당",
            "캐시백", "리워드", "포인트적립", "적립금",
            "환불", "취소", "반환"
        )
        
        val withdrawalKeywords = arrayOf(
            "출금", "지출", "결제", "이체", "송금", "승인",
            "스마트폰출금", "ATM출금", "현금출금", "자동이체",
            "카드승인", "체크카드", "신용카드", "직불카드"
        )
        
        // 입금 키워드 확인
        for (keyword in depositKeywords) {
            if (text.contains(keyword)) {
                Log.d(TAG, "🔍 Deposit keyword found: '$keyword'")
                return true
            }
        }
        
        // 출금 키워드가 있으면 출금으로 판단
        for (keyword in withdrawalKeywords) {
            if (text.contains(keyword)) {
                Log.d(TAG, "🔍 Withdrawal keyword found: '$keyword'")
                return false
            }
        }
        
        // 금액 패턴으로 추가 판단
        if (text.contains("잔액증가") || text.contains("잔고증가") || 
            text.contains(Regex("\\+[0-9,]+원"))) {
            Log.d(TAG, "🔍 Balance increase pattern detected")
            return true
        }
        
        if (text.contains("잔액감소") || text.contains("잔고감소") || 
            text.contains(Regex("\\-[0-9,]+원"))) {
            Log.d(TAG, "🔍 Balance decrease pattern detected")
            return false
        }
        
        // 기본값: 애매한 경우 출금으로 처리 (기존 동작 유지)
        Log.d(TAG, "🤷 Unable to determine transaction type, defaulting to withdrawal")
        return false
    }
    
    /**
     * 은행별 특화 패턴 파싱
     */
    private fun tryBankSpecificParsing(text: String, packageName: String): ParsedTransaction {
        return when {
            packageName.contains("kbstar") -> parseKBPattern(text)
            packageName.contains("shinhan") -> parseShinhanPattern(text)
            packageName.contains("toss") -> parseTossPattern(text)
            packageName.contains("woori") -> parseWooriPattern(text)
            packageName.contains("kakao") -> parseKakaoPattern(text)
            else -> ParsedTransaction(null, null, null, 0.0, "No bank-specific pattern")
        }
    }
    
    /**
     * KB국민은행 특화 파싱 - 실제 알림 패턴 기반
     */
    private fun parseKBPattern(text: String): ParsedTransaction {
        Log.d(TAG, "🏦 Trying KB-specific patterns")
        
        val kbPatterns = arrayOf(
            // 실제 패턴: "이*혁님 08/25 19:39 941602-**-***064 이수혁 스마트폰출금 1 잔액73,349"
            Triple(
                Pattern.compile("([가-힣*]+님)\\s+\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}\\s+[0-9\\-*]+\\s+([가-힣]+)\\s+(스마트폰출금|ATM출금|이체)\\s+([0-9,]+)\\s*잔액"),
                0.95,
                "KB Standard Pattern"
            ),
            // 간소화 패턴: "이수혁 스마트폰출금 1"
            Triple(
                Pattern.compile("([가-힣]+)\\s+(스마트폰출금|ATM출금|이체|현금출금)\\s+([0-9,]+)"),
                0.90,
                "KB Simplified Pattern"
            ),
            // 제목 기반 패턴: "출금 1원" + 내용 파싱
            Triple(
                Pattern.compile("출금\\s+([0-9,]+)원"),
                0.80,
                "KB Title Pattern"
            )
        )
        
        for ((pattern, baseConfidence, patternName) in kbPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                var amount: Long? = null
                var merchant: String? = null
                val transactionType = "출금"
                
                when (matcher.groupCount()) {
                    4 -> { // Full KB pattern
                        merchant = matcher.group(2)?.trim()
                        amount = matcher.group(4)?.replace(",", "")?.toLongOrNull()
                        Log.d(TAG, "💡 KB Full pattern matched: merchant='$merchant', amount=$amount")
                    }
                    3 -> { // Simplified KB pattern
                        merchant = matcher.group(1)?.trim()
                        amount = matcher.group(3)?.replace(",", "")?.toLongOrNull()
                        Log.d(TAG, "💡 KB Simplified pattern matched: merchant='$merchant', amount=$amount")
                    }
                    1 -> { // Title pattern - need to extract merchant from full text
                        amount = matcher.group(1)?.replace(",", "")?.toLongOrNull()
                        merchant = extractKBMerchantFromFullText(text)
                        Log.d(TAG, "💡 KB Title pattern matched: merchant='$merchant', amount=$amount")
                    }
                }
                
                if (amount != null && amount > 0) {
                    val confidence = if (merchant != null && merchant != "알 수 없음") 
                        baseConfidence else baseConfidence * 0.7
                    
                    return ParsedTransaction(
                        amount = amount,
                        merchant = merchant ?: "알 수 없음",
                        transactionType = transactionType,
                        confidence = confidence,
                        details = "$patternName: ${matcher.group()}"
                    )
                }
            }
        }
        
        return ParsedTransaction(null, null, null, 0.0, "No KB pattern matched")
    }
    
    /**
     * KB 특화 merchant 추출
     */
    private fun extractKBMerchantFromFullText(text: String): String? {
        // KB 텍스트에서 계좌번호 패턴 이후의 한글 이름 추출
        val merchantPatterns = arrayOf(
            Pattern.compile("[0-9]{6}-[*\\-]{2,3}[*\\-]{3}[0-9]{3}\\s+([가-힣]+)\\s+(스마트폰출금|ATM출금)"),
            Pattern.compile("\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}\\s+[0-9\\-*]+\\s+([가-힣]+)\\s+"),
            Pattern.compile("([가-힣]{2,6})\\s+(스마트폰출금|ATM출금|이체|현금출금)")
        )
        
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (candidate != null && candidate.length >= 2 && !candidate.endsWith("님")) {
                    Log.d(TAG, "🎯 KB merchant extracted: '$candidate'")
                    return candidate
                }
            }
        }
        
        return null
    }
    
    /**
     * 신한은행 특화 파싱
     */
    private fun parseShinhanPattern(text: String): ParsedTransaction {
        Log.d(TAG, "🏦 Trying Shinhan-specific patterns")
        
        val shinhanPatterns = arrayOf(
            Triple(
                Pattern.compile("\\[([가-힣a-zA-Z\\s]+)\\]\\s*([0-9,]+)원.*승인"),
                0.90,
                "Shinhan Bracket Pattern"
            ),
            Triple(
                Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+([0-9,]+)원.*승인"),
                0.85,
                "Shinhan Approval Pattern"
            )
        )
        
        return tryPatternsWithConfidence(shinhanPatterns, text, "결제")
    }
    
    /**
     * 토스 특화 파싱
     */
    private fun parseTossPattern(text: String): ParsedTransaction {
        Log.d(TAG, "🏦 Trying Toss-specific patterns")
        
        val tossPatterns = arrayOf(
            Triple(
                Pattern.compile("([가-힣a-zA-Z\\s]+)에서\\s+([0-9,]+)원\\s+결제"),
                0.90,
                "Toss Payment Pattern"
            ),
            Triple(
                Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+([0-9,]+)원\\s+결제"),
                0.85,
                "Toss Simple Pattern"
            )
        )
        
        return tryPatternsWithConfidence(tossPatterns, text, "결제")
    }
    
    /**
     * 우리은행 특화 파싱
     */
    private fun parseWooriPattern(text: String): ParsedTransaction {
        Log.d(TAG, "🏦 Trying Woori-specific patterns")
        
        val wooriPatterns = arrayOf(
            Triple(
                Pattern.compile("ATM\\s+([가-힣a-zA-Z\\s]+)\\s+([0-9,]+)원"),
                0.90,
                "Woori ATM Pattern"
            ),
            Triple(
                Pattern.compile("현금출금\\s+([0-9,]+)원"),
                0.80,
                "Woori Withdrawal Pattern"
            )
        )
        
        return tryPatternsWithConfidence(wooriPatterns, text, "출금")
    }
    
    /**
     * 카카오페이 특화 파싱
     */
    private fun parseKakaoPattern(text: String): ParsedTransaction {
        Log.d(TAG, "🏦 Trying Kakao-specific patterns")
        
        val kakaoPatterns = arrayOf(
            Triple(
                Pattern.compile("([가-힣a-zA-Z\\s]+)에서\\s+([0-9,]+)원.*카카오페이"),
                0.90,
                "Kakao Payment Pattern"
            )
        )
        
        return tryPatternsWithConfidence(kakaoPatterns, text, "결제")
    }
    
    /**
     * 다중 패턴 시도 헬퍼 함수
     */
    private fun tryPatternsWithConfidence(
        patterns: Array<Triple<Pattern, Double, String>>, 
        text: String, 
        defaultTransactionType: String
    ): ParsedTransaction {
        for ((pattern, baseConfidence, patternName) in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val merchant = if (matcher.groupCount() >= 1) matcher.group(1)?.trim() else null
                val amount = if (matcher.groupCount() >= 2) 
                    matcher.group(2)?.replace(",", "")?.toLongOrNull() 
                else 
                    matcher.group(1)?.replace(",", "")?.toLongOrNull()
                
                if (amount != null && amount > 0) {
                    val confidence = if (merchant != null && merchant.length >= 2) 
                        baseConfidence else baseConfidence * 0.8
                    
                    return ParsedTransaction(
                        amount = amount,
                        merchant = merchant ?: "알 수 없음",
                        transactionType = defaultTransactionType,
                        confidence = confidence,
                        details = "$patternName: ${matcher.group()}"
                    )
                }
            }
        }
        
        return ParsedTransaction(null, null, null, 0.0, "No pattern matched")
    }
    
    /**
     * 다중 패턴 매칭 (은행 불특정)
     */
    private fun tryMultiPatternParsing(text: String): ParsedTransaction {
        Log.d(TAG, "🔄 Trying multi-pattern parsing")
        
        val generalPatterns = arrayOf(
            Triple(
                Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+([0-9,]+)원\\s*(출금|결제|이체|승인)"),
                0.75,
                "General Pattern 1"
            ),
            Triple(
                Pattern.compile("(출금|결제|이체|승인)\\s+([0-9,]+)원\\s+([가-힣a-zA-Z\\s]+)"),
                0.75,
                "General Pattern 2"
            ),
            Triple(
                Pattern.compile("([0-9,]+)원.*?([가-힣a-zA-Z\\s]{2,})"),
                0.60,
                "General Pattern 3"
            )
        )
        
        for ((pattern, baseConfidence, patternName) in generalPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                var amount: Long? = null
                var merchant: String? = null
                var transactionType = "출금"
                
                when (matcher.groupCount()) {
                    3 -> {
                        if (matcher.group(2)?.matches(Regex("[0-9,]+")) == true) {
                            merchant = matcher.group(1)?.trim()
                            amount = matcher.group(2)?.replace(",", "")?.toLongOrNull()
                            transactionType = matcher.group(3) ?: "출금"
                        } else {
                            transactionType = matcher.group(1) ?: "출금"
                            amount = matcher.group(2)?.replace(",", "")?.toLongOrNull()
                            merchant = matcher.group(3)?.trim()
                        }
                    }
                    2 -> {
                        amount = matcher.group(1)?.replace(",", "")?.toLongOrNull()
                        merchant = matcher.group(2)?.trim()
                    }
                }
                
                if (amount != null && amount > 0) {
                    // 상인명 품질 검증
                    if (merchant != null && merchant.length >= 2 && 
                        !merchant.matches(Regex("(출금|결제|이체|승인|은행|카드|페이|님)"))) {
                        
                        return ParsedTransaction(
                            amount = amount,
                            merchant = merchant,
                            transactionType = transactionType,
                            confidence = baseConfidence,
                            details = "$patternName: ${matcher.group()}"
                        )
                    }
                }
            }
        }
        
        return ParsedTransaction(null, null, null, 0.0, "No general pattern matched")
    }
    
    private fun getAppName(packageName: String): String {
        return when {
            packageName.contains("kbstar") -> "KB국민은행"
            packageName.contains("shinhan") -> "신한은행"
            packageName.contains("toss") -> "토스"
            packageName.contains("woori") -> "우리은행"
            packageName.contains("kakao") -> "카카오페이"
            else -> packageName
        }
    }
}