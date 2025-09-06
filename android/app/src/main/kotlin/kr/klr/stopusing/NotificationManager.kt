package kr.klr.stopusing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kr.klr.stopusing.data.TransactionResponse

/**
 * 거래 분류 알림 관리자
 */
class TransactionNotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "TransactionNotificationManager"
        private const val CHANNEL_ID = "transaction_classification"
        private const val CHANNEL_NAME = "그만써! 거래 분류"
        private const val CHANNEL_DESCRIPTION = "그만써!에서 지출 거래를 고정지출 또는 지출로 분류"
        
        // Action constants
        const val ACTION_FIXED_EXPENSE = "kr.klr.stopusing.ACTION_FIXED_EXPENSE"
        const val ACTION_OVER_EXPENSE = "kr.klr.stopusing.ACTION_OVER_EXPENSE"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_TRANSACTION_TITLE = "transaction_title"
        const val EXTRA_TRANSACTION_PRICE = "transaction_price"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * 알림 권한 체크
     */
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }
    
    /**
     * 알림 채널 생성 (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "✅ 거래 분류 알림 채널 생성 완료")
        }
    }
    
    /**
     * 거래 분류 알림 표시
     */
    fun showTransactionClassificationNotification(transaction: TransactionResponse) {
        try {
            // 알림 권한 체크
            if (!checkNotificationPermission()) {
                Log.w(TAG, "🚫 알림 권한이 없습니다. 설정에서 알림 권한을 허용해주세요.")
                Log.w(TAG, "💡 설정 > 앱 > 그만써! > 알림 권한 확인 필요")
                return
            }
            
            Log.d(TAG, "📱 거래 분류 알림 표시: ${transaction.title} (${transaction.price}원)")
            
            // 고정지출 버튼 액션
            val fixedExpenseIntent = Intent(context, TransactionActionReceiver::class.java).apply {
                action = ACTION_FIXED_EXPENSE
                putExtra(EXTRA_TRANSACTION_ID, transaction.id)
                putExtra(EXTRA_TRANSACTION_TITLE, transaction.title)
                putExtra(EXTRA_TRANSACTION_PRICE, transaction.price)
            }
            
            val fixedExpensePendingIntent = PendingIntent.getBroadcast(
                context,
                (transaction.id.toInt() * 2), // 고유한 request code
                fixedExpenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // 초과지출 버튼 액션
            val overExpenseIntent = Intent(context, TransactionActionReceiver::class.java).apply {
                action = ACTION_OVER_EXPENSE
                putExtra(EXTRA_TRANSACTION_ID, transaction.id)
                putExtra(EXTRA_TRANSACTION_TITLE, transaction.title)
                putExtra(EXTRA_TRANSACTION_PRICE, transaction.price)
            }
            
            val overExpensePendingIntent = PendingIntent.getBroadcast(
                context,
                (transaction.id.toInt() * 2 + 1), // 고유한 request code
                overExpenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // 알림 빌드
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("💳 그만써! - 지출 분류 필요")
                .setContentText("${transaction.title} - ${formatPrice(transaction.price)}원")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("${transaction.title}\n${formatPrice(transaction.price)}원\n\n이 지출을 어떻게 분류하시겠습니까?")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(false)
                .setWhen(System.currentTimeMillis())
                .addAction(
                    android.R.drawable.ic_menu_save,
                    "고정지출",
                    fixedExpensePendingIntent
                )
                .addAction(
                    android.R.drawable.ic_dialog_alert,
                    "지출", 
                    overExpensePendingIntent
                )
                .build()
            
            // 알림 표시
            val notificationId = transaction.id.toInt()
            try {
                notificationManager.notify(notificationId, notification)
                Log.d(TAG, "✅ 거래 분류 알림 표시 완료: ID $notificationId")
            } catch (e: SecurityException) {
                Log.e(TAG, "🚫 알림 권한이 거부됨: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "💥 알림 표시 중 오류: ${e.message}", e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 거래 분류 알림 표시 실패", e)
        }
    }
    
    /**
     * 알림 제거
     */
    fun dismissNotification(transactionId: Long) {
        try {
            NotificationManagerCompat.from(context).cancel(transactionId.toInt())
            Log.d(TAG, "🗑️ 거래 분류 알림 제거: ID $transactionId")
        } catch (e: Exception) {
            Log.e(TAG, "💥 알림 제거 실패", e)
        }
    }
    
    /**
     * 금액 포맷팅 (천 단위 구분)
     */
    private fun formatPrice(price: Long): String {
        return "%,d".format(price)
    }
}