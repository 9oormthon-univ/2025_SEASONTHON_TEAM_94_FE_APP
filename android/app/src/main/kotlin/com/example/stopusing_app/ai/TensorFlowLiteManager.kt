package com.example.stopusing_app.ai

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.example.stopusing_app.config.KoreanFinancialVocabulary
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite 모델 관리 클래스
 * 모델 로딩, 초기화, 추론 실행을 담당
 */
class TensorFlowLiteManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TensorFlowLiteManager"
    }
    
    private var tfliteInterpreter: Interpreter? = null
    
    /**
     * TensorFlow Lite 모델 초기화
     * 
     * @return 초기화 성공 여부
     */
    fun initializeModel(): Boolean {
        return try {
            Log.d(TAG, "🤖 Initializing TensorFlow Lite model...")
            
            if (!isModelAvailable()) {
                Log.w(TAG, "⚠️ Model file not found: ${KoreanFinancialVocabulary.MODEL_FILE}")
                return false
            }
            
            val modelBuffer = loadModelFile()
            val options = Interpreter.Options().apply {
                setNumThreads(2) // CPU 최적화
                setUseNNAPI(true) // Android Neural Networks API 활용
            }
            
            tfliteInterpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "✅ TensorFlow Lite model initialized successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Failed to initialize TensorFlow Lite model", e)
            tfliteInterpreter = null
            false
        }
    }
    
    /**
     * 모델 추론 실행
     * 
     * @param inputBuffer 입력 데이터 버퍼 (character-level tokenized input)
     * @return 추론 결과 배열 [batch_size=1, sequence_length=200, num_classes=7]
     */
    fun runInference(inputBuffer: ByteBuffer): Array<Array<FloatArray>>? {
        val interpreter = tfliteInterpreter ?: run {
            Log.e(TAG, "💥 TensorFlow Lite interpreter not initialized")
            return null
        }
        
        return try {
            // 범용 모델 출력 형태: [1, 200, 7]
            val outputBuffer = Array(1) { 
                Array(KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH) { 
                    FloatArray(7) 
                } 
            }
            
            interpreter.run(inputBuffer, outputBuffer)
            Log.d(TAG, "🤖 Universal model inference completed successfully")
            outputBuffer
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Universal model inference failed", e)
            null
        }
    }
    
    /**
     * 모델이 초기화되었는지 확인
     */
    fun isInitialized(): Boolean = tfliteInterpreter != null
    
    /**
     * 모델 파일이 assets에 존재하는지 확인
     */
    private fun isModelAvailable(): Boolean {
        return try {
            context.assets.open(KoreanFinancialVocabulary.MODEL_FILE).close()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * assets에서 모델 파일 로드
     */
    private fun loadModelFile(): ByteBuffer {
        val assetManager: AssetManager = context.assets
        val fileDescriptor = assetManager.openFd(KoreanFinancialVocabulary.MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        
        Log.d(TAG, "📁 Loading model file: ${KoreanFinancialVocabulary.MODEL_FILE} (${declaredLength} bytes)")
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        try {
            tfliteInterpreter?.close()
            tfliteInterpreter = null
            Log.d(TAG, "🧹 TensorFlow Lite resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error during cleanup", e)
        }
    }
    
    /**
     * 모델 정보 로깅
     */
    fun logModelInfo() {
        val interpreter = tfliteInterpreter ?: return
        
        try {
            val inputTensor = interpreter.getInputTensor(0)
            val outputTensor = interpreter.getOutputTensor(0)
            
            Log.d(TAG, "📊 Model Info:")
            Log.d(TAG, "  Input shape: ${inputTensor.shape().contentToString()}")
            Log.d(TAG, "  Output shape: ${outputTensor.shape().contentToString()}")
            Log.d(TAG, "  Input type: ${inputTensor.dataType()}")
            Log.d(TAG, "  Output type: ${outputTensor.dataType()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error getting model info", e)
        }
    }
}