package kr.klr.stopusing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.klr.stopusing.data.TransactionType
import kr.klr.stopusing.data.TransactionTypeUpdateRequest
import com.google.gson.Gson
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 거래 분류 알림에서 버튼 클릭 처리를 담당하는 BroadcastReceiver
 */
class TransactionActionReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "TransactionActionReceiver"
        private const val API_BASE_URL = "https://api.stopusing.klr.kr/api/v1/transactions"
    }
    
    private val gson = Gson()
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val transactionId = intent.getLongExtra(TransactionNotificationManager.EXTRA_TRANSACTION_ID, -1)
        val transactionTitle = intent.getStringExtra(TransactionNotificationManager.EXTRA_TRANSACTION_TITLE) ?: ""
        val transactionPrice = intent.getLongExtra(TransactionNotificationManager.EXTRA_TRANSACTION_PRICE, 0)
        
        if (transactionId == -1L) {
            Log.e(TAG, "💥 잘못된 거래 ID")
            return
        }
        
        val transactionType = when (intent.action) {
            TransactionNotificationManager.ACTION_FIXED_EXPENSE -> {
                Log.d(TAG, "🏠 고정지출 선택: $transactionTitle (ID: $transactionId)")
                TransactionType.FIXED_EXPENSE
            }
            TransactionNotificationManager.ACTION_OVER_EXPENSE -> {
                Log.d(TAG, "💸 초과지출 선택: $transactionTitle (ID: $transactionId)")
                TransactionType.OVER_EXPENSE
            }
            else -> {
                Log.e(TAG, "💥 알 수 없는 액션: ${intent.action}")
                return
            }
        }
        
        // 알림 제거
        val notificationManager = TransactionNotificationManager(context)
        notificationManager.dismissNotification(transactionId)
        
        // API 요청 전송
        updateTransactionType(transactionId, transactionType, transactionTitle)
        
        Log.d(TAG, "✅ 거래 분류 완료: $transactionTitle → ${transactionType.value}")
    }
    
    /**
     * 거래 유형 업데이트 API 요청
     */
    private fun updateTransactionType(transactionId: Long, type: TransactionType, title: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$API_BASE_URL/alarm")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "PUT"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                }
                
                // JSON 요청 바디 생성
                val request = TransactionTypeUpdateRequest(type)
                val jsonPayload = gson.toJson(request)
                
                Log.d(TAG, "🌐 API 요청 전송:")
                Log.d(TAG, "   URL: $API_BASE_URL/alarm")
                Log.d(TAG, "   Method: PUT")
                Log.d(TAG, "   Body: $jsonPayload")
                
                // 요청 바디 전송
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonPayload)
                writer.flush()
                writer.close()
                
                // 응답 처리
                val responseCode = connection.responseCode
                
                when (responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        Log.d(TAG, "✅ 거래 유형 업데이트 성공: $title → ${type.value}")
                        Log.d(TAG, "📊 응답 코드: $responseCode")
                    }
                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        Log.e(TAG, "❌ 거래를 찾을 수 없음: ID $transactionId")
                    }
                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        Log.e(TAG, "❌ 잘못된 요청: $jsonPayload")
                    }
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                        Log.e(TAG, "❌ 서버 오류 (500)")
                    }
                    else -> {
                        Log.e(TAG, "❌ API 요청 실패: $responseCode")
                    }
                }
                
                // 응답 내용 읽기 (디버깅용)
                try {
                    val response = if (responseCode in 200..299) {
                        connection.inputStream.bufferedReader().readText()
                    } else {
                        connection.errorStream?.bufferedReader()?.readText() ?: "No error message"
                    }
                    Log.d(TAG, "📝 응답 내용: $response")
                } catch (e: Exception) {
                    Log.w(TAG, "응답 내용 읽기 실패", e)
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                Log.e(TAG, "💥 거래 유형 업데이트 API 요청 실패", e)
                Log.e(TAG, "📊 요청 정보: ID=$transactionId, Type=${type.value}, Title=$title")
            }
        }
    }
}