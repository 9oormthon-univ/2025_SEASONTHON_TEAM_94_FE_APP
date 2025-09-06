package kr.klr.stopusing

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.gson.Gson
import kr.klr.stopusing.data.ApiResponse
import kr.klr.stopusing.data.TransactionResponse

class NotificationListenerService : NotificationListenerService() {
    
    // AI Transaction Parser (includes Smart Parser as fallback)
    private lateinit var aiParser: AITransactionParser
    
    // Notification Manager for transaction classification
    private lateinit var transactionNotificationManager: TransactionNotificationManager
    
    // JSON parser
    private val gson = Gson()
    
    companion object {
        private const val TAG = "FinancialNotificationListener"
        private const val CHANNEL = "kr.klr.stopusing/notification_listener"
        private const val PREFS_NAME = "stopusing_prefs"
        private const val USER_UID_KEY = "user_uid"
        
        // Korean financial app package names (verified from Google Play Store)
        private val KOREAN_FINANCIAL_APPS = setOf(
            // 주요 은행
            "com.kbstar.kbbank",           // KB국민은행
            "com.shinhan.sbanking",        // 신한은행
            "com.wooribank.smart.npib",    // 우리은행
            "com.nh.smart.nhallonepay",    // NH농협은행 (올원페이)
            "com.ibk.neobanking",          // IBK기업은행
            "com.kebhana.hanapush",        // 하나은행
            "com.standardchartered.scb.kr.mobile", // SC제일은행
            "com.kbank.smart",             // 케이뱅크
            "com.kakaobank.channel",       // 카카오뱅크
            "com.toss.im",                 // 토스뱅크
            
            // 간편결제 및 핀테크
            "viva.republica.toss",         // 토스
            // "com.kakao.talk",              // 카카오톡 (카카오페이)
            "com.nhn.android.payapp",      // 페이코
            "com.samsung.android.samsungpay", // 삼성페이
            "com.lgu.mobile.lgpay",        // LG페이
            "com.ssg.serviceapp.android.egiftcertificate", // SSG페이
            "com.tmoney.tmoneycard",       // 티머니
            
            // 주요 카드사
            "com.hanaskcard.paycla",       // 하나카드
            "com.lotte.lottesmartpay",     // 롯데카드  
            "com.hyundaicard.appcard",     // 현대카드
            "com.kbcard.cxh.appcard",      // KB카드
            "com.shinhancard.smartshinhan", // 신한카드
            "com.wooricard.wpay",          // 우리카드
            "com.samsung.android.scard",   // 삼성카드
            "com.bccard.android.mobile",   // BC카드
            "com.nhcard.nhallonepay",      // NH농협카드
            
            // 증권사
            "com.miraeasset.trade",        // 미래에셋증권
            "kr.co.shinhansec.shinhansecapp", // 신한투자증권
            "com.kbsec.mobile.kbstar",     // KB증권
            "com.namuh.acecounter.android" // 키움증권
        )
        
        // 패키지명 → 은행명/서비스명 매핑
        private val PACKAGE_TO_BANK_NAME = mapOf(
            // 주요 은행
            "com.kbstar.kbbank" to "KB국민은행",
            "com.shinhan.sbanking" to "신한은행", 
            "com.wooribank.smart.npib" to "우리은행",
            "com.nh.smart.nhallonepay" to "NH농협은행",
            "com.ibk.neobanking" to "IBK기업은행",
            "com.kebhana.hanapush" to "하나은행",
            "com.standardchartered.scb.kr.mobile" to "SC제일은행",
            "com.kbank.smart" to "케이뱅크",
            "com.kakaobank.channel" to "카카오뱅크",
            "com.toss.im" to "토스뱅크",
            
            // 간편결제 및 핀테크
            "viva.republica.toss" to "토스",
            "com.nhn.android.payapp" to "페이코",
            "com.samsung.android.samsungpay" to "삼성페이",
            "com.lgu.mobile.lgpay" to "LG페이",
            "com.ssg.serviceapp.android.egiftcertificate" to "SSG페이",
            "com.tmoney.tmoneycard" to "티머니",
            
            // 주요 카드사
            "com.hanaskcard.paycla" to "하나카드",
            "com.lotte.lottesmartpay" to "롯데카드",
            "com.hyundaicard.appcard" to "현대카드",
            "com.kbcard.cxh.appcard" to "KB카드",
            "com.shinhancard.smartshinhan" to "신한카드",
            "com.wooricard.wpay" to "우리카드",
            "com.samsung.android.scard" to "삼성카드",
            "com.bccard.android.mobile" to "BC카드",
            "com.nhcard.nhallonepay" to "NH농협카드",
            
            // 증권사
            "com.miraeasset.trade" to "미래에셋증권",
            "kr.co.shinhansec.shinhansecapp" to "신한투자증권",
            "com.kbsec.mobile.kbstar" to "KB증권",
            "com.namuh.acecounter.android" to "키움증권"
        )
    }
    
    private var methodChannel: MethodChannel? = null
    private var flutterEngine: FlutterEngine? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationListenerService created")
        aiParser = AITransactionParser(this)
        transactionNotificationManager = TransactionNotificationManager(this)
        setupFlutterEngine()
    }
    
    private fun setupFlutterEngine() {
        try {
            Log.d(TAG, "🔧 Attempting to setup Flutter engine...")
            
            // NotificationListenerService는 백그라운드 서비스이므로
            // Flutter Engine 연결이 어려울 수 있음
            // 대신 브로드캐스트를 사용하거나 직접 데이터베이스에 저장
            
            Log.w(TAG, "⚠️ Skipping Flutter engine setup - using alternative method")
            methodChannel = null
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Failed to setup Flutter engine", e)
            methodChannel = null
        }
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn.packageName
        val notification = sbn.notification
        
        Log.d(TAG, "Notification received from: $packageName")
        
        // Check if this is from a Korean financial app
        if (isKoreanFinancialApp(packageName)) {
            Log.d(TAG, "✅ Processing financial notification from: $packageName")
            processFinancialNotification(packageName, notification)
        } else {
            Log.d(TAG, "❌ Ignoring non-financial app notification from: $packageName")
        }
    }
    
    private fun isKoreanFinancialApp(packageName: String): Boolean {
        return KOREAN_FINANCIAL_APPS.contains(packageName)
    }
    
    /**
     * 패키지명으로부터 은행명/서비스명 추출
     */
    private fun getBankNameFromPackage(packageName: String): String {
        return PACKAGE_TO_BANK_NAME[packageName] ?: "알 수 없음"
    }
    
    private fun processFinancialNotification(packageName: String, notification: Notification) {
        try {
            val extras = notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            
            val fullText = "$title $text $bigText"
            Log.d(TAG, "📱 Processing notification from $packageName")
            Log.d(TAG, "📄 Title: '$title'")
            Log.d(TAG, "📄 Text: '$text'")
            Log.d(TAG, "📄 BigText: '$bigText'")
            Log.d(TAG, "📄 FullText: '$fullText'")
            
            // 🔥 입금 알림은 AI 파싱하지 않고 바로 무시
            if (isDepositTransaction(title, fullText)) {
                Log.d(TAG, "📈 입금 거래 감지 - AI 파싱 건너뛰기: '$title'")
                Log.d(TAG, "💰 지출 관리 앱이므로 입금은 추적하지 않습니다")
                return
            }
            
            // Extract transaction information using AI Parser
            val parseResult = aiParser.parseTransaction(fullText, packageName)
            
            if (parseResult.isSuccessful()) {
                // 🔥 지출 관리: 출금/결제/송금만 추적 (입금 제외)
                val transactionType = parseResult.transactionType ?: "출금"
                
                if (isExpenseTransaction(transactionType)) {
                    val expenseInfo: Map<String, Any> = mapOf(
                        "packageName" to packageName,
                        "appName" to getAppName(packageName),
                        "amount" to (parseResult.amount ?: 0L),
                        "merchant" to (parseResult.merchant ?: "알 수 없음"),
                        "transactionType" to transactionType,
                        "rawText" to fullText,
                        "timestamp" to System.currentTimeMillis()
                    )
                    
                    Log.d(TAG, "✅ ${parseResult.getSummary()}")
                    Log.d(TAG, "🔍 ${parseResult.details}")
                    sendToFlutter(expenseInfo)
                    
                    // API 서버로 거래 정보 전송
                    sendToApi(
                        price = parseResult.amount ?: 0L,
                        title = parseResult.merchant ?: "알 수 없음",
                        startAt = getCurrentISO8601Time(),
                        bankName = getBankNameFromPackage(packageName),
                        memo = fullText
                    )
                } else {
                    Log.d(TAG, "📈 입금 거래 무시: ${parseResult.getSummary()}")
                    Log.d(TAG, "💰 지출 관리 앱이므로 입금은 추적하지 않습니다")
                }
            } else {
                Log.w(TAG, "❌ AI parsing failed from: '$fullText'")
                Log.w(TAG, "🔍 ${parseResult.details}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error processing notification", e)
        }
    }
    
    /**
     * 입금 거래인지 종합적으로 판단 (AI 파싱 전 사전 필터링)
     * 제목과 전체 텍스트를 모두 분석하여 더 정확한 판단
     */
    private fun isDepositTransaction(title: String, fullText: String): Boolean {
        val lowerTitle = title.lowercase()
        val lowerFullText = fullText.lowercase()
        
        // 1. 명확한 입금 패턴 확인
        val depositPatterns = listOf(
            "입금",           // 일반 입금
            "수신",           // 송금 수신
            "받기",           // 송금 받기
            "입출금",         // 입출금 (대부분 입금)
            "급여",           // 급여 입금
            "이자",           // 이자 입금
            "환급",           // 세금 환급
            "지급"           // 보험금 지급 등
        )
        
        // 2. 명확한 출금 패턴 확인 
        val expensePatterns = listOf(
            "출금",           // 일반 출금
            "결제",           // 카드 결제
            "승인",           // 결제 승인
            "송금",           // 송금
            "이체",           // 계좌 이체
            "납부",           // 요금 납부
            "구매",           // 온라인 구매
            "할부",           // 할부 결제
            "자동이체"        // 자동이체
        )
        
        // 3. 전체 텍스트에서 입금/출금 점수 계산
        var depositScore = 0
        var expenseScore = 0
        
        // 제목에서 더 높은 가중치
        depositPatterns.forEach { pattern ->
            if (lowerTitle.contains(pattern)) depositScore += 3
            if (lowerFullText.contains(pattern)) depositScore += 1
        }
        
        expensePatterns.forEach { pattern ->
            if (lowerTitle.contains(pattern)) expenseScore += 3
            if (lowerFullText.contains(pattern)) expenseScore += 1
        }
        
        // 4. 은행별 특수 패턴 확인
        // KB: "스마트폰출금", "ATM출금"
        if (lowerFullText.contains("스마트폰출금") || lowerFullText.contains("atm출금")) {
            expenseScore += 2
        }
        
        // 신한: 계좌번호 다음에 금액이 나오는 패턴에서 방향 확인
        if (lowerFullText.contains("잔액")) {
            // "잔액 xxxxx원" 앞의 내용 분석
            val beforeBalance = lowerFullText.substringBefore("잔액")
            if (beforeBalance.contains("출금") || beforeBalance.contains("결제")) {
                expenseScore += 2
            }
        }
        
        // 5. 최종 판단: 입금 점수가 출금 점수보다 높고, 최소 1점 이상이면 입금으로 판단
        return depositScore > expenseScore && depositScore > 0
    }
    
    /**
     * 지출 거래인지 판단 (StopUsing 앱 목적)
     * 입금은 제외하고 출금/결제/송금만 추적
     */
    private fun isExpenseTransaction(transactionType: String): Boolean {
        return when (transactionType.lowercase()) {
            "입금" -> false  // 입금은 지출이 아님
            "출금", "결제", "송금", "이체", "승인" -> true  // 지출 거래
            else -> true  // 알 수 없는 경우 지출로 간주 (보수적 접근)
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app name for $packageName", e)
            packageName
        }
    }
    
    private fun sendToFlutter(expenseInfo: Map<String, Any>) {
        try {
            Log.d(TAG, "💾 Saving expense info directly to database...")
            
            // 직접 데이터베이스에 저장
            saveToDatabase(expenseInfo)
            
            // SharedPreferences를 통해 Flutter에 새 데이터 알림
            notifyFlutterOfNewTransaction()
            
            Log.d(TAG, "✅ Expense data saved and Flutter notified: $expenseInfo")
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error saving expense data: ${e.message}", e)
        }
    }
    
    private fun saveToDatabase(expenseInfo: Map<String, Any>) {
        try {
            val dbHelper = TransactionDBHelper(this)
            val db = dbHelper.writableDatabase
            
            val values = ContentValues().apply {
                put("packageName", expenseInfo["packageName"] as String)
                put("appName", expenseInfo["appName"] as String)
                put("amount", expenseInfo["amount"] as Long)
                put("merchant", expenseInfo["merchant"] as String)
                put("transactionType", expenseInfo["transactionType"] as String)
                put("rawText", expenseInfo["rawText"] as String)
                put("timestamp", expenseInfo["timestamp"] as Long)
            }
            
            val id = db.insert("transactions", null, values)
            Log.d(TAG, "📊 Transaction saved to database with ID: $id")
            
            // 저장 후 데이터 확인
            val cursor = db.query("transactions", null, null, null, null, null, "timestamp DESC", "5")
            Log.d(TAG, "📋 Recent transactions in database:")
            while (cursor.moveToNext()) {
                val appName = cursor.getString(cursor.getColumnIndexOrThrow("appName"))
                val amount = cursor.getLong(cursor.getColumnIndexOrThrow("amount"))
                val merchant = cursor.getString(cursor.getColumnIndexOrThrow("merchant"))
                Log.d(TAG, "  - $appName: $merchant $amount 원")
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            Log.e(TAG, "💥 Database error: ${e.message}", e)
        }
    }
    
    private fun notifyFlutterOfNewTransaction() {
        try {
            // SharedPreferences에 새 거래 플래그 설정 (Flutter default preferences 사용)
            val prefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val currentTime = System.currentTimeMillis()
            prefs.edit()
                .putBoolean("flutter.new_transaction", true)
                .putLong("flutter.last_transaction_time", currentTime)
                .apply()
                
            Log.d(TAG, "🚩 Flutter notification flag set: new_transaction=true, time=$currentTime")
            
            // 설정된 값 확인
            val checkPrefs = getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
            val isSet = checkPrefs.getBoolean("flutter.new_transaction", false)
            val timeSet = checkPrefs.getLong("flutter.last_transaction_time", 0)
            Log.d(TAG, "🔍 Verification: new_transaction=$isSet, time=$timeSet")
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error setting notification flag: ${e.message}", e)
        }
    }
    
    /**
     * API 서버로 거래 정보 전송 (AI 파싱 성공 후)
     * 사용자가 요청한 백엔드 형식에 맞춰 모든 필드 포함
     */
    private fun sendToApi(
        price: Long, 
        title: String, 
        startAt: String,
        bankName: String,
        memo: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.stopusing.klr.kr/api/v1/transactions/alarm")
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                val jsonPayload = """
                {
                    "price": $price,
                    "startAt": "$startAt",
                    "title": "$title",
                    "bankName": "$bankName",
                    "memo": "$memo",
                    "userUid": "${getUserUid()}"
                }
                """.trimIndent()
                
                Log.d(TAG, "🌐 API 요청 전송: https://api.stopusing.klr.kr/api/v1/transactions/alarm")
                Log.d(TAG, "📤 JSON 페이로드: $jsonPayload")
                
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonPayload)
                writer.flush()
                writer.close()
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    Log.d(TAG, "✅ API 전송 성공: $responseCode")
                    
                    // API 응답을 JSON으로 파싱하여 거래 분류 알림 표시
                    try {
                        val response = connection.inputStream.bufferedReader().readText()
                        Log.d(TAG, "📝 API 응답: $response")
                        
                        val apiResponse = gson.fromJson(response, ApiResponse::class.java)
                        
                        if (apiResponse.success && apiResponse.data != null) {
                            val transactionResponse = apiResponse.data
                            
                            // 거래 분류 알림 표시
                            transactionNotificationManager.showTransactionClassificationNotification(transactionResponse)
                            Log.d(TAG, "🔔 거래 분류 알림 표시: ${transactionResponse.title} (ID: ${transactionResponse.id})")
                        } else {
                            Log.e(TAG, "❌ API 응답 실패: success=${apiResponse.success}, message=${apiResponse.message}")
                        }
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "💥 API 응답 파싱 실패", e)
                    }
                } else {
                    Log.w(TAG, "⚠️ API 전송 실패: $responseCode")
                    try {
                        val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                        Log.w(TAG, "❌ 오류 응답: $errorResponse")
                    } catch (e: Exception) {
                        Log.w(TAG, "오류 응답 읽기 실패", e)
                    }
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                Log.e(TAG, "💥 API 전송 오류: ${e.message}", e)
            }
        }
    }
    
    /**
     * 현재 시간을 ISO8601 형식으로 반환
     */
    private fun getCurrentISO8601Time(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
    
    /**
     * 저장된 user UID를 가져오기
     */
    private fun getUserUid(): String {
        return try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val userUid = prefs.getString(USER_UID_KEY, "a") ?: "a"
            Log.d(TAG, "✅ User UID retrieved: $userUid")
            userUid
        } catch (e: Exception) {
            Log.e(TAG, "💥 Failed to get user UID: ${e.message}")
            "a" // fallback to default
        }
    }
    
    private fun saveToLocalDatabase(withdrawalInfo: Map<String, Any>) {
        try {
            Log.d(TAG, "💾 Fallback: Saving directly to local database")
            // TODO: Implement direct database save as fallback
            // For now, just log the data that should have been saved
            Log.d(TAG, "📊 Data to save: $withdrawalInfo")
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error saving to local database: ${e.message}", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        aiParser.cleanup()
        methodChannel = null
        flutterEngine?.destroy()
        flutterEngine = null
        Log.d(TAG, "NotificationListenerService destroyed")
    }
    
    // Check if notification listener permission is enabled
    private fun isNotificationListenerEnabled(): Boolean {
        val componentName = ComponentName(this, javaClass)
        val enabledNotificationListeners = android.provider.Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledNotificationListeners?.contains(componentName.flattenToString()) == true
    }
}

// 간단한 SQLite Helper 클래스 - Flutter와 같은 데이터베이스 사용
class TransactionDBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_VERSION = 2
        const val DATABASE_NAME = "financial_transactions.db"
        const val TABLE_NAME = "transactions"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                packageName TEXT NOT NULL,
                appName TEXT NOT NULL,
                amount INTEGER NOT NULL,
                merchant TEXT NOT NULL,
                rawText TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                transactionType TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN transactionType TEXT")
        }
    }
}