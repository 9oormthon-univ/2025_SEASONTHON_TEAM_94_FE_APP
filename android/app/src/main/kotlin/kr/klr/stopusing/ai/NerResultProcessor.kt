package kr.klr.stopusing.ai

import android.util.Log
import kr.klr.stopusing.config.KoreanFinancialVocabulary
import kr.klr.stopusing.data.TransactionParseResult
import kr.klr.stopusing.text.PerfectKoreanTokenizer

/**
 * 범용 NER(Named Entity Recognition) 결과 처리 클래스
 * 문자 레벨 AI 모델의 출력을 해석하여 거래 정보를 추출
 */
class NerResultProcessor {
    
    companion object {
        private const val TAG = "NerResultProcessor"
    }
    
    private val tokenizer = PerfectKoreanTokenizer()
    
    /**
     * 범용 NER 모델 출력을 거래 정보로 해석
     * 
     * @param outputBuffer 모델 출력 [1, 200, 7]
     * @param originalText 원본 텍스트
     * @param packageName 앱 패키지명
     * @return 파싱 결과
     */
    fun processNerOutput(
        outputBuffer: Array<Array<FloatArray>>, 
        originalText: String,
        packageName: String
    ): TransactionParseResult {
        
        Log.d(TAG, "🔍 Processing universal NER output for: $originalText")
        
        val predictions = outputBuffer[0] // [200, 7]
        val entities = extractEntitiesFromCharLevel(predictions, originalText)
        val avgConfidence = calculateAverageConfidence(predictions)
        
        // 패턴 매칭으로 보완 (fallback)
        val patternAmount = tokenizer.extractAmountPattern(originalText)
        val patternMerchant = tokenizer.extractMerchantPattern(originalText)
        val patternType = determineTransactionType(originalText)
        
        // AI 결과와 패턴 매칭 결합
        val finalAmount = entities.amount ?: patternAmount
        val finalMerchant = entities.merchant ?: patternMerchant ?: "알 수 없음"
        val finalType = entities.transactionType ?: patternType ?: "출금"
        
        Log.d(TAG, "📊 Universal NER Results - Amount: $finalAmount, Merchant: $finalMerchant, Type: $finalType")
        Log.d(TAG, "🎯 Confidence: ${String.format("%.3f", avgConfidence)}")
        
        return TransactionParseResult(
            amount = finalAmount,
            merchant = finalMerchant,
            transactionType = finalType,
            confidence = avgConfidence,
            method = "UNIVERSAL_AI_NER",
            details = "Character-level NER with ${entities.extractedEntities.size} entities"
        )
    }
    
