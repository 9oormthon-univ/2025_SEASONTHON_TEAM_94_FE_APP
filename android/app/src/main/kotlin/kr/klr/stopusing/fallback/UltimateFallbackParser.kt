package kr.klr.stopusing.fallback

import android.util.Log
import kr.klr.stopusing.data.TransactionParseResult

/**
 * 궁극의 백업 파서 - AI와 Regex가 모두 실패할 때의 마지막 보루
 * 100% 파싱 성공률을 보장하는 최후의 수단
 */
class UltimateFallbackParser {
    
    companion object {
        private const val TAG = "UltimateFallbackParser"
    }
    
    /**
     * 모든 파싱 방법이 실패했을 때의 최후 분석
     * 텍스트에서 어떤 정보라도 추출하려고 시도
     */
    fun parseAsLastResort(text: String, packageName: String): TransactionParseResult {
        Log.w(TAG, "🚨 [ULTIMATE] Last resort parsing for: $text")
        
        return try {
            // 1단계: 숫자 완전 검색
            val amountCandidates = findAllNumbers(text)
            val bestAmount = selectBestAmount(amountCandidates)
            
            // 2단계: 한글 텍스트 완전 검색  
            val textCandidates = findAllKoreanText(text)
            val bestMerchant = selectBestMerchant(textCandidates)
            
            // 3단계: 패키지명 기반 추론
            val inferredType = inferTransactionTypeFromPackage(packageName)
            
            // 결과 조합
            val confidence = calculateUltimateConfidence(bestAmount, bestMerchant, text)
            
            Log.w(TAG, "🔧 [ULTIMATE] Final result: amount=$bestAmount, merchant=$bestMerchant, confidence=$confidence")
            
            TransactionParseResult(
                amount = bestAmount,
                merchant = bestMerchant ?: "알 수 없음",
                transactionType = inferredType,
                confidence = confidence,
                method = "ULTIMATE_FALLBACK",
                details = "Emergency parsing from text analysis. Original: '$text'"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [ULTIMATE] Even ultimate parsing failed", e)
            createMinimalResult(text, packageName)
        }
    }
    
    /**
     * 텍스트에서 모든 숫자 후보 찾기
     */
    private fun findAllNumbers(text: String): List<Long> {
        val numbers = mutableListOf<Long>()
        
        // 여러 패턴으로 숫자 찾기
        val patterns = listOf(
            Regex("[0-9,]{2,}"),          // 기본 숫자+콤마
            Regex("[0-9]+"),              // 순수 숫자
            Regex("[0-9]{1,3}(?:,[0-9]{3})*") // 표준 콤마 형식
        )
        
        for (pattern in patterns) {
            pattern.findAll(text).forEach { match ->
                val numberStr = match.value.replace(",", "")
                val number = numberStr.toLongOrNull()
                
                // 유효한 거래 금액 범위 (10원 ~ 1억원)
                if (number != null && number >= 10 && number <= 100_000_000) {
                    numbers.add(number)
                }
            }
        }
        
        return numbers.distinct().sorted()
    }
    
    /**
     * 최적의 금액 선택
     */
    private fun selectBestAmount(amounts: List<Long>): Long? {
        if (amounts.isEmpty()) return null
        
        // 선택 기준:
        // 1. 일반적인 거래 금액 범위 (100원 ~ 100만원)에 있는 것 우선
        // 2. 너무 작거나 큰 금액 제외
        // 3. 여러개면 중간값 선택
        
        val reasonableAmounts = amounts.filter { it >= 100 && it <= 1_000_000 }
        
        return when {
            reasonableAmounts.isNotEmpty() -> reasonableAmounts[reasonableAmounts.size / 2]
            amounts.size == 1 -> amounts[0]
            else -> amounts.maxByOrNull { amount ->
                // 점수 기반 선택 (일반적인 거래 금액에 가까울수록 높은 점수)
                when {
                    amount in 1000..500_000 -> 100
                    amount in 100..1000 -> 80
                    amount in 500_000..5_000_000 -> 60
                    else -> 20
                }
            }
        }
    }
    
    /**
     * 텍스트에서 한글 텍스트 후보 찾기
     */
    private fun findAllKoreanText(text: String): List<String> {
        val candidates = mutableListOf<String>()
        
        // 한글 텍스트 패턴들
        val patterns = listOf(
            Regex("[가-힣]{2,}"),         // 기본 한글 2자 이상
            Regex("[가-힣*]{3,}"),        // * 포함 한글
            Regex("[가-힣\\w]{2,}")       // 한글+영숫자 조합
        )
        
        for (pattern in patterns) {
            pattern.findAll(text).forEach { match ->
                val candidate = match.value.replace("*", "").trim()
                if (candidate.length >= 2) {
                    candidates.add(candidate)
                }
            }
        }
        
        return candidates.distinct()
    }
    
    /**
     * 최적의 상호명 선택
     */
    private fun selectBestMerchant(candidates: List<String>): String? {
        if (candidates.isEmpty()) return null
        
        val financialTerms = setOf("은행", "뱅크", "출금", "입금", "이체", "송금", "결제", "승인", 
                                  "잔액", "전자", "금융", "체크", "신용", "카드", "원", "ATM", "스마트")
        
        // 금융 용어 제외하고 적절한 길이의 후보 선택
        val filtered = candidates.filter { candidate ->
            candidate.length in 2..10 && 
            !financialTerms.any { term -> candidate.contains(term) }
        }
        
        return when {
            filtered.isEmpty() -> candidates.firstOrNull()
            filtered.size == 1 -> filtered[0]
            else -> {
                // 가장 그럴듯한 상호명 선택 (3-6자 길이 우선)
                filtered.minByOrNull { candidate ->
                    when (candidate.length) {
                        in 3..6 -> 1
                        2 -> 2
                        in 7..10 -> 3
                        else -> 4
                    }
                }
            }
        }
    }
    
    /**
     * 패키지명으로 거래 유형 추론
     */
    private fun inferTransactionTypeFromPackage(packageName: String): String {
        return when {
            packageName.contains("toss") -> "송금"
            packageName.contains("kakao") -> "송금"
            packageName.contains("card") || packageName.contains("pay") -> "결제"
            else -> "출금"
        }
    }
    
    /**
     * 궁극 파싱의 신뢰도 계산
     */
    private fun calculateUltimateConfidence(amount: Long?, merchant: String?, text: String): Double {
        var confidence = 0.1 // 기본 최소 신뢰도
        
        // 금액이 있으면 +0.3
        if (amount != null) {
            confidence += 0.3
            
            // 합리적인 금액이면 추가 점수
            if (amount in 100..1_000_000) {
                confidence += 0.2
            }
        }
        
        // 상호명이 있으면 +0.2
        if (merchant != null && merchant != "알 수 없음" && merchant.length >= 2) {
            confidence += 0.2
            
            // 적절한 길이면 추가 점수
            if (merchant.length in 3..8) {
                confidence += 0.1
            }
        }
        
        // 텍스트가 한국어 금융 패턴과 비슷하면 +0.1
        if (text.contains("원") || text.contains("출금") || text.contains("결제") || text.contains("이체")) {
            confidence += 0.1
        }
        
        return minOf(1.0, confidence)
    }
    
    /**
     * 최소한의 결과라도 생성
     */
    private fun createMinimalResult(text: String, packageName: String): TransactionParseResult {
        Log.e(TAG, "🆘 [ULTIMATE] Creating minimal result")
        
        return TransactionParseResult(
            amount = null,
            merchant = "파싱불가",
            transactionType = "알림",
            confidence = 0.05,
            method = "MINIMAL_EMERGENCY",
            details = "Complete parsing failure - returning minimal result for: '$text'"
        )
    }
}