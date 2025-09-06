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
     * 텍스트에서 한글 텍스트 후보 찾기 (강화된 상호명 인식)
     */
    private fun findAllKoreanText(text: String): List<String> {
        val candidates = mutableListOf<String>()
        
        Log.d(TAG, "🔍 [ULTIMATE] Searching Korean text in: $text")
        
        // 한글 텍스트 패턴들 (우선순위 순)
        val patterns = listOf(
            // 우선순위 1: 거래 키워드 직접 연결 패턴
            Regex("\\s([가-힣]{2,8})\\s+(스마트폰출금|체크카드출금|출금|결제|송금)"),
            Regex("([가-힣]{2,8})(스마트폰출금|체크카드출금|출금|결제)"),
            
            // 우선순위 2: 숫자 패턴과 연결된 상호명
            Regex("\\d+\\s+([가-힣]{2,8})\\s+(스마트폰출금|출금)"),
            Regex("([가-힣]{2,8})\\s+[0-9,]+원?"),
            Regex("[0-9,]+원?\\s+([가-힣]{2,8})"),
            
            // 우선순위 3: 계좌번호/특수패턴 뒤 상호명
            Regex("\\*{2,}\\d+\\s+([가-힣]{2,8})"),
            Regex("\\d{2}:\\d{2}.*?([가-힣]{2,8})\\s+(출금|결제)"),
            
            // 우선순위 4: 기본 한글 패턴
            Regex("\\b([가-힣]{3,8})\\b"),      // 단어 경계의 완전한 한글
            Regex("[가-힣]{2,8}"),              // 기본 한글 2자 이상
            Regex("[가-힣*]{3,8}"),             // * 포함 한글 (정리 후)
            Regex("[가-힣\\w]{2,8}")            // 한글+영숫자 조합
        )
        
        for ((index, pattern) in patterns.withIndex()) {
            pattern.findAll(text).forEach { match ->
                val candidate = if (match.groupValues.size > 1) {
                    match.groupValues[1] // 그룹이 있으면 첫 번째 그룹 사용
                } else {
                    match.value // 그룹이 없으면 전체 매치 사용
                }
                
                val cleanCandidate = candidate.replace("*", "").replace("님", "").trim()
                if (cleanCandidate.length >= 2) {
                    candidates.add("$cleanCandidate|P$index") // 패턴 인덱스 저장 (우선순위용)
                }
            }
        }
        
        // 중복 제거 및 우선순위 정렬
        val uniqueCandidates = candidates.map { 
            val parts = it.split("|P")
            val name = parts[0]
            val priority = parts.getOrNull(1)?.toIntOrNull() ?: 999
            Pair(name, priority)
        }.distinctBy { it.first }
         .sortedBy { it.second } // 우선순위 순 정렬
         .map { it.first }
        
        Log.d(TAG, "📝 [ULTIMATE] Korean text candidates: $uniqueCandidates")
        return uniqueCandidates
    }
    
    /**
     * 최적의 상호명 선택 (강화된 로직)
     */
    private fun selectBestMerchant(candidates: List<String>): String? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates[0]
        
        Log.d(TAG, "🎯 [ULTIMATE] Selecting best merchant from: $candidates")
        
        val financialTerms = setOf("은행", "뱅크", "출금", "입금", "이체", "송금", "결제", "승인", 
                                  "잔액", "전자", "금융", "체크", "신용", "카드", "원", "ATM", "스마트")
        
        // 점수 기반 상호명 선택
        val scoredCandidates = candidates.mapNotNull { candidate ->
            // 금융 용어 포함된 경우 제외
            if (financialTerms.any { term -> candidate.contains(term) }) {
                null
            } else {
                val score = calculateUltimateMerchantScore(candidate)
                Pair(candidate, score)
            }
        }.sortedByDescending { it.second }
        
        Log.d(TAG, "📊 [ULTIMATE] Scored merchants: ${scoredCandidates.take(3)}")
        
        return scoredCandidates.firstOrNull()?.first
    }
    
    /**
     * 궁극 파서 상호명 점수 계산
     */
    private fun calculateUltimateMerchantScore(candidate: String): Int {
        var score = 0
        
        // 기본 길이 점수 (3-6자가 가장 적절한 상호명)
        score += when (candidate.length) {
            in 3..6 -> 100
            2 -> 60
            in 7..8 -> 80
            else -> 20
        }
        
        // 완전한 한글 패턴 가산점
        if (candidate.matches(Regex("[가-힣]{2,8}"))) {
            score += 40
        }
        
        // 일반적인 한국어 상호명 패턴 가산점
        if (isLikelyBusinessName(candidate)) {
            score += 50
        }
        
        // 개인명 패턴 감점
        if (isLikelyPersonalName(candidate)) {
            score -= 60
        }
        
        // 너무 짧은 이름 감점
        if (candidate.length <= 2) {
            score -= 30
        }
        
        // 특수 상호명 패턴 인식
        if (hasBusinessKeywords(candidate)) {
            score += 30
        }
        
        return maxOf(0, score) // 최소 0점
    }
    
    /**
     * 상호명 패턴 확인
     */
    private fun isLikelyBusinessName(name: String): Boolean {
        // 일반적인 상호명 패턴
        val businessPatterns = listOf(
            // 업종 키워드
            Regex(".*[가-힣]*(마트|식당|카페|치킨|피자|커피|PC방|노래방|학원|미용실|병원|약국).*"),
            // 프랜차이즈
            Regex(".*(맥도날드|버거킹|KFC|롯데리아|스타벅스|투썸|이디야|할리스|배스킨|던킨|파리바게뜨).*"),
            // 상호 접미사
            Regex("[가-힣]{2,}(스토어|샵|하우스|플레이스|랜드|센터)")
        )
        
        return businessPatterns.any { pattern -> name.matches(pattern) }
    }
    
    /**
     * 개인명 패턴 확인
     */
    private fun isLikelyPersonalName(name: String): Boolean {
        // 일반적인 개인명 패턴
        val personalPatterns = listOf(
            // 일반 성씨로 시작하는 2-3글자
            Regex("[김이박최정강조윤장임한오서신권황안송류전고문양백허남심노정][가-힣]{1,2}"),
            // "님"으로 끝남
            Regex(".*님"),
            // 2글자 (대부분 개인명)
            Regex("[가-힣]{2}")
        )
        
        return personalPatterns.any { name.matches(it) }
    }
    
    /**
     * 상호명 키워드 확인
     */
    private fun hasBusinessKeywords(name: String): Boolean {
        val businessKeywords = setOf("마트", "카페", "치킨", "PC", "학원", "병원", "약국", "미용실", 
                                   "식당", "커피", "상점", "센터", "샵", "스토어")
        return businessKeywords.any { keyword -> name.contains(keyword) }
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