    /**
     * 문자 레벨 NER 예측에서 엔티티 추출 (개선된 버전)
     */
    private fun extractEntitiesFromCharLevel(predictions: Array<FloatArray>, originalText: String): EntityExtractionResult {
        val extractedEntities = mutableListOf<String>()
        var amount: Long? = null
        var merchant: String? = null
        var transactionType: String? = null
        
        var currentEntity = StringBuilder()
        var currentEntityType = -1
        
        // 안전한 텍스트 길이 처리
        val textLength = minOf(originalText.length, predictions.size, KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH)
        
        for (i in 0 until textLength) {
            val char = originalText[i]
            val tokenScores = predictions[i]
            
            // 안전한 예측 레이블 계산
            val predictedLabel = if (tokenScores.isNotEmpty()) {
                tokenScores.indices.maxByOrNull { tokenScores[it] } ?: 0
            } else {
                0 // OUTSIDE
            }
            
            // 신뢰도 임계값 적용 (0.3 이상인 경우만 신뢰)
            val maxScore = tokenScores.maxOrNull() ?: 0.0f
            val finalLabel = if (maxScore >= 0.3f) predictedLabel else 0
            
            when (finalLabel) {
                KoreanFinancialVocabulary.NerLabels.BEGIN_AMOUNT -> {
                    finishCurrentEntity(currentEntity.toString(), currentEntityType, extractedEntities)
                    currentEntity = StringBuilder()
                    currentEntity.append(char)
                    currentEntityType = KoreanFinancialVocabulary.NerLabels.BEGIN_AMOUNT
                }
                KoreanFinancialVocabulary.NerLabels.INSIDE_AMOUNT -> {
                    if (currentEntityType == KoreanFinancialVocabulary.NerLabels.BEGIN_AMOUNT || 
                        currentEntityType == KoreanFinancialVocabulary.NerLabels.INSIDE_AMOUNT) {
                        currentEntity.append(char)
                        currentEntityType = KoreanFinancialVocabulary.NerLabels.INSIDE_AMOUNT
                    } else {
                        // B- 태그 없이 I- 태그가 나온 경우 무시
                        finishCurrentEntity(currentEntity.toString(), currentEntityType, extractedEntities)
                        currentEntity = StringBuilder()
                        currentEntityType = -1
                    }
                }
                KoreanFinancialVocabulary.NerLabels.BEGIN_MERCHANT -> {
                    finishCurrentEntity(currentEntity.toString(), currentEntityType, extractedEntities)
                    currentEntity = StringBuilder()
                    currentEntity.append(char)
                    currentEntityType = KoreanFinancialVocabulary.NerLabels.BEGIN_MERCHANT
                }
                KoreanFinancialVocabulary.NerLabels.INSIDE_MERCHANT -> {
                    if (currentEntityType == KoreanFinancialVocabulary.NerLabels.BEGIN_MERCHANT || 
                        currentEntityType == KoreanFinancialVocabulary.NerLabels.INSIDE_MERCHANT) {
                        currentEntity.append(char)
                        currentEntityType = KoreanFinancialVocabulary.NerLabels.INSIDE_MERCHANT
                    } else {
                        // B- 태그 없이 I- 태그가 나온 경우 무시
                        finishCurrentEntity(currentEntity.toString(), currentEntityType, extractedEntities)
                        currentEntity = StringBuilder()
                        currentEntityType = -1
                    }
                }
                KoreanFinancialVocabulary.NerLabels.BEGIN_DATE -> {
                    finishCurrentEntity(currentEntity.toString(), currentEntityType, extractedEntities)
                    currentEntity = StringBuilder()
                    currentEntity.append(char)
                    currentEntityType = KoreanFinancialVocabulary.NerLabels.BEGIN_DATE
                }
                KoreanFinancialVocabulary.NerLabels.INSIDE_DATE -> {
                    if (currentEntityType == KoreanFinancialVocabulary.NerLabels.BEGIN_DATE || 
                        currentEntityType == KoreanFinancialVocabulary.NerLabels.INSIDE_DATE) {
                        currentEntity.append(char)
                        currentEntityType = KoreanFinancialVocabulary.NerLabels.INSIDE_DATE
                    } else {
                        // B- 태그 없이 I- 태그가 나온 경우 무시
                        finishCurrentEntity(currentEntity.toString(), currentEntityType, extractedEntities)
                        currentEntity = StringBuilder()
                        currentEntityType = -1
                    }
                }
                else -> { // OUTSIDE
                    finishCurrentEntity(currentEntity.toString(), currentEntityType, extractedEntities)
                    currentEntity = StringBuilder()
                    currentEntityType = -1
                }
            }
        }
        
        // 마지막 엔티티 처리
        finishCurrentEntity(currentEntity.toString(), currentEntityType, extractedEntities)
        
        // 추출된 엔티티에서 실제 값들 파싱 (더 엄격한 검증)
        for (entity in extractedEntities) {
            val cleanEntity = entity.trim()
            if (cleanEntity.isEmpty() || cleanEntity.length < 2) continue
            
            // 금액 파싱 (숫자 + 콤마 패턴, 더 엄격한 검증)
            if (amount == null && cleanEntity.matches(Regex("[0-9,]+"))) {
                val parsedAmount = cleanEntity.replace(",", "").toLongOrNull()
                if (parsedAmount != null && parsedAmount > 0 && parsedAmount <= 10_000_000) { // 1천만원 이하만
                    amount = parsedAmount
                }
            }
            
            // 가맹점명 파싱 (한글 2글자 이상, 금융 키워드 제외)
            if (merchant == null && cleanEntity.matches(Regex("[가-힣]{2,}")) && 
                !isFinancialKeyword(cleanEntity)) {
                merchant = cleanEntity
            }
        }
        
        return EntityExtractionResult(
            amount = amount,
            merchant = merchant,
            transactionType = transactionType,
            extractedEntities = extractedEntities
        )
    }
    
    private fun finishCurrentEntity(entity: String, entityType: Int, extractedEntities: MutableList<String>) {
        if (entity.isNotEmpty() && entityType != -1) {
            extractedEntities.add(entity)
            Log.d(TAG, "🏷️ Extracted entity: '$entity' (type: ${KoreanFinancialVocabulary.NerLabels.LABEL_NAMES[entityType]})")
        }
    }
    
    /**
     * 거래 유형 판단 (입금/출금)
     */
    private fun determineTransactionType(text: String): String {
        return when {
            text.contains("입금") && !text.contains("출금") -> "입금"
            else -> "출금"
        }
    }
    
    /**
     * 평균 신뢰도 계산 (개선된 버전)
     */
    private fun calculateAverageConfidence(predictions: Array<FloatArray>): Double {
        var totalConfidence = 0.0
        var count = 0
        var highConfidenceCount = 0
        
        for (tokenScores in predictions) {
            if (tokenScores.isEmpty()) continue
            
            val maxScore = tokenScores.maxOrNull() ?: 0.0f
            totalConfidence += maxScore
            count++
            
            // 높은 신뢰도 토큰 개수도 세어봄
            if (maxScore >= 0.7f) {
                highConfidenceCount++
            }
        }
        
        if (count == 0) return 0.0
        
        val avgConfidence = totalConfidence / count
        
        // 높은 신뢰도 토큰 비율로 가중치 적용
        val highConfidenceRatio = highConfidenceCount.toDouble() / count
        val weightedConfidence = avgConfidence * (0.7 + 0.3 * highConfidenceRatio)
        
        return minOf(1.0, weightedConfidence) // 최대 1.0으로 제한
    }
    
    /**
     * 금융 키워드 확인 (가맹점명에서 제외해야 할 단어들)
     */
    private fun isFinancialKeyword(text: String): Boolean {
        val keywords = setOf("은행", "뱅크", "출금", "입금", "이체", "송금", "결제", "승인", "잔액", 
                           "전자", "금융", "체크", "신용", "카드", "원", "님", "매수", "매도", "ATM")
        return keywords.any { text.contains(it) }
    }
    
    /**
     * 엔티티 추출 결과를 담는 데이터 클래스
     */
    private data class EntityExtractionResult(
        val amount: Long?,
        val merchant: String?,
        val transactionType: String?,
        val extractedEntities: List<String>
    )
}