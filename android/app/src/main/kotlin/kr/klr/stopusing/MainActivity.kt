package kr.klr.stopusing

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "kr.klr.stopusing/notification_listener"
    private lateinit var methodChannel: MethodChannel
    private var withdrawalReceiver: BroadcastReceiver? = null
    
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        
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
                else -> {
                    result.notImplemented()
                }
            }
        }
        
        // 브로드캐스트 수신자 설정
        setupWithdrawalReceiver()
    }
    
    private fun setupWithdrawalReceiver() {
        withdrawalReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "kr.klr.stopusing.WITHDRAWAL_DETECTED") {
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
            }
        }
        
        // 브로드캐스트 수신자 등록 (Android 14+ 호환)
        val filter = IntentFilter("kr.klr.stopusing.WITHDRAWAL_DETECTED")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(withdrawalReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(withdrawalReceiver, filter)
        }
        android.util.Log.d("MainActivity", "🎯 Withdrawal broadcast receiver registered")
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
    
}
