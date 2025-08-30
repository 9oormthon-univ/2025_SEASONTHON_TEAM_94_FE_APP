package kr.klr.stopusing.data

/**
 * AI 파싱 결과를 담는 데이터 클래스
 */
data class TransactionParseResult(
    /** 거래 금액 (원) */
    val amount: Long?,
    
    /** 가맹점/상대방 이름 */
    val merchant: String?,
    
    /** 거래 유형 (출금/결제/이체 등) */
    val transactionType: String?,
    
    /** AI 예측 신뢰도 (0.0 ~ 1.0) */
    val confidence: Double,
    
    /** 파싱 방법 (AI_MODEL/AI_SIMULATION 등) */
    val method: String,
    
    /** 상세 정보 및 디버그 메시지 */
    val details: String = ""
) {
    /**
     * 파싱이 성공했는지 확인
     */
    fun isSuccessful(): Boolean = amount != null && amount > 0
    
    /**
     * 높은 신뢰도 결과인지 확인
     */
    fun isHighConfidence(): Boolean = confidence >= 0.7
    
    /**
     * 파싱 결과 요약 정보
     */
    fun getSummary(): String {
        return if (isSuccessful()) {
            "${merchant ?: "알 수 없음"} ${amount}원 ${transactionType ?: "거래"} (신뢰도: ${String.format("%.2f", confidence)})"
        } else {
            "파싱 실패: $details"
        }
    }
}