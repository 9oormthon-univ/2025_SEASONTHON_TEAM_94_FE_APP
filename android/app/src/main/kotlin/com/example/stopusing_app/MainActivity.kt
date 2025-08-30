package com.example.stopusing_app

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.stopusing_app/notification_listener"
    private lateinit var methodChannel: MethodChannel
    private var withdrawalReceiver: BroadcastReceiver? = null

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
                if (intent?.action == "com.example.stopusing_app.WITHDRAWAL_DETECTED") {
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
        val filter = IntentFilter("com.example.stopusing_app.WITHDRAWAL_DETECTED")
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
    
}
