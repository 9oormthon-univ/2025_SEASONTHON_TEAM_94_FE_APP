package kr.klr.stopusing

import android.content.Context
import android.util.Log
import kr.klr.stopusing.ai.TensorFlowLiteManager
import kr.klr.stopusing.ai.NerResultProcessor
import kr.klr.stopusing.config.KoreanFinancialVocabulary
import kr.klr.stopusing.data.TransactionParseResult
import kr.klr.stopusing.text.PerfectKoreanTokenizer

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
            Log.e(TAG, "💥 Model not initialized")
            return createErrorResult("Model not initialized", text)
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
                Log.e(TAG, "💥 Model inference returned null")
                return createErrorResult("Model inference failed", text)
            }
            
            Log.d(TAG, "📊 Model output shape: [${modelOutput.size}, ${modelOutput[0].size}, ${modelOutput[0][0].size}]")
            
            // 3. 결과 처리 및 반환
            Log.d(TAG, "🔍 Step 3: Processing NER results...")
            val result = resultProcessor.processNerOutput(modelOutput, text, packageName)
            
            logParsingResult(result, text)
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Parsing error", e)
            Log.e(TAG, "📱 Stack trace:", e)
            createErrorResult("Parsing exception: ${e.message}", text)
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
     * 리소스 정리
     */
    fun cleanup() {
        Log.d(TAG, "🧹 Cleaning up AI Transaction Parser resources...")
        modelManager.cleanup()
    }
}