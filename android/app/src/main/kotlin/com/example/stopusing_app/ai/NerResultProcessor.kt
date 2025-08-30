package com.example.stopusing_app.ai

import android.util.Log
import com.example.stopusing_app.config.KoreanFinancialVocabulary
import com.example.stopusing_app.data.TransactionParseResult
import com.example.stopusing_app.text.PerfectKoreanTokenizer

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
     * 문자 레벨 NER 예측에서 엔티티 추출
     */
    private fun extractEntitiesFromCharLevel(predictions: Array<FloatArray>, originalText: String): EntityExtractionResult {
        val extractedEntities = mutableListOf<String>()
        var amount: Long? = null
        var merchant: String? = null
        var transactionType: String? = null
        
        var currentEntity = StringBuilder()
        var currentEntityType = -1
        
        // 문자별로 NER 레이블 분석 (텍스트 길이만큼만 처리)
        val textLength = minOf(originalText.length, predictions.size)
        
        for (i in 0 until textLength) {
            val char = originalText[i]
            val tokenScores = predictions[i]
            val predictedLabel = tokenScores.indices.maxByOrNull { tokenScores[it] } ?: 0
            
            when (predictedLabel) {
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
        
        // 추출된 엔티티에서 실제 값들 파싱
        for (entity in extractedEntities) {
            val cleanEntity = entity.trim()
            if (cleanEntity.isEmpty()) continue
            
            // 금액 파싱 (숫자 + 콤마 패턴)
            if (amount == null && cleanEntity.matches(Regex("[0-9,]+"))) {
                amount = cleanEntity.replace(",", "").toLongOrNull()
            }
            
            // 가맹점명 파싱 (한글 2글자 이상)
            if (merchant == null && cleanEntity.matches(Regex("[가-힣]{2,}"))) {
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
     * 평균 신뢰도 계산
     */
    private fun calculateAverageConfidence(predictions: Array<FloatArray>): Double {
        var totalConfidence = 0.0
        var count = 0
        
        for (tokenScores in predictions) {
            val maxScore = tokenScores.maxOrNull() ?: 0.0f
            totalConfidence += maxScore
            count++
        }
        
        return if (count > 0) totalConfidence / count else 0.0
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