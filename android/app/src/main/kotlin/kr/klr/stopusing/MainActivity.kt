package kr.klr.stopusing

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import kr.klr.stopusing.data.TransactionResponse

class MainActivity: FlutterActivity() {
    private val CHANNEL = "kr.klr.stopusing/notification_listener"
    private lateinit var methodChannel: MethodChannel
    private var withdrawalReceiver: BroadcastReceiver? = null
    private var currentUserUid: String = "a" // Default user UID
    
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val PREFS_NAME = "stopusing_prefs"
        private const val USER_UID_KEY = "user_uid"
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // Load saved user UID
        loadUserUid()
        
        methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "isNotificationListenerEnabled" -> {
                    val isEnabled = isNotificationListenerEnabled()
                    android.util.Log.d("MainActivity", "NotificationListener enabled: $isEnabled")
                    result.success(isEnabled)
                }
                "openNotificationListenerSettings" -> {
                    openNotificationListenerSettings()
                    result.success(null)
                }
                "requestNotificationPermission" -> {
                    requestNotificationPermission()
                    result.success(null)
                }
                "hasNotificationPermission" -> {
                    val hasPermission = hasNotificationPermission()
                    android.util.Log.d("MainActivity", "Notification permission: $hasPermission")
                    result.success(hasPermission)
                }
                "checkAllPermissions" -> {
                    val listenerEnabled = isNotificationListenerEnabled()
                    val notificationPermission = hasNotificationPermission()
                    val allPermissions = mapOf(
                        "listenerEnabled" to listenerEnabled,
                        "notificationPermission" to notificationPermission,
                        "allGranted" to (listenerEnabled && notificationPermission)
                    )
                    android.util.Log.d("MainActivity", "All permissions check: $allPermissions")
                    result.success(allPermissions)
                }
                "showTestNotification" -> {
                    // 테스트 알림 표시
                    showTestNotification()
                    result.success(true)
                }
                "updateUserUid" -> {
                    val arguments = call.arguments as? Map<String, Any>
                    val userUid = arguments?.get("userUid") as? String
                    if (userUid != null) {
                        updateUserUid(userUid)
                        result.success(true)
                    } else {
                        result.error("INVALID_ARGUMENT", "userUid is required", null)
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
        
        // 브로드캐스트 수신자 설정
        setupWithdrawalReceiver()
        
        // onCreate에서 인텐트 처리
        handleIntent(intent)
    }
    
    private fun setupWithdrawalReceiver() {
        withdrawalReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "kr.klr.stopusing.WITHDRAWAL_DETECTED" -> {
                        android.util.Log.d("MainActivity", "📨 Withdrawal broadcast received!")
                        
                        val withdrawalData = mapOf(
                            "packageName" to intent.getStringExtra("packageName"),
                            "appName" to intent.getStringExtra("appName"),
                            "amount" to intent.getLongExtra("amount", 0L),
                            "merchant" to intent.getStringExtra("merchant"),
                            "rawText" to intent.getStringExtra("rawText"),
                            "timestamp" to intent.getLongExtra("timestamp", 0L)
                        )
                        
                        android.util.Log.d("MainActivity", "📊 Withdrawal data: $withdrawalData")
                        
                        // Flutter로 데이터 전송
                        try {
                            methodChannel.invokeMethod("onWithdrawalDetected", withdrawalData)
                            android.util.Log.d("MainActivity", "✅ Successfully sent to Flutter via methodChannel")
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "💥 Error sending to Flutter: ${e.message}", e)
                        }
                    }
                    "kr.klr.stopusing.DIRECT_TEST_NOTIFICATION" -> {
                        android.util.Log.d("MainActivity", "🧪 Direct test notification broadcast received!")
                        
                        // 테스트 알림 생성
                        showTestNotification()
                        
                        android.util.Log.d("MainActivity", "✅ Test notification triggered via broadcast")
                    }
                }
            }
        }
        
        // 브로드캐스트 수신자 등록 (Android 14+ 호환)
        val filter = IntentFilter()
        filter.addAction("kr.klr.stopusing.WITHDRAWAL_DETECTED")
        filter.addAction("kr.klr.stopusing.DIRECT_TEST_NOTIFICATION")
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(withdrawalReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(withdrawalReceiver, filter)
        }
        android.util.Log.d("MainActivity", "🎯 Broadcast receivers registered (WITHDRAWAL + TEST_NOTIFICATION)")
    }
    
    private fun handleIntent(intent: Intent?) {
        android.util.Log.d("MainActivity", "🔍 Handling intent: ${intent?.action}")
        intent?.extras?.let { extras ->
            for (key in extras.keySet()) {
                android.util.Log.d("MainActivity", "📦 Intent extra: $key = ${extras.get(key)}")
            }
        }
        
        if (intent?.getStringExtra("action") == "test_notification") {
            android.util.Log.d("MainActivity", "🧪 Test notification requested via intent")
            showTestNotification()
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d("MainActivity", "🔄 onNewIntent called")
        handleIntent(intent)
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
        withdrawalReceiver?.let {
            unregisterReceiver(it)
            android.util.Log.d("MainActivity", "🗑️ Withdrawal broadcast receiver unregistered")
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val componentName = ComponentName(this, NotificationListenerService::class.java)
        val enabledNotificationListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        val componentString = componentName.flattenToString()
        
        android.util.Log.d("MainActivity", "Looking for component: $componentString")
        android.util.Log.d("MainActivity", "Enabled listeners: $enabledNotificationListeners")
        
        return enabledNotificationListeners?.contains(componentString) == true
    }

    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
    
    /**
     * Android 13+ 푸시 알림 권한 요청
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    /**
     * 푸시 알림 권한 확인
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13 미만은 자동으로 허용
            true
        }
    }
    
    /**
     * 권한 요청 결과 처리 (Deprecated이지만 여전히 지원됨)
     */
    @Deprecated("Deprecated in favor of registerForActivityResult")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                val granted = grantResults.isNotEmpty() && 
                             grantResults[0] == PackageManager.PERMISSION_GRANTED
                android.util.Log.d("MainActivity", "Notification permission result: $granted")
                
                // Flutter로 결과 전송
                try {
                    methodChannel.invokeMethod("onNotificationPermissionResult", granted)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error sending permission result: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 테스트 알림 표시 (세븐일레븐용인외대)
     */
    private fun showTestNotification() {
        try {
            android.util.Log.d("MainActivity", "🧪 테스트 알림 생성 시작: 세븐일레븐용인외대")
            
            // TransactionNotificationManager 인스턴스 생성
            val notificationManager = TransactionNotificationManager(this)
            
            // 세븐일레븐용인외대 테스트 거래 데이터 생성
            val testTransaction = TransactionResponse(
                id = 99999L,
                price = 5000L,
                title = "세븐일레븐용인외대",
                type = null,
                userUid = currentUserUid,
                category = null,
                createdAt = "2025-09-06T10:00:00Z",
                updatedAt = "2025-09-06T10:00:00Z",
                startedAt = "2025-09-06T10:00:00Z"
            )
            
            // 테스트 알림 표시
            notificationManager.showTransactionClassificationNotification(testTransaction)
            
            android.util.Log.d("MainActivity", "✅ 테스트 알림 생성 완료: ${testTransaction.title} (${testTransaction.price}원)")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "💥 테스트 알림 생성 실패: ${e.message}", e)
        }
    }
    
    /**
     * Load user UID from SharedPreferences
     */
    private fun loadUserUid() {
        try {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            currentUserUid = prefs.getString(USER_UID_KEY, "a") ?: "a"
            android.util.Log.d("MainActivity", "✅ User UID loaded: $currentUserUid")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "💥 Failed to load user UID: ${e.message}")
            currentUserUid = "a" // fallback to default
        }
    }
    
    /**
     * Update user UID and save to SharedPreferences
     */
    private fun updateUserUid(userUid: String) {
        try {
            currentUserUid = userUid
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(USER_UID_KEY, userUid)
            editor.apply()
            android.util.Log.d("MainActivity", "✅ User UID updated: $userUid")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "💥 Failed to update user UID: ${e.message}")
        }
    }
    
}
