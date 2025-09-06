package kr.klr.stopusing.fallback

import android.util.Log
import kr.klr.stopusing.data.TransactionParseResult
import java.util.regex.Pattern

/**
 * 완벽한 Regex 기반 Fallback 파서
 * AI 모델 실패시 100% 복원력을 위한 강력한 패턴 매칭
 */
class RegexTransactionParser {
    
    companion object {
        private const val TAG = "RegexFallbackParser"
    }
    
    /**
     * 한국 금융앱별 완벽한 패턴 매칭 파싱
     */
    fun parseTransaction(text: String, packageName: String): TransactionParseResult {
        Log.d(TAG, "🔄 [FALLBACK] Starting regex parsing for: $text")
        Log.d(TAG, "📱 [FALLBACK] Package: $packageName")
        
        return try {
            val bankType = determineBankType(packageName)
            val result = when (bankType) {
                BankType.KB -> parseKBBank(text)
                BankType.SHINHAN -> parseShinhanBank(text)
                BankType.WOORI -> parseWooriBank(text)
                BankType.KAKAO -> parseKakaoBank(text)
                BankType.TOSS -> parseToss(text)
                BankType.NH -> parseNHBank(text)
                BankType.HANA -> parseHanaBank(text)
                BankType.IBK -> parseIBKBank(text)
                BankType.CARD_COMPANY -> parseCardCompany(text)
                BankType.SECURITIES -> parseSecurities(text)
                BankType.OTHER -> parseGeneric(text)
            }
            
            Log.d(TAG, "✅ [FALLBACK] Regex parsing success: ${result.getSummary()}")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [FALLBACK] Regex parsing failed", e)
            createErrorResult("Regex fallback failed: ${e.message}", text)
        }
    }
    
    /**
     * KB국민은행 패턴 (개선된 버전)
     */
    private fun parseKBBank(text: String): TransactionParseResult {
        Log.d(TAG, "🏦 [KB] Parsing KB Bank: $text")
        
        // KB패턴: "24,400원 역전할머니맥주 입출금(8608) 09.01 00:01 잔액 234,592원"
        val patterns = listOf(
            // 기본 패턴: 금액 + 상호명 + 거래유형
            Pattern.compile("([0-9,]+)원\\s+([가-힣\\w*]+)\\s+(입출금|스마트폰출금|ATM출금|체크카드출금)"),
            // 체크카드 패턴: 상호명 + 체크카드출금 + 금액  
            Pattern.compile("([가-힣\\w*]+)\\s+체크카드출금\\s+([0-9,]+)"),
            // 역순 패턴: 상호명 + 금액
            Pattern.compile("([가-힣]{2,})\\s+([0-9,]+)원"),
            // 간단 패턴: 금액 + 상호명
            Pattern.compile("([0-9,]+)원\\s+([가-힣]{2,})")
        )
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                var amount: Long? = null
                var merchant = "알 수 없음"
                
                // 더 안전한 그룹 파싱
                for (i in 1..matcher.groupCount()) {
                    val group = matcher.group(i) ?: continue
                    
                    // 금액 찾기
                    if (amount == null && group.matches(Regex("[0-9,]+"))) {
                        amount = extractNumber(group)
                    }
                    // 상호명 찾기 (금융 키워드 제외)
                    else if (merchant == "알 수 없음" && group.matches(Regex("[가-힣\\w*]{2,}")) && 
                             !isFinancialKeyword(group)) {
                        merchant = group
                    }
                }
                
                if (amount != null && amount > 0) {
                    Log.d(TAG, "✅ [KB] Success: amount=$amount, merchant=$merchant")
                    return createSuccessResult(amount, merchant, determineTransactionType(text), "KB_REGEX")
                }
            }
        }
        
        Log.d(TAG, "⚠️ [KB] No match, fallback to generic")
        return parseGeneric(text)
    }
    
    /**
     * 신한은행 패턴
     */
    private fun parseShinhanBank(text: String): TransactionParseResult {
        // 신한패턴: "이*혁님 08/31 17:09 941602-**-***64 바이브PC 체크카드출금 2,000 잔액35,033"
        val patterns = listOf(
            // 기본 패턴 (이름님 패턴)
            Pattern.compile("([가-힣*]+님).*?([가-힣\\w]+)\\s+(체크카드출금|출금|결제)\\s+([0-9,]+)"),
            // 역순 패턴
            Pattern.compile("([0-9,]+)\\s+([가-힣]+)\\s+(체크카드출금|출금)"),
            // 간단 패턴
            Pattern.compile("([가-힣*]+님).*?([0-9,]+)")
        )
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val groups = (1..matcher.groupCount()).map { matcher.group(it) ?: "" }
                
                val amount = groups.firstOrNull { it.matches(Regex("[0-9,]+")) }?.let { extractNumber(it) }
                val merchant = groups.firstOrNull { 
                    it.matches(Regex("[가-힣\\w*]+")) && !it.contains("출금") && !it.contains("결제") 
                } ?: "알 수 없음"
                
                return createSuccessResult(amount, merchant, determineTransactionType(text), "SHINHAN_REGEX")
            }
        }
        
        return parseGeneric(text)
    }
    
    /**
     * 우리은행 패턴
     */
    private fun parseWooriBank(text: String): TransactionParseResult {
        val patterns = listOf(
            Pattern.compile("([0-9,]+)원.*?([가-힣]+).*?(출금|결제|이체)"),
            Pattern.compile("([가-힣]+).*?(출금|결제).*?([0-9,]+)원")
        )
        
        return parseWithPatterns(patterns, text, "WOORI_REGEX")
    }
    
    /**
     * 카카오뱅크 패턴
     */
    private fun parseKakaoBank(text: String): TransactionParseResult {
        val patterns = listOf(
            Pattern.compile("([0-9,]+)원\\s+(출금|송금).*?([가-힣]+)"),
            Pattern.compile("([가-힣]+).*?([0-9,]+)원.*?(송금|출금)")
        )
        
        return parseWithPatterns(patterns, text, "KAKAO_REGEX")
    }
    
    /**
     * 토스 패턴
     */
    private fun parseToss(text: String): TransactionParseResult {
        val patterns = listOf(
            Pattern.compile("([가-힣]+).*?([0-9,]+)원.*?(결제|송금)"),
            Pattern.compile("([0-9,]+)원.*?([가-힣]+).*?(결제|송금)")
        )
        
        return parseWithPatterns(patterns, text, "TOSS_REGEX")
    }
    
    /**
     * NH농협은행 패턴
     */
    private fun parseNHBank(text: String): TransactionParseResult {
        val patterns = listOf(
            Pattern.compile("([0-9,]+)원.*?([가-힣]+).*?(출금|이체)"),
            Pattern.compile("([가-힣]+).*?([0-9,]+)원.*?(출금|이체)")
        )
        
        return parseWithPatterns(patterns, text, "NH_REGEX")
    }
    
    /**
     * 하나은행 패턴
     */
    private fun parseHanaBank(text: String): TransactionParseResult {
        val patterns = listOf(
            Pattern.compile("([0-9,]+)원.*?([가-힣]+).*?(출금|결제)"),
            Pattern.compile("([가-힣]+).*?([0-9,]+)원.*?(출금|결제)")
        )
        
        return parseWithPatterns(patterns, text, "HANA_REGEX")
    }
    
    /**
     * IBK기업은행 패턴
     */
    private fun parseIBKBank(text: String): TransactionParseResult {
        val patterns = listOf(
            Pattern.compile("([0-9,]+)원.*?([가-힣]+).*?(출금|이체)"),
            Pattern.compile("([가-힣]+).*?([0-9,]+)원.*?(출금|이체)")
        )
        
        return parseWithPatterns(patterns, text, "IBK_REGEX")
    }
    
    /**
     * 카드사 패턴
     */
    private fun parseCardCompany(text: String): TransactionParseResult {
        val patterns = listOf(
            Pattern.compile("([가-힣\\w]+).*?([0-9,]+)원.*?(결제|승인)"),
            Pattern.compile("([0-9,]+)원.*?([가-힣\\w]+).*?(결제|승인)"),
            Pattern.compile("(체크|신용)카드.*?([가-힣\\w]+).*?([0-9,]+)원")
        )
        
        return parseWithPatterns(patterns, text, "CARD_REGEX")
    }
    
    /**
     * 증권사 패턴
     */
    private fun parseSecurities(text: String): TransactionParseResult {
        val patterns = listOf(
            Pattern.compile("([가-힣]+).*?([0-9,]+)원.*?(매수|매도|출금)"),
            Pattern.compile("([0-9,]+)원.*?([가-힣]+).*?(매수|매도|출금)")
        )
        
        return parseWithPatterns(patterns, text, "SECURITIES_REGEX")
    }
    
    /**
     * 범용 패턴 (최후의 수단, 개선된 버전)
     */
    private fun parseGeneric(text: String): TransactionParseResult {
        Log.d(TAG, "🔄 [GENERIC] Using generic patterns for: $text")
        
        // 더 포괄적인 패턴들 (우선순위 순서)
        val patterns = listOf(
            // 한국어 금융 패턴들
            Pattern.compile("([가-힣]{2,}).*?([0-9,]+)원.*?(출금|결제|이체|송금)"),
            Pattern.compile("([0-9,]+)원.*?([가-힣]{2,}).*?(출금|결제|이체|송금)"),
            // 기본 금액 + 상호명 패턴
            Pattern.compile("([0-9,]+)원?\\s+([가-힣\\w*]{2,})"),
            // 상호명 + 금액 패턴
            Pattern.compile("([가-힣\\w*]{2,})\\s+([0-9,]+)원?"),
            // 복합 패턴 (더 넓은 범위)
            Pattern.compile("([가-힣\\w*]{2,}).*?([0-9,]+)"),
            Pattern.compile("([0-9,]+).*?([가-힣\\w*]{2,})"),
            // 최후의 수단: 숫자만
            Pattern.compile("([0-9,]+)원?")
        )
        
        for ((index, pattern) in patterns.withIndex()) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                Log.d(TAG, "📝 [GENERIC] Pattern $index matched")
                
                var amount: Long? = null
                var merchant = "알 수 없음"
                
                // 모든 그룹에서 정보 추출
                for (i in 1..matcher.groupCount()) {
                    val group = matcher.group(i) ?: continue
                    if (group.isBlank()) continue
                    
                    // 금액 찾기
                    if (amount == null && group.matches(Regex("[0-9,]+"))) {
                        val parsed = extractNumber(group)
                        if (parsed != null && parsed > 0 && parsed <= 50_000_000) { // 5천만원 이하 (더 관대하게)
                            amount = parsed
                        }
                    }
                    // 상호명 찾기
                    else if (merchant == "알 수 없음" && 
                             group.matches(Regex("[가-힣\\w*]{2,}")) && 
                             !isFinancialKeyword(group)) {
                        merchant = group
                    }
                }
                
                // 금액이 있으면 성공으로 처리 (상호명이 없어도)
                if (amount != null && amount > 0) {
                    Log.d(TAG, "✅ [GENERIC] Success: amount=$amount, merchant=$merchant")
                    return createSuccessResult(amount, merchant, determineTransactionType(text), "GENERIC_REGEX")
                }
            }
        }
        
        // 정말 최후의 수단: 정규식으로 금액만이라도 찾기
        val lastResortAmount = Regex("([0-9,]{2,})").find(text)?.groupValues?.get(1)?.let { extractNumber(it) }
        if (lastResortAmount != null && lastResortAmount > 0) {
            Log.d(TAG, "⚠️ [GENERIC] Last resort: found amount only: $lastResortAmount")
            return createSuccessResult(lastResortAmount, "알 수 없음", "출금", "LAST_RESORT_REGEX")
        }
        
        Log.e(TAG, "❌ [GENERIC] All patterns failed")
        return createErrorResult("No patterns matched after exhaustive search", text)
    }
    
    /**
     * 패턴 그룹으로 파싱하는 헬퍼 (개선된 버전)
     */
    private fun parseWithPatterns(patterns: List<Pattern>, text: String, method: String): TransactionParseResult {
        Log.d(TAG, "🔍 [$method] Trying patterns for: $text")
        
        for ((index, pattern) in patterns.withIndex()) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val groups = (1..matcher.groupCount()).map { matcher.group(it) ?: "" }
                Log.d(TAG, "📝 [$method] Pattern $index matched: $groups")
                
                var amount: Long? = null
                var merchant = "알 수 없음"
                
                // 더 안전한 추출 로직
                for (group in groups) {
                    if (group.isBlank()) continue
                    
                    // 금액 찾기 (더 엄격한 검증)
                    if (amount == null && group.matches(Regex("[0-9,]+"))) {
                        val parsed = extractNumber(group)
                        if (parsed != null && parsed > 0 && parsed <= 10_000_000) { // 1천만원 이하
                            amount = parsed
                        }
                    }
                    // 상호명 찾기 (금융 키워드 제외, 최소 2글자)
                    else if (merchant == "알 수 없음" && 
                             group.matches(Regex("[가-힣\\w*]{2,}")) && 
                             !isFinancialKeyword(group)) {
                        merchant = group
                    }
                }
                
                if (amount != null && amount > 0) {
                    Log.d(TAG, "✅ [$method] Success: amount=$amount, merchant=$merchant")
                    return createSuccessResult(amount, merchant, determineTransactionType(text), method)
                }
            }
        }
        
        Log.d(TAG, "⚠️ [$method] No patterns matched, fallback to generic")
        return parseGeneric(text)
    }
    
    /**
     * 은행 타입 결정
     */
    private fun determineBankType(packageName: String): BankType {
        return when {
            packageName.contains("kbstar") -> BankType.KB
            packageName.contains("shinhan") -> BankType.SHINHAN
            packageName.contains("wooribank") -> BankType.WOORI
            packageName.contains("kakaobank") -> BankType.KAKAO
            packageName.contains("toss") -> BankType.TOSS
            packageName.contains("nh") -> BankType.NH
            packageName.contains("hana") -> BankType.HANA
            packageName.contains("ibk") -> BankType.IBK
            packageName.contains("card") || packageName.contains("pay") -> BankType.CARD_COMPANY
            packageName.contains("sec") || packageName.contains("trade") -> BankType.SECURITIES
            else -> BankType.OTHER
        }
    }
    
    /**
     * 거래 유형 판단
     */
    private fun determineTransactionType(text: String): String {
        return when {
            text.contains("입금") && !text.contains("출금") -> "입금"
            text.contains("송금") -> "송금"
            text.contains("이체") -> "이체"
            text.contains("결제") || text.contains("승인") -> "결제"
            text.contains("출금") -> "출금"
            text.contains("매수") -> "매수"
            text.contains("매도") -> "매도"
            else -> "출금"
        }
    }
    
    /**
     * 숫자 추출
     */
    private fun extractNumber(text: String): Long? {
        return text.replace(",", "").replace("원", "").toLongOrNull()
    }
    
    /**
     * 금융 키워드 제외
     */
    private fun isFinancialKeyword(text: String): Boolean {
        val keywords = setOf("은행", "뱅크", "출금", "입금", "이체", "송금", "결제", "승인", "잔액", 
                           "전자", "금융", "체크", "신용", "카드", "원", "님", "매수", "매도")
        return keywords.any { text.contains(it) }
    }
    
    /**
     * 성공 결과 생성
     */
    private fun createSuccessResult(amount: Long?, merchant: String, type: String, method: String): TransactionParseResult {
        return TransactionParseResult(
            amount = amount,
            merchant = merchant,
            transactionType = type,
            confidence = 0.85, // Regex fallback 신뢰도
            method = method,
            details = "Regex pattern matching successful"
        )
    }
    
    /**
     * 오류 결과 생성
     */
    private fun createErrorResult(error: String, originalText: String): TransactionParseResult {
        return TransactionParseResult(
            amount = null,
            merchant = null,
            transactionType = null,
            confidence = 0.0,
            method = "REGEX_FAILED",
            details = error
        )
    }
    
    /**
     * 은행 타입 열거형
     */
    private enum class BankType {
        KB, SHINHAN, WOORI, KAKAO, TOSS, NH, HANA, IBK, CARD_COMPANY, SECURITIES, OTHER
    }
}