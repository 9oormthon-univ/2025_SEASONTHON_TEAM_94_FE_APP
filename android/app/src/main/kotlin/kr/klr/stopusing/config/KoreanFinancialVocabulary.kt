package kr.klr.stopusing.config

/**
 * 한국어 금융 도메인 어휘 및 상수 관리
 */
object KoreanFinancialVocabulary {
    
    /** Ultimate TimeDistributed NER 모델 파일명 (소액 거래 포함) */
    const val MODEL_FILE = "korean_financial_ultimate_ner.tflite"
    
    /** AI 파싱 최소 신뢰도 임계값 */
    const val AI_CONFIDENCE_THRESHOLD = 0.50
    
    /** 최대 토큰 시퀀스 길이 (Dense 모델용) */
    const val MAX_SEQUENCE_LENGTH = 200
    
    /** 
     * Ultimate TimeDistributed NER 모델 정보
     * - 문자 레벨 토큰화로 모든 한국 금융앱 지원
     * - 1원~50만원 완전한 금액 범위 커버
     * - TimeDistributed Dense로 토큰별 정확한 NER 분류
     */
    const val VOCAB_SIZE = 396  // 실제 문자 어휘 크기 (메타데이터 기준) - 역, 브, 혁 등 추가 문자 포함
    const val NUM_CLASSES = 7   // NER 레이블 수 (O, B-AMOUNT, I-AMOUNT, B-MERCHANT, I-MERCHANT, B-DATE, I-DATE)
    
    /**
     * NER 레이블 정의 (범용 모델)
     */
    object NerLabels {
        const val OUTSIDE = 0          // O
        const val BEGIN_AMOUNT = 1     // B-AMOUNT
        const val INSIDE_AMOUNT = 2    // I-AMOUNT
        const val BEGIN_MERCHANT = 3   // B-MERCHANT
        const val INSIDE_MERCHANT = 4  // I-MERCHANT
        const val BEGIN_DATE = 5       // B-DATE
        const val INSIDE_DATE = 6      // I-DATE
        
        val LABEL_NAMES = mapOf(
            OUTSIDE to "O",
            BEGIN_AMOUNT to "B-AMOUNT",
            INSIDE_AMOUNT to "I-AMOUNT",
            BEGIN_MERCHANT to "B-MERCHANT",
            INSIDE_MERCHANT to "I-MERCHANT",
            BEGIN_DATE to "B-DATE",
            INSIDE_DATE to "I-DATE"
        )
    }
    
    /**
     * 거래 유형 매핑
     */
    val TRANSACTION_TYPES = mapOf(
        "출금" to "출금",
        "결제" to "결제",
        "이체" to "이체",
        "송금" to "송금",
        "현금출금" to "출금",
        "ATM출금" to "출금",
        "스마트폰출금" to "출금"
    )
}