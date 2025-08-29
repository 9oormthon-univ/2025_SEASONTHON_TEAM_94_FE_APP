package com.example.stopusing_app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.os.Bundle

class TestNotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "test_financial_notifications"
        private const val CHANNEL_NAME = "Test Financial Notifications"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 실제 금융 앱 알림을 시뮬레이션
     */
    fun simulateFinancialNotification(
        appName: String,
        packageName: String,
        transactionType: String,
        merchant: String,
        amount: String
    ) {
        val notificationId = System.currentTimeMillis().toInt()
        
        val title = when (packageName) {
            "viva.republica.toss" -> "토스 결제알림"
            "com.kbstar.kbbank" -> "KB국민은행"
            "com.shinhan.sbanking" -> "신한은행 알림"
            "com.kakao.talk" -> "카카오페이 결제"
            "com.wooribank.smart.npib" -> "우리은행"
            "com.kebhana.hanapush" -> "하나은행 알림"
            else -> "$appName 알림"
        }
        
        val content = generateNotificationContent(transactionType, merchant, amount, packageName)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        // 알림 표시
        notificationManager.notify(notificationId, notification)
        
        // 직접 NotificationListenerService 호출 (테스트용)
        simulateNotificationToListener(packageName, notification)
    }
    
    /**
     * 각 금융 앱별 실제 알림 텍스트 패턴 시뮬레이션
     */
    private fun generateNotificationContent(
        transactionType: String,
        merchant: String,
        amount: String,
        packageName: String
    ): String {
        return when (packageName) {
            "viva.republica.toss" -> {
                when (transactionType) {
                    "카드결제" -> "$merchant\n${amount}원 결제완료\n토스카드"
                    "계좌이체" -> "$merchant\n${amount}원 송금완료"
                    "출금" -> "$merchant\n${amount}원 출금"
                    else -> "$merchant ${amount}원 $transactionType"
                }
            }
            "com.kbstar.kbbank" -> {
                when (transactionType) {
                    "카드결제" -> "[KB국민카드] $merchant ${amount}원 승인"
                    "계좌이체" -> "[KB국민은행] $merchant ${amount}원 이체"
                    "출금" -> "[KB국민은행] ATM 출금 ${amount}원"
                    else -> "[KB국민은행] $merchant ${amount}원 $transactionType"
                }
            }
            "com.shinhan.sbanking" -> {
                when (transactionType) {
                    "카드결제" -> "신한카드 $merchant ${amount}원 결제승인"
                    "계좌이체" -> "신한은행 $merchant ${amount}원 이체완료"
                    "출금" -> "신한은행 현금출금 ${amount}원"
                    else -> "신한은행 $merchant ${amount}원 $transactionType"
                }
            }
            "com.kakao.talk" -> {
                when (transactionType) {
                    "카드결제" -> "💳 카카오페이 결제\n$merchant\n${amount}원"
                    "계좌이체" -> "💸 카카오페이 송금\n$merchant\n${amount}원"
                    "출금" -> "💰 카카오페이 출금\n$merchant\n${amount}원"
                    else -> "💳 카카오페이\n$merchant ${amount}원 $transactionType"
                }
            }
            "com.wooribank.smart.npib" -> {
                when (transactionType) {
                    "카드결제" -> "우리카드 $merchant ${amount}원 결제"
                    "계좌이체" -> "우리은행 $merchant ${amount}원 이체"
                    "출금" -> "우리은행 출금 ${amount}원"
                    else -> "우리은행 $merchant ${amount}원 $transactionType"
                }
            }
            "com.kebhana.hanapush" -> {
                when (transactionType) {
                    "카드결제" -> "하나카드 승인\n$merchant\n${amount}원"
                    "계좌이체" -> "하나은행 이체\n$merchant\n${amount}원"
                    "출금" -> "하나은행 출금\n${amount}원"
                    else -> "하나은행 $merchant ${amount}원 $transactionType"
                }
            }
            else -> "$transactionType $merchant ${amount}원"
        }
    }
    
    /**
     * NotificationListenerService에 직접 알림 전달 (테스트용)
     */
    private fun simulateNotificationToListener(
        packageName: String,
        notification: Notification
    ) {
        try {
            android.util.Log.d("TestNotificationHelper", "🧪 Simulating notification to listener")
            android.util.Log.d("TestNotificationHelper", "📦 Package: $packageName")
            android.util.Log.d("TestNotificationHelper", "📄 Title: ${notification.extras.getCharSequence(Notification.EXTRA_TITLE)}")
            android.util.Log.d("TestNotificationHelper", "📄 Text: ${notification.extras.getCharSequence(Notification.EXTRA_TEXT)}")
            
            // NotificationListenerService 인스턴스를 직접 찾아서 호출하는 대신
            // 별도의 방법으로 테스트 데이터를 전달해야 함
            // 실제로는 시스템 알림을 통해 NotificationListenerService가 자동 호출됨
            
        } catch (e: Exception) {
            android.util.Log.e("TestNotificationHelper", "💥 Error simulating notification", e)
        }
    }
    
    /**
     * 미리 정의된 테스트 시나리오들
     */
    fun runTestScenario(scenario: TestScenario) {
        when (scenario) {
            TestScenario.TOSS_COFFEE -> {
                simulateFinancialNotification(
                    "토스", "viva.republica.toss", "카드결제", 
                    "스타벅스 강남점", "4,500"
                )
            }
            TestScenario.KB_RESTAURANT -> {
                simulateFinancialNotification(
                    "KB국민은행", "com.kbstar.kbbank", "카드결제",
                    "맥도날드 홍대점", "8,900"
                )
            }
            TestScenario.SHINHAN_TRANSFER -> {
                simulateFinancialNotification(
                    "신한은행", "com.shinhan.sbanking", "계좌이체",
                    "김철수", "50,000"
                )
            }
            TestScenario.KAKAO_DELIVERY -> {
                simulateFinancialNotification(
                    "카카오페이", "com.kakao.talk", "카드결제",
                    "배달의민족", "13,500"
                )
            }
            TestScenario.WOORI_ATM -> {
                simulateFinancialNotification(
                    "우리은행", "com.wooribank.smart.npib", "출금",
                    "ATM 현금출금", "100,000"
                )
            }
            TestScenario.HANA_SHOPPING -> {
                simulateFinancialNotification(
                    "하나은행", "com.kebhana.hanapush", "카드결제",
                    "이마트 트레이더스", "45,800"
                )
            }
        }
    }
    
    /**
     * 파싱 로직 직접 테스트 (NotificationListenerService 우회)
     */
    fun testParsingLogic(scenario: TestScenario) {
        val (appName, packageName, transactionType, merchant, amount) = when (scenario) {
            TestScenario.TOSS_COFFEE -> arrayOf("토스", "viva.republica.toss", "카드결제", "스타벅스 강남점", "4,500")
            TestScenario.KB_RESTAURANT -> arrayOf("KB국민은행", "com.kbstar.kbbank", "카드결제", "맥도날드 홍대점", "8,900")
            TestScenario.SHINHAN_TRANSFER -> arrayOf("신한은행", "com.shinhan.sbanking", "계좌이체", "김철수", "50,000")
            TestScenario.KAKAO_DELIVERY -> arrayOf("카카오페이", "com.kakao.talk", "카드결제", "배달의민족", "13,500")
            TestScenario.WOORI_ATM -> arrayOf("우리은행", "com.wooribank.smart.npib", "출금", "ATM 현금출금", "100,000")
            TestScenario.HANA_SHOPPING -> arrayOf("하나은행", "com.kebhana.hanapush", "카드결제", "이마트 트레이더스", "45,800")
        }
        
        val content = generateNotificationContent(transactionType, merchant, amount, packageName)
        
        android.util.Log.d("TestNotificationHelper", "🧪 직접 파싱 테스트 시작")
        android.util.Log.d("TestNotificationHelper", "📦 Package: $packageName")
        android.util.Log.d("TestNotificationHelper", "📄 Content: '$content'")
        
        // 직접 파싱 로직 테스트 - 실제 NotificationListenerService와 동일한 로직 사용
        val testResult = testWithdrawalExtraction(content, packageName)
        
        if (testResult != null) {
            android.util.Log.d("TestNotificationHelper", "✅ 파싱 성공: $testResult")
            
            // Flutter로 결과 전송 시뮬레이션
            val context = this.context
            if (context is MainActivity) {
                // Method Channel을 통해 Flutter에 결과 전송
                android.util.Log.d("TestNotificationHelper", "📤 Flutter로 결과 전송 중...")
            }
        } else {
            android.util.Log.w("TestNotificationHelper", "❌ 파싱 실패: '$content'")
        }
    }
    
    /**
     * NotificationListenerService와 동일한 파싱 로직 (테스트용)
     */
    private fun testWithdrawalExtraction(text: String, packageName: String): Map<String, Any>? {
        // NotificationListenerService의 WITHDRAWAL_PATTERNS과 동일한 패턴 사용
        val patterns = arrayOf(
            java.util.regex.Pattern.compile("(출금|지출|결제|이체).*?([0-9,]+)원", java.util.regex.Pattern.CASE_INSENSITIVE),
            java.util.regex.Pattern.compile("([0-9,]+)원.*?(출금|지출|결제|이체)", java.util.regex.Pattern.CASE_INSENSITIVE),
            java.util.regex.Pattern.compile("카드.*?승인.*?([0-9,]+)원", java.util.regex.Pattern.CASE_INSENSITIVE),
            java.util.regex.Pattern.compile("([0-9,]+)원.*?승인", java.util.regex.Pattern.CASE_INSENSITIVE),
            java.util.regex.Pattern.compile("승인.*?([0-9,]+)원", java.util.regex.Pattern.CASE_INSENSITIVE),
            java.util.regex.Pattern.compile("([가-힣a-zA-Z\\s]+)\\s+([0-9,]+)원", java.util.regex.Pattern.CASE_INSENSITIVE),
            java.util.regex.Pattern.compile("([0-9,]+)원\\s+([가-힣a-zA-Z\\s]+)", java.util.regex.Pattern.CASE_INSENSITIVE),
            java.util.regex.Pattern.compile("결제완료.*?([0-9,]+)원", java.util.regex.Pattern.CASE_INSENSITIVE),
            java.util.regex.Pattern.compile("([0-9,]+)원.*?결제완료", java.util.regex.Pattern.CASE_INSENSITIVE),
            java.util.regex.Pattern.compile("송금.*?([0-9,]+)원", java.util.regex.Pattern.CASE_INSENSITIVE),
            java.util.regex.Pattern.compile("현금출금.*?([0-9,]+)원", java.util.regex.Pattern.CASE_INSENSITIVE),
            java.util.regex.Pattern.compile("ATM.*?([0-9,]+)원", java.util.regex.Pattern.CASE_INSENSITIVE)
        )
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                android.util.Log.d("TestNotificationHelper", "🎯 매칭된 패턴: ${pattern.pattern()}")
                
                val amount = extractTestAmount(matcher, text)
                val merchant = extractTestMerchant(matcher, text)
                
                if (amount != null) {
                    return mapOf(
                        "packageName" to packageName,
                        "appName" to getTestAppName(packageName),
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
    
    private fun extractTestAmount(matcher: java.util.regex.Matcher, text: String): Long? {
        return try {
            for (i in 1..matcher.groupCount()) {
                val group = matcher.group(i)
                if (group != null && group.matches(Regex("[0-9,]+"))) {
                    return group.replace(",", "").toLong()
                }
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("TestNotificationHelper", "Error extracting amount", e)
            null
        }
    }
    
    private fun extractTestMerchant(matcher: java.util.regex.Matcher, text: String): String? {
        return try {
            for (i in 1..matcher.groupCount()) {
                val group = matcher.group(i)
                if (group != null && !group.matches(Regex("[0-9,]+")) && 
                    !group.matches(Regex("(출금|지출|결제|이체|승인)")) && group.length > 1) {
                    return group.trim()
                }
            }
            
            // Enhanced merchant extraction
            val lines = text.split("\n")
            for (line in lines) {
                val trimmedLine = line.trim()
                if (trimmedLine.matches(Regex(".*[0-9,]+원.*")) || 
                    trimmedLine.matches(Regex("(출금|지출|결제|이체|승인|완료)")) ||
                    trimmedLine.length < 2) {
                    continue
                }
                
                if (!trimmedLine.contains("카드") && !trimmedLine.contains("은행") && 
                    !trimmedLine.contains("페이") && trimmedLine.isNotEmpty()) {
                    return trimmedLine
                }
            }
            
            "알 수 없음"
        } catch (e: Exception) {
            android.util.Log.e("TestNotificationHelper", "Error extracting merchant", e)
            "알 수 없음"
        }
    }
    
    private fun getTestAppName(packageName: String): String {
        return when (packageName) {
            "viva.republica.toss" -> "토스"
            "com.kbstar.kbbank" -> "KB국민은행"
            "com.shinhan.sbanking" -> "신한은행"
            "com.kakao.talk" -> "카카오페이"
            "com.wooribank.smart.npib" -> "우리은행"
            "com.kebhana.hanapush" -> "하나은행"
            else -> packageName
        }
    }
    
    enum class TestScenario {
        TOSS_COFFEE,
        KB_RESTAURANT,  
        SHINHAN_TRANSFER,
        KAKAO_DELIVERY,
        WOORI_ATM,
        HANA_SHOPPING
    }
}