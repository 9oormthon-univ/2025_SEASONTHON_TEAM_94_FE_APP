package com.example.stopusing_app

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import java.util.regex.Pattern

class NotificationListenerService : NotificationListenerService() {
    
    // Smart Transaction Parser
    private lateinit var smartParser: SmartTransactionParser
    
    companion object {
        private const val TAG = "FinancialNotificationListener"
        private const val CHANNEL = "com.example.stopusing_app/notification_listener"
        
        // Korean financial app package names (verified from Google Play Store)
        private val KOREAN_FINANCIAL_APPS = setOf(
            // 테스트용 - 현재 앱
            "com.example.stopusing_app",   // 테스트 앱 (개발용)
            
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
            "com.kakao.talk",              // 카카오톡 (카카오페이)
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
        
        // Enhanced regex patterns for Korean financial notifications
        private val WITHDRAWAL_PATTERNS = arrayOf(
            // Standard patterns
            Pattern.compile("(출금|지출|결제|이체).*?([0-9,]+)원", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([0-9,]+)원.*?(출금|지출|결제|이체)", Pattern.CASE_INSENSITIVE),
            
            // Card approval patterns
            Pattern.compile("카드.*?승인.*?([0-9,]+)원", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([0-9,]+)원.*?승인", Pattern.CASE_INSENSITIVE),
            Pattern.compile("승인.*?([0-9,]+)원", Pattern.CASE_INSENSITIVE),
            
            // Bank-specific patterns with brackets
            Pattern.compile("\\[(.+?)\\].*?([0-9,]+)원", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([0-9,]+)원.*?\\[(.+?)\\]", Pattern.CASE_INSENSITIVE),
            
            // Merchant-amount patterns
            Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+([0-9,]+)원", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([0-9,]+)원\\s+([가-힣a-zA-Z\\s]+)", Pattern.CASE_INSENSITIVE),
            
            // Payment completion patterns
            Pattern.compile("결제완료.*?([0-9,]+)원", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([0-9,]+)원.*?결제완료", Pattern.CASE_INSENSITIVE),
            
            // Transfer/withdrawal patterns
            Pattern.compile("송금.*?([0-9,]+)원", Pattern.CASE_INSENSITIVE),
            Pattern.compile("현금출금.*?([0-9,]+)원", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ATM.*?([0-9,]+)원", Pattern.CASE_INSENSITIVE)
        )
    }
    
    private var methodChannel: MethodChannel? = null
    private var flutterEngine: FlutterEngine? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationListenerService created")
        smartParser = SmartTransactionParser()
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
            
            // Try to extract withdrawal information using Smart Parser
            val parseResult = smartParser.parseTransaction(fullText, packageName)
            
            if (parseResult.amount != null && parseResult.amount > 0) {
                val withdrawalInfo = mapOf(
                    "packageName" to packageName,
                    "appName" to getAppName(packageName),
                    "amount" to parseResult.amount,
                    "merchant" to (parseResult.merchant ?: "알 수 없음"),
                    "rawText" to fullText,
                    "timestamp" to System.currentTimeMillis()
                )
                
                Log.d(TAG, "✅ Smart parsing successful (confidence: ${String.format("%.2f", parseResult.confidence)})")
                Log.d(TAG, "📊 Extracted: amount=${parseResult.amount}, merchant=${parseResult.merchant}")
                Log.d(TAG, "🔍 Details: ${parseResult.details}")
                sendToFlutter(withdrawalInfo)
            } else {
                Log.w(TAG, "❌ Smart parsing failed (confidence: ${String.format("%.2f", parseResult.confidence)}) from: '$fullText'")
                Log.w(TAG, "🔍 Details: ${parseResult.details}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error processing notification", e)
        }
    }
    
    private fun extractWithdrawalInfo(text: String, packageName: String): Map<String, Any>? {
        for (pattern in WITHDRAWAL_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amount = extractAmount(matcher, text)
                val merchant = extractMerchant(matcher, text)
                
                if (amount != null) {
                    return mapOf(
                        "packageName" to packageName,
                        "appName" to getAppName(packageName),
                        "amount" to amount,
                        "merchant" to (merchant ?: "알 수 없음"),
                        "rawText" to text,
                        "timestamp" to System.currentTimeMillis()
                    )
                }
            }
        }
        return null
    }
    
    private fun extractAmount(matcher: java.util.regex.Matcher, text: String): Long? {
        return try {
            for (i in 1..matcher.groupCount()) {
                val group = matcher.group(i)
                if (group != null && group.matches(Regex("[0-9,]+"))) {
                    return group.replace(",", "").toLong()
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting amount", e)
            null
        }
    }
    
    private fun extractMerchant(matcher: java.util.regex.Matcher, text: String): String? {
        return try {
            Log.d(TAG, "🔍 Extracting merchant from text: '$text'")
            
            // First try to find merchant from regex groups
            for (i in 1..matcher.groupCount()) {
                val group = matcher.group(i)
                if (group != null && !group.matches(Regex("[0-9,]+")) && 
                    !group.matches(Regex("(출금|지출|결제|이체|승인)")) && group.length > 1) {
                    Log.d(TAG, "📍 Found merchant from regex group $i: '$group'")
                    return group.trim()
                }
            }
            
            // Bank-specific merchant extraction patterns
            val merchant = when {
                // KB국민은행 패턴: "이*혁님 08/25 19:39 941602-**-***064 이수혁 스마트폰출금"
                text.contains("kbstar") || text.contains("KB") || text.contains("국민") -> {
                    extractKBMerchant(text)
                }
                // 신한은행 패턴
                text.contains("shinhan") || text.contains("신한") -> {
                    extractShinhanMerchant(text)
                }
                // 토스 패턴
                text.contains("toss") || text.contains("토스") -> {
                    extractTossMerchant(text)
                }
                // 우리은행 패턴
                text.contains("woori") || text.contains("우리") -> {
                    extractWooriMerchant(text)
                }
                // 카카오페이 패턴
                text.contains("kakao") || text.contains("카카오") -> {
                    extractKakaoMerchant(text)
                }
                // 일반 패턴
                else -> {
                    extractGenericMerchant(text)
                }
            }
            
            Log.d(TAG, "💡 Final extracted merchant: '$merchant'")
            merchant ?: "알 수 없음"
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting merchant", e)
            "알 수 없음"
        }
    }
    
    private fun extractKBMerchant(text: String): String? {
        // KB 패턴: "이*혁님 08/25 19:39 941602-**-***064 이수혁 스마트폰출금"
        // 계좌번호 다음의 한글 이름이 merchant
        val kbPattern = Pattern.compile("[0-9]{6}-[\\*]{2}-[\\*]{3}[0-9]{3}\\s+([가-힣]+)\\s+(스마트폰출금|ATM출금|이체|결제)")
        val matcher = kbPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        
        // 다른 KB 패턴들도 시도
        val kbPatterns = arrayOf(
            Pattern.compile("([가-힣]{2,10})\\s+(스마트폰출금|ATM출금|이체)"),
            Pattern.compile("\\s+([가-힣]+)\\s+[0-9,]+\\s*잔액")
        )
        
        for (pattern in kbPatterns) {
            val patternMatcher = pattern.matcher(text)
            if (patternMatcher.find()) {
                val merchant = patternMatcher.group(1)?.trim()
                if (merchant != null && !merchant.matches(Regex("(출금|지출|결제|이체|승인|님)"))) {
                    return merchant
                }
            }
        }
        
        return null
    }
    
    private fun extractShinhanMerchant(text: String): String? {
        // 신한은행 특화 패턴들
        val shinhanPatterns = arrayOf(
            Pattern.compile("\\[([가-힣a-zA-Z\\s]+)\\].*?[0-9,]+원"),
            Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+[0-9,]+원.*승인"),
            Pattern.compile("승인.*?([가-힣a-zA-Z\\s]+)\\s+[0-9,]+원")
        )
        
        for (pattern in shinhanPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (merchant != null && merchant.length > 1 && 
                    !merchant.matches(Regex("(출금|지출|결제|이체|승인|신한|카드)"))) {
                    return merchant
                }
            }
        }
        
        return null
    }
    
    private fun extractTossMerchant(text: String): String? {
        // 토스 특화 패턴들
        val tossPatterns = arrayOf(
            Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+[0-9,]+원"),
            Pattern.compile("[0-9,]+원\\s+([가-힣a-zA-Z\\s]+)"),
            Pattern.compile("결제.*?([가-힣a-zA-Z\\s]+)")
        )
        
        for (pattern in tossPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (merchant != null && merchant.length > 1 && 
                    !merchant.matches(Regex("(출금|지출|결제|이체|승인|토스)"))) {
                    return merchant
                }
            }
        }
        
        return null
    }
    
    private fun extractWooriMerchant(text: String): String? {
        // 우리은행 특화 패턴들
        val wooriPatterns = arrayOf(
            Pattern.compile("ATM.*?([가-힣a-zA-Z\\s]+)\\s+[0-9,]+원"),
            Pattern.compile("현금출금.*?([가-힣a-zA-Z\\s]+)")
        )
        
        for (pattern in wooriPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (merchant != null && merchant.length > 1) {
                    return merchant
                }
            }
        }
        
        return null
    }
    
    private fun extractKakaoMerchant(text: String): String? {
        // 카카오페이 특화 패턴들
        val kakaoPatterns = arrayOf(
            Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+[0-9,]+원.*카카오페이"),
            Pattern.compile("카카오페이.*?([가-힣a-zA-Z\\s]+)\\s+[0-9,]+원")
        )
        
        for (pattern in kakaoPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (merchant != null && merchant.length > 1 && 
                    !merchant.matches(Regex("(카카오|페이)"))) {
                    return merchant
                }
            }
        }
        
        return null
    }
    
    private fun extractGenericMerchant(text: String): String? {
        // 일반적인 merchant 추출 패턴들
        val lines = text.split("\\n")
        for (line in lines) {
            val trimmedLine = line.trim()
            // Skip lines that only contain amounts or transaction types
            if (trimmedLine.matches(Regex(".*[0-9,]+원.*")) || 
                trimmedLine.matches(Regex("(출금|지출|결제|이체|승인|완료)")) ||
                trimmedLine.length < 2) {
                continue
            }
            
            // Look for merchant names (usually the first non-amount, non-transaction-type line)
            if (!trimmedLine.contains("카드") && !trimmedLine.contains("은행") && 
                !trimmedLine.contains("페이") && trimmedLine.isNotEmpty()) {
                return trimmedLine
            }
        }
        
        // If still no merchant found, try to extract from specific patterns
        val merchantPatterns = arrayOf(
            Pattern.compile("([가-힣a-zA-Z\\s]+)\\s*[0-9,]+원"),
            Pattern.compile("[0-9,]+원\\s*([가-힣a-zA-Z\\s]+)")
        )
        
        for (pattern in merchantPatterns) {
            val merchantMatcher = pattern.matcher(text)
            if (merchantMatcher.find()) {
                val merchant = merchantMatcher.group(1)?.trim()
                if (merchant != null && merchant.length > 1 && 
                    !merchant.matches(Regex("(출금|지출|결제|이체|승인)"))) {
                    return merchant
                }
            }
        }
        
        return null
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
    
    private fun sendToFlutter(withdrawalInfo: Map<String, Any>) {
        try {
            Log.d(TAG, "💾 Saving withdrawal info directly to database...")
            
            // 직접 데이터베이스에 저장
            saveToDatabase(withdrawalInfo)
            
            // SharedPreferences를 통해 Flutter에 새 데이터 알림
            notifyFlutterOfNewTransaction()
            
            Log.d(TAG, "✅ Data saved and Flutter notified: $withdrawalInfo")
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error saving data: ${e.message}", e)
        }
    }
    
    private fun saveToDatabase(withdrawalInfo: Map<String, Any>) {
        try {
            val dbHelper = TransactionDBHelper(this)
            val db = dbHelper.writableDatabase
            
            val values = ContentValues().apply {
                put("packageName", withdrawalInfo["packageName"] as String)
                put("appName", withdrawalInfo["appName"] as String)
                put("amount", withdrawalInfo["amount"] as Long)
                put("merchant", withdrawalInfo["merchant"] as String)
                put("rawText", withdrawalInfo["rawText"] as String)
                put("timestamp", withdrawalInfo["timestamp"] as Long)
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
        const val DATABASE_VERSION = 1
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
                timestamp INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}