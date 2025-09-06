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
 * 고급 거래 알림 파싱 엔진
 * 
 * 한국어 금융 알림에서 고급 분석을 통해
 * 거래 정보(금액, 가맹점, 거래유형)를 추출합니다.
 * 
 * 주요 컴포넌트:
 * - TensorFlowLiteManager: 고급 분석 모델 관리
 * - PerfectKoreanTokenizer: 완벽한 문자 레벨 토큰화
 * - NerResultProcessor: 분석 결과 해석
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
        Log.d(TAG, "🚀 Initializing Advanced Transaction Parser...")
        
        val success = modelManager.initializeModel()
        if (success) {
            modelManager.logModelInfo()
            Log.d(TAG, "✅ Advanced Transaction Parser initialized successfully")
        } else {
            Log.e(TAG, "❌ Failed to initialize Advanced Transaction Parser")
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
        Log.d(TAG, "📊 Starting transaction parsing for: $text")
        Log.d(TAG, "📱 Package: $packageName")
        
        // 고급 분석 엔진이 비활성화되면 바로 fallback으로
        if (!KoreanFinancialVocabulary.ENABLE_ADVANCED_ANALYTICS) {
            Log.d(TAG, "🔄 Using standard processing mode")
            return fallbackParser.parseTransaction(text, packageName)
        }
        
        if (!modelManager.isInitialized()) {
            Log.e(TAG, "💥 Processing engine unavailable - switching to standard mode")
            return fallbackParser.parseTransaction(text, packageName)
        }
        
        return try {
            // 1. 텍스트 토큰화
            Log.d(TAG, "🔤 Step 1: Tokenizing text...")
            val tokens = tokenizer.tokenizeText(text)
            val inputBuffer = tokenizer.createInputBuffer(tokens)
            
            // 2. 고급 분석 처리
            Log.d(TAG, "🧠 Step 2: Running advanced processing...")
            val modelOutput = modelManager.runInference(inputBuffer)
            if (modelOutput == null) {
                Log.e(TAG, "💥 Advanced processing failed - switching to standard mode")
                return fallbackParser.parseTransaction(text, packageName)
            }
            
            Log.d(TAG, "📊 Model output shape: [${modelOutput.size}, ${modelOutput[0].size}, ${modelOutput[0][0].size}]")
            
            // 3. 결과 처리 및 반환
            Log.d(TAG, "🔍 Step 3: Processing analysis results...")
            val analysisResult = resultProcessor.processNerOutput(modelOutput, text, packageName)
            
            // 분석 결과 품질 검증 및 fallback 결정
            val finalResult = validateAndEnhanceResult(analysisResult, text, packageName)
            
            logParsingResult(finalResult, text)
            finalResult
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Processing error - switching to standard mode", e)
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
            method = "PROCESSING_ERROR",
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
            "confidenceThreshold" to KoreanFinancialVocabulary.ANALYSIS_CONFIDENCE_THRESHOLD,
            "maxSequenceLength" to KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH,
            "vocabularySize" to KoreanFinancialVocabulary.VOCAB_SIZE
        )
    }
    
    /**
     * 분석 결과 품질 검증 및 fallback 보강
     */
    private fun validateAndEnhanceResult(
        analysisResult: TransactionParseResult, 
        text: String, 
        packageName: String
    ): TransactionParseResult {
        
        // 고급 분석 성공 조건 검증 (더 엄격한 검증)
        val isAnalysisSuccessful = analysisResult.isSuccessful() && 
                            analysisResult.confidence >= KoreanFinancialVocabulary.ANALYSIS_CONFIDENCE_THRESHOLD &&
                            analysisResult.amount != null && analysisResult.amount!! > 0 &&
                            !analysisResult.merchant.isNullOrBlank()
        
        if (isAnalysisSuccessful) {
            Log.d(TAG, "✅ Advanced parsing successful with high confidence: ${analysisResult.confidence}")
            return analysisResult
        }
        
        // 고급 분석 실패 또는 낮은 신뢰도 - fallback 사용
        Log.w(TAG, "⚠️ Analysis result quality low (confidence: ${analysisResult.confidence}) - using standard mode")
        Log.w(TAG, "🔄 Analysis Result: amount=${analysisResult.amount}, merchant=${analysisResult.merchant}")
        
        val fallbackResult = fallbackParser.parseTransaction(text, packageName)
        
        if (fallbackResult.isSuccessful()) {
            Log.d(TAG, "✅ Standard parsing successful: ${fallbackResult.getSummary()}")
            return fallbackResult
        }
        
        // Both advanced analysis and standard fallback failed - use hybrid approach
        Log.w(TAG, "⚠️ Both advanced and standard parsing had issues - using hybrid result")
        val hybridResult = createHybridResult(analysisResult, fallbackResult, text)
        
        // 하이브리드 결과도 실패하면 궁극 백업 파서 사용
        if (!hybridResult.isSuccessful()) {
            Log.w(TAG, "⚠️ Hybrid result failed - trying emergency fallback")
            val ultimateResult = ultimateFallbackParser.parseAsLastResort(text, packageName)
            
            if (ultimateResult.isSuccessful()) {
                Log.d(TAG, "✅ Emergency fallback succeeded: ${ultimateResult.getSummary()}")
                return ultimateResult
            }
            
            // 정말 모든 방법이 실패한 경우
            Log.e(TAG, "💥 ALL parsing methods failed - returning error result")
            return createErrorResult("All parsing methods (Advanced + Standard + Emergency) failed", text)
        }
        
        return hybridResult
    }
    
    /**
     * 고급 분석과 표준 결과를 결합한 최적 결과 생성
     */
    private fun createHybridResult(
        analysisResult: TransactionParseResult,
        standardResult: TransactionParseResult, 
        text: String
    ): TransactionParseResult {
        
        // 최선의 정보를 조합 (우선순위: standard > analysis)
        val bestAmount = standardResult.amount?.takeIf { it > 0 } ?: analysisResult.amount?.takeIf { it > 0 }
        val bestMerchant = standardResult.merchant?.takeIf { it != "알 수 없음" && it.isNotBlank() } 
                          ?: analysisResult.merchant?.takeIf { it != "알 수 없음" && it.isNotBlank() } ?: "알 수 없음"
        val bestType = standardResult.transactionType?.takeIf { it.isNotBlank() } 
                      ?: analysisResult.transactionType?.takeIf { it.isNotBlank() } ?: "출금"
        
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
            method = "HYBRID_ANALYSIS",
            details = "Combined Analysis (${String.format("%.2f", analysisResult.confidence)}) + Standard (${String.format("%.2f", standardResult.confidence)}) result"
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