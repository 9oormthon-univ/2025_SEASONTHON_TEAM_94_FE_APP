package kr.klr.stopusing.data

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

/**
 * 백엔드 API에서 반환되는 래핑된 응답 구조
 */
data class ApiResponse(
    val success: Boolean,
    val status: Int,
    val code: String,
    val message: String,
    val data: TransactionResponse
)

/**
 * 실제 거래 데이터 클래스
 */
data class TransactionResponse(
    val id: Long,
    val price: Long,
    val title: String,
    val type: String?,
    @SerializedName("userUid")
    val userUid: String,
    val category: String?,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String,
    @SerializedName("startedAt")
    val startedAt: String
)

/**
 * 거래 유형 업데이트 요청을 위한 데이터 클래스
 */
data class TransactionTypeUpdateRequest(
    val type: TransactionType,
    val userUid: String = "a"  // 고정값 "a"로 설정
)

/**
 * 거래 유형 enum
 */
enum class TransactionType(val value: String) {
    OVER_EXPENSE("OVER_EXPENSE"),     // 초과지출
    FIXED_EXPENSE("FIXED_EXPENSE");   // 고정지출
    
    override fun toString(): String = value
}