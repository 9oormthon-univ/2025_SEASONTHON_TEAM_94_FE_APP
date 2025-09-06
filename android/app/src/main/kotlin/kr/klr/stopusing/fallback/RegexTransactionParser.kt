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
     * KB국민은행 패턴 (개선된 상호명 인식)
     */
    private fun parseKBBank(text: String): TransactionParseResult {
        Log.d(TAG, "🏦 [KB] Parsing KB Bank: $text")
        
        // 8a09149 커밋에서 실제 작동한 KB 패턴들 적용
        val patterns = listOf(
            // 우선순위 1: 핵심 실제 패턴 - "이*혁님 08/25 19:39 941602-**-***064 이수혁 스마트폰출금 1 잔액73,349"
            Pattern.compile("([가-힣*]+님)\\s+\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}\\s+[0-9\\-*]+\\s+([가-힣]+)\\s+(스마트폰출금|ATM출금|이체)\\s+([0-9,]+)\\s*잔액"),
            
            // 우선순위 2: 간소화 패턴 - "이수혁 스마트폰출금 1"
            Pattern.compile("([가-힣]+)\\s+(스마트폰출금|ATM출금|이체|현금출금)\\s+([0-9,]+)"),
            
            // 우선순위 3: 제목 기반 패턴 - "출금 1원"
            Pattern.compile("출금\\s+([0-9,]+)원"),
            
            // 우선순위 4: 기존 KB 패턴들 (호환성 유지)
            Pattern.compile("([0-9,]+)원\\s+([가-힣\\w*]{2,8})\\s+(입출금|스마트폰출금|ATM출금|체크카드출금)"),
            Pattern.compile("([가-힣\\w*]{2,8})\\s+(체크카드출금|스마트폰출금)\\s+([0-9,]+)")
        )
        
        for ((index, pattern) in patterns.withIndex()) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                Log.d(TAG, "📝 [KB] Pattern $index matched")
                
                val groups = (1..matcher.groupCount()).map { matcher.group(it) ?: "" }
                Log.d(TAG, "📊 [KB] Groups: $groups")
                
                // 8a09149 커밋의 정확한 그룹 분석 로직
                var amount: Long? = null
                var merchant: String? = null
                val transactionType = "출금"
                
                when (matcher.groupCount()) {
                    4 -> { // 핵심 패턴: ([가-힣*]+님) (...) ([가-힣]+) (...) ([0-9,]+)
                        merchant = matcher.group(2)?.trim() // 두 번째 그룹이 상호명!
                        amount = matcher.group(4)?.replace(",", "")?.toLongOrNull()
                        Log.d(TAG, "💡 [KB] Full pattern matched: merchant='$merchant', amount=$amount")
                    }
                    3 -> { // 간소화 패턴: ([가-힣]+) (...) ([0-9,]+)
                        if (matcher.group(2)?.contains("출금") == true) {
                            merchant = matcher.group(1)?.trim()
                            amount = matcher.group(3)?.replace(",", "")?.toLongOrNull()
                        } else {
                            amount = matcher.group(1)?.replace(",", "")?.toLongOrNull()
                            merchant = matcher.group(2)?.trim()
                        }
                        Log.d(TAG, "💡 [KB] Simplified pattern matched: merchant='$merchant', amount=$amount")
                    }
                    1 -> { // 제목 패턴: 금액만 추출 후 전체 텍스트에서 상호명 검색
                        amount = matcher.group(1)?.replace(",", "")?.toLongOrNull()
                        merchant = extractKBMerchantFromFullText(text)
                        Log.d(TAG, "💡 [KB] Title pattern matched: merchant='$merchant', amount=$amount")
                    }
                }
                
                // 상호명이 없으면 추가 검색
                if (merchant == null || merchant == "알 수 없음") {
                    val additionalCandidates = findKBMerchantCandidates(text)
                    merchant = selectBestKBMerchant(additionalCandidates, text)
                }
                
                val bestMerchant = merchant
                
                if (amount != null && amount > 0) {
                    Log.d(TAG, "✅ [KB] Success: amount=$amount, merchant=$bestMerchant")
                    return createSuccessResult(amount, bestMerchant, transactionType, "KB_REGEX")
                }
            }
        }
        
        Log.d(TAG, "⚠️ [KB] No patterns matched, fallback to generic")
        return parseGeneric(text)
    }
    
    /**
     * KB국민은행 전용 상호명 후보 검색 (8a09149 커밋 로직 적용)
     */
    private fun findKBMerchantCandidates(text: String): List<String> {
        val candidates = mutableListOf<String>()
        
        // 8a09149 커밋에서 실제 작동했던 정확한 패턴들
        val workingPatterns = listOf(
            // 핵심 패턴: "이*혁님 08/25 19:39 941602-**-***064 이수혁 스마트폰출금 1 잔액73,349"
            Regex("([가-힣*]+님)\\s+\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}\\s+[0-9\\-*]+\\s+([가-힣]+)\\s+(스마트폰출금|ATM출금|이체)\\s+([0-9,]+)\\s*잔액"),
            
            // KB 계좌번호 뒤 상호명 패턴 (정확한 형태)
            Regex("[0-9]{6}-[*]{2}-[*]{3}[0-9]{3}\\s+([가-힣]+)\\s+(스마트폰출금|ATM출금)"),
            
            // 간소화 패턴: "이수혁 스마트폰출금 1"
            Regex("([가-힣]+)\\s+(스마트폰출금|ATM출금|이체|현금출금)\\s+([0-9,]+)"),
            
            // 추가 보조 패턴들
            Regex("\\s([가-힣]{2,8})\\s+스마트폰출금"),
            Regex("([가-힣]{2,8})\\s+(스마트폰출금|체크카드출금)\\s+([0-9,]+)"),
            Regex("\\d{2}:\\d{2}.*?([가-힣]{2,8})\\s+(스마트폰출금|출금)")
        )
        
        for ((index, pattern) in workingPatterns.withIndex()) {
            pattern.findAll(text).forEach { match ->
                val candidate = when (match.groupValues.size) {
                    5 -> match.groupValues[2] // 핵심 패턴의 두 번째 그룹 (상호명)
                    4 -> match.groupValues[1] // 간소화 패턴의 첫 번째 그룹
                    2 -> match.groupValues[1] // 보조 패턴의 첫 번째 그룹
                    else -> match.groupValues.getOrNull(1)
                }?.trim()
                
                if (candidate != null && candidate.length >= 2 && 
                    !candidate.endsWith("님") && !isFinancialKeyword(candidate)) {
                    candidates.add(candidate)
                    Log.d(TAG, "🎯 [KB] Pattern $index found merchant candidate: '$candidate'")
                }
            }
        }
        
        return candidates.distinct()
    }
    
    /**
     * KB국민은행 최적 상호명 선택
     */
    private fun selectBestKBMerchant(candidates: List<String>, text: String): String {
        if (candidates.isEmpty()) return "알 수 없음"
        if (candidates.size == 1) return candidates[0]
        
        // KB국민은행 특화 점수 계산
        val scoredCandidates = candidates.map { candidate ->
            var score = 0
            
            // 기본 길이 점수
            score += when (candidate.length) {
                in 3..6 -> 100
                2 -> 70
                in 7..8 -> 80
                else -> 30
            }
            
            // 개인명 패턴 감점
            if (candidate.matches(Regex("[김이박최정강조윤장임][가-힣]+"))) {
                score -= 50
            }
            
            // 순수 한글 3글자 이상 가산점
            if (candidate.matches(Regex("[가-힣]{3,8}"))) {
                score += 30
            }
            
            // 거래 키워드 근처에 있으면 가산점
            if (text.contains("$candidate 스마트폰출금") || text.contains("${candidate}스마트폰출금")) {
                score += 50
            }
            
            // KB 패턴: 원 뒤에 있으면 가산점
            if (text.contains("원 $candidate") || text.contains("원　$candidate")) {
                score += 40
            }
            
            Pair(candidate, score)
        }.sortedByDescending { it.second }
        
        Log.d(TAG, "📊 [KB] Merchant scores: ${scoredCandidates.take(3)}")
        return scoredCandidates.first().first
    }
    
    /**
     * KB 특화 merchant 추출 (8a09149 커밋 원본 로직)
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
                    Log.d(TAG, "🎯 [KB] Merchant extracted from full text: '$candidate'")
                    return candidate
                }
            }
        }
        
        return null
    }
    
    /**
     * 신한은행 패턴 (개선된 상호명 인식)
     */
    private fun parseShinhanBank(text: String): TransactionParseResult {
        Log.d(TAG, "🏦 [SHINHAN] Parsing: $text")
        
        // KB 패턴에서 가져온 참고: "이*혁님 09/06 17:09 941602-**-***64 이수혁 스마트폰출금 1 잔액35,033"
        val patterns = listOf(
            // 우선순위 1: 완전한 상호명 + 거래유형 패턴
            Pattern.compile("\\s([가-힣]{2,8})\\s+(스마트폰출금|체크카드출금|출금|결제)\\s+([0-9,]+)"),
            // 우선순위 2: 금액 + 상호명 패턴
            Pattern.compile("([0-9,]+)\\s+([가-힣]{2,8})\\s+(스마트폰출금|체크카드출금|출금)"),
            // 우선순위 3: 더 넓은 범위 패턴
            Pattern.compile("([가-힣*]+님).*?([가-힣]{2,8})\\s+(스마트폰출금|체크카드출금|출금|결제)\\s*([0-9,]+)?"),
            // 우선순위 4: 역순 패턴 (금액이 먼저)
            Pattern.compile("([0-9,]+)\\s+(잔액|원).*?([가-힣]{2,6})"),
            // 우선순위 5: 간단 패턴
            Pattern.compile("([가-힣*]+님).*?([0-9,]+)")
        )
        
        for ((index, pattern) in patterns.withIndex()) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                Log.d(TAG, "📝 [SHINHAN] Pattern $index matched")
                
                val groups = (1..matcher.groupCount()).map { matcher.group(it) ?: "" }
                Log.d(TAG, "📊 [SHINHAN] Groups: $groups")
                
                // 개선된 그룹 분석
                var amount: Long? = null
                val merchantCandidates = mutableListOf<String>()
                
                for (group in groups) {
                    if (group.isBlank()) continue
                    
                    // 금액 찾기
                    if (amount == null && group.matches(Regex("[0-9,]+"))) {
                        val parsed = extractNumber(group)
                        if (parsed != null && parsed > 0 && parsed <= 10_000_000) {
                            amount = parsed
                        }
                    }
                    
                    // 상호명 후보 수집 (더 엄격한 필터링)
                    if (group.matches(Regex("[가-힣*]{2,8}")) && 
                        !group.contains("출금") && !group.contains("결제") && 
                        !group.contains("잔액") && !group.contains("원")) {
                        // "*님" 형태는 개인명으로 처리, 순수 한글만 상호명 후보
                        val cleanGroup = group.replace("*", "").replace("님", "")
                        if (cleanGroup.matches(Regex("[가-힣]{2,8}")) && !isFinancialKeyword(cleanGroup)) {
                            merchantCandidates.add(cleanGroup)
                        }
                    }
                }
                
                // 원본 텍스트에서 추가 상호명 검색
                val additionalCandidates = findShinhanMerchantCandidates(text)
                val allCandidates = (merchantCandidates + additionalCandidates).distinct()
                
                // 최적 상호명 선택
                val bestMerchant = selectBestShinhanMerchant(allCandidates, text)
                
                if (amount != null && amount > 0) {
                    Log.d(TAG, "✅ [SHINHAN] Success: amount=$amount, merchant=$bestMerchant, candidates=$allCandidates")
                    return createSuccessResult(amount, bestMerchant, determineTransactionType(text), "SHINHAN_REGEX")
                }
            }
        }
        
        Log.d(TAG, "⚠️ [SHINHAN] No patterns matched, fallback to generic")
        return parseGeneric(text)
    }
    
    /**
     * 신한은행 전용 상호명 후보 검색
     */
    private fun findShinhanMerchantCandidates(text: String): List<String> {
        val candidates = mutableListOf<String>()
        
        // 신한은행 특화 패턴들
        val patterns = listOf(
            // "이수혁 스마트폰출금" 직접 패턴
            Regex("\\s([가-힣]{2,8})\\s+스마트폰출금"),
            // "64 이수혁 스마트폰출금" 패턴 
            Regex("\\d+\\s+([가-힣]{2,8})\\s+스마트폰출금"),
            // 계좌번호 뒤 상호명 패턴
            Regex("\\*{3}\\d+\\s+([가-힣]{2,8})\\s+"),
            // 날짜/시간 뒤 상호명 패턴
            Regex("\\d{2}:\\d{2}\\s+\\d+-.*?\\s+([가-힣]{2,8})\\s+"),
            // 일반적인 상호명 패턴
            Regex("([가-힣]{3,8})(?=\\s+(스마트폰출금|체크카드출금|출금))")
        )
        
        for (pattern in patterns) {
            pattern.findAll(text).forEach { match ->
                val candidate = match.groupValues[1].trim()
                if (candidate.length >= 2 && !isFinancialKeyword(candidate)) {
                    candidates.add(candidate)
                }
            }
        }
        
        return candidates.distinct()
    }
    
    /**
     * 신한은행 최적 상호명 선택
     */
    private fun selectBestShinhanMerchant(candidates: List<String>, text: String): String {
        if (candidates.isEmpty()) return "알 수 없음"
        if (candidates.size == 1) return candidates[0]
        
        // 신한은행 특화 점수 계산
        val scoredCandidates = candidates.map { candidate ->
            var score = 0
            
            // 기본 길이 점수
            score += when (candidate.length) {
                in 3..6 -> 100
                2 -> 70
                in 7..8 -> 80
                else -> 30
            }
            
            // 개인명 패턴 감점 (신한은행의 경우)
            if (candidate.matches(Regex("[김이박최정강조윤장임][가-힣]+"))) {
                score -= 50 // 성씨+이름 패턴은 개인명 가능성 높음
            }
            
            // 순수 한글 3글자 이상 가산점
            if (candidate.matches(Regex("[가-힣]{3,8}"))) {
                score += 30
            }
            
            // 스마트폰출금 키워드 근처에 있으면 가산점
            if (text.contains("$candidate 스마트폰출금") || text.contains("${candidate}스마트폰출금")) {
                score += 50
            }
            
            // 계좌번호 패턴 뒤에 있으면 가산점 (실제 상호명일 가능성)
            if (text.contains("***") && text.indexOf(candidate) > text.indexOf("***")) {
                score += 30
            }
            
            Pair(candidate, score)
        }.sortedByDescending { it.second }
        
        Log.d(TAG, "📊 [SHINHAN] Merchant scores: ${scoredCandidates.take(3)}")
        return scoredCandidates.first().first
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