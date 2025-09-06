package kr.klr.stopusing

import android.content.Context
import android.util.Log
import kr.klr.stopusing.ai.TensorFlowLiteManager
import kr.klr.stopusing.ai.NerResultProcessor
import kr.klr.stopusing.config.KoreanFinancialVocabulary
import kr.klr.stopusing.data.TransactionParseResult
import kr.klr.stopusing.text.PerfectKoreanTokenizer
import kr.klr.stopusing.fallback.RegexTransactionParser
import kr.klr.stopusing.fallback.UltimateFallbackParser

/**
 * AI 기반 거래 알림 파싱 엔진
 * 
 * TensorFlow Lite 모델을 사용하여 한국어 금융 알림에서
 * 거래 정보(금액, 가맹점, 거래유형)를 추출합니다.
 * 
 * 주요 컴포넌트:
 * - TensorFlowLiteManager: 모델 관리
 * - PerfectKoreanTokenizer: 완벽한 문자 레벨 토큰화
 * - NerResultProcessor: NER 결과 해석
 */
class AITransactionParser(private val context: Context) {
    
    companion object {
        private const val TAG = "AITransactionParser"
    }
    
    // 핵심 컴포넌트들
    private val modelManager = TensorFlowLiteManager(context)
    private val tokenizer = PerfectKoreanTokenizer()
    private val resultProcessor = NerResultProcessor()
    private val fallbackParser = RegexTransactionParser()
    private val ultimateFallbackParser = UltimateFallbackParser()
    
    init {
        initializeParser()
    }
    
    /**
     * 파서 초기화
     */
    private fun initializeParser() {
        Log.d(TAG, "🚀 Initializing AI Transaction Parser...")
        
        val success = modelManager.initializeModel()
        if (success) {
            modelManager.logModelInfo()
            Log.d(TAG, "✅ AI Transaction Parser initialized successfully")
        } else {
            Log.e(TAG, "❌ Failed to initialize AI Transaction Parser")
        }
    }
    
    /**
     * 거래 알림 텍스트 파싱
     * 
     * @param text 알림 텍스트
     * @param packageName 앱 패키지명
     * @return 파싱 결과
     */
    fun parseTransaction(text: String, packageName: String): TransactionParseResult {
        Log.d(TAG, "🤖 Starting AI parsing for: $text")
        Log.d(TAG, "📱 Package: $packageName")
        
        if (!modelManager.isInitialized()) {
            Log.e(TAG, "💥 Model not initialized - switching to FALLBACK")
            return fallbackParser.parseTransaction(text, packageName)
        }
        
        return try {
            // 1. 텍스트 토큰화
            Log.d(TAG, "🔤 Step 1: Tokenizing text...")
            val tokens = tokenizer.tokenizeText(text)
            val inputBuffer = tokenizer.createInputBuffer(tokens)
            
            // 2. AI 모델 추론
            Log.d(TAG, "🧠 Step 2: Running AI inference...")
            val modelOutput = modelManager.runInference(inputBuffer)
            if (modelOutput == null) {
                Log.e(TAG, "💥 Model inference returned null - switching to FALLBACK")
                return fallbackParser.parseTransaction(text, packageName)
            }
            
            Log.d(TAG, "📊 Model output shape: [${modelOutput.size}, ${modelOutput[0].size}, ${modelOutput[0][0].size}]")
            
            // 3. 결과 처리 및 반환
            Log.d(TAG, "🔍 Step 3: Processing NER results...")
            val aiResult = resultProcessor.processNerOutput(modelOutput, text, packageName)
            
            // AI 결과 품질 검증 및 fallback 결정
            val finalResult = validateAndEnhanceResult(aiResult, text, packageName)
            
            logParsingResult(finalResult, text)
            finalResult
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Parsing error - switching to FALLBACK", e)
            Log.e(TAG, "📱 Stack trace:", e)
            fallbackParser.parseTransaction(text, packageName)
        }
    }
    
    /**
     * 파싱 결과 로깅
     */
    private fun logParsingResult(result: TransactionParseResult, originalText: String) {
        if (result.isSuccessful()) {
            Log.d(TAG, "✅ Parsing successful: ${result.getSummary()}")
            if (result.isHighConfidence()) {
                Log.d(TAG, "🎯 High confidence result")
            } else {
                Log.w(TAG, "⚠️ Low confidence result")
            }
        } else {
            Log.w(TAG, "❌ Parsing failed for: '$originalText'")
            Log.w(TAG, "🔍 Details: ${result.details}")
        }
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
            method = "AI_ERROR",
            details = error
        )
    }
    
    /**
     * 파서 정보 반환
     */
    fun getParserInfo(): Map<String, Any> {
        return mapOf(
            "modelInitialized" to modelManager.isInitialized(),
            "modelFile" to KoreanFinancialVocabulary.MODEL_FILE,
            "confidenceThreshold" to KoreanFinancialVocabulary.AI_CONFIDENCE_THRESHOLD,
            "maxSequenceLength" to KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH,
            "vocabularySize" to KoreanFinancialVocabulary.VOCAB_SIZE
        )
    }
    
    /**
     * AI 결과 품질 검증 및 fallback 보강
     */
    private fun validateAndEnhanceResult(
        aiResult: TransactionParseResult, 
        text: String, 
        packageName: String
    ): TransactionParseResult {
        
        // AI 성공 조건 검증 (더 엄격한 검증)
        val isAISuccessful = aiResult.isSuccessful() && 
                            aiResult.confidence >= KoreanFinancialVocabulary.AI_CONFIDENCE_THRESHOLD &&
                            aiResult.amount != null && aiResult.amount!! > 0 &&
                            !aiResult.merchant.isNullOrBlank()
        
        if (isAISuccessful) {
            Log.d(TAG, "✅ AI parsing successful with high confidence: ${aiResult.confidence}")
            return aiResult
        }
        
        // AI 실패 또는 낮은 신뢰도 - fallback 사용
        Log.w(TAG, "⚠️ AI result quality low (confidence: ${aiResult.confidence}) - using FALLBACK")
        Log.w(TAG, "🔄 AI Result: amount=${aiResult.amount}, merchant=${aiResult.merchant}")
        
        val fallbackResult = fallbackParser.parseTransaction(text, packageName)
        
        if (fallbackResult.isSuccessful()) {
            Log.d(TAG, "✅ FALLBACK successful: ${fallbackResult.getSummary()}")
            return fallbackResult
        }
        
        // Both AI and fallback failed - use hybrid approach
        Log.w(TAG, "⚠️ Both AI and FALLBACK had issues - using hybrid result")
        val hybridResult = createHybridResult(aiResult, fallbackResult, text)
        
        // 하이브리드 결과도 실패하면 궁극 백업 파서 사용
        if (!hybridResult.isSuccessful()) {
            Log.w(TAG, "⚠️ Hybrid result failed - trying ULTIMATE fallback")
            val ultimateResult = ultimateFallbackParser.parseAsLastResort(text, packageName)
            
            if (ultimateResult.isSuccessful()) {
                Log.d(TAG, "✅ ULTIMATE fallback succeeded: ${ultimateResult.getSummary()}")
                return ultimateResult
            }
            
            // 정말 모든 방법이 실패한 경우
            Log.e(TAG, "💥 ALL parsing methods failed - returning error result")
            return createErrorResult("All parsing methods (AI + Regex + Ultimate) failed", text)
        }
        
        return hybridResult
    }
    
    /**
     * AI와 fallback 결과를 결합한 최적 결과 생성
     */
    private fun createHybridResult(
        aiResult: TransactionParseResult,
        fallbackResult: TransactionParseResult, 
        text: String
    ): TransactionParseResult {
        
        // 최선의 정보를 조합 (우선순위: fallback > AI)
        val bestAmount = fallbackResult.amount?.takeIf { it > 0 } ?: aiResult.amount?.takeIf { it > 0 }
        val bestMerchant = fallbackResult.merchant?.takeIf { it != "알 수 없음" && it.isNotBlank() } 
                          ?: aiResult.merchant?.takeIf { it != "알 수 없음" && it.isNotBlank() } ?: "알 수 없음"
        val bestType = fallbackResult.transactionType?.takeIf { it.isNotBlank() } 
                      ?: aiResult.transactionType?.takeIf { it.isNotBlank() } ?: "출금"
        
        // 더 정교한 신뢰도 계산
        val hybridConfidence = when {
            bestAmount != null && bestAmount > 0 && bestMerchant != "알 수 없음" -> 0.75
            bestAmount != null && bestAmount > 0 -> 0.65
            bestMerchant != "알 수 없음" -> 0.50
            else -> 0.30
        }
        
        Log.d(TAG, "🔀 HYBRID result: amount=$bestAmount, merchant=$bestMerchant, confidence=$hybridConfidence")
        
        return TransactionParseResult(
            amount = bestAmount,
            merchant = bestMerchant,
            transactionType = bestType,
            confidence = hybridConfidence,
            method = "AI_FALLBACK_HYBRID",
            details = "Combined AI (${String.format("%.2f", aiResult.confidence)}) + Fallback (${String.format("%.2f", fallbackResult.confidence)}) result"
        )
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        Log.d(TAG, "🧹 Cleaning up AI Transaction Parser resources...")
        modelManager.cleanup()
    }
}