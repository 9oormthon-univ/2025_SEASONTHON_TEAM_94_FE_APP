package com.example.stopusing_app.text

import android.util.Log
import com.example.stopusing_app.config.KoreanFinancialVocabulary
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 한국어 텍스트 문자 레벨 토큰화 처리 클래스 
 * 범용 AI 모델과 호환되는 문자 단위 토큰화
 */
class KoreanTextTokenizer {
    
    companion object {
        private const val TAG = "KoreanTextTokenizer"
        private const val UNK_TOKEN = 1
        private const val PAD_TOKEN = 0
    }
    
    /**
     * 실제 금융 알림 문자만 포함한 간단한 문자 사전
     */
    private val charVocab = buildMap<String, Int> {
        put("[PAD]", 0)
        put("[UNK]", 1)
        
        var idx = 2
        
        // 숫자
        for (i in '0'..'9') put(i.toString(), idx++)
        
        // 기본 한글 (실제 금융 알림에 나오는 문자들)
        val koreanChars = "가나다라마바사아자차카타파하어오우이" +
                "각간갈감갑강개객거건걸검겁게격견결경계고곤골공과관광구국군굴금급긱" +
                "날남남내너네노농누눈단달담답당대댁더덕던데도독돈돌동두둔뒤드득든등디" +
                "라락란랑래랜러럭런럴레령로록론롯료루룩룬룰리린립" +
                "마막만말맘맛망매맥맨머먹면멘모목몰몸무문물미민믹" +
                "바박반받발방배백버번벌벅본볼부북분불비빈빌빈빛" +
                "사삭산살삼상새색서석선설성세센소속손솔송수숙순술숨시식신실심" +
                "안알암압앙앞애액야약얀양어억언얼업에여연영예오옥온올용우욱운울원위유육은을음응의이인일임입잇자작잔잠장재전절정제조족존좀종주죽준중즉지직진질집" +
                "차착찬찻창체천철첫초총추출충치" +
                "타탁택탄탈탑테토통투특트특" +
                "파팍팬페폐폰표푸풀" +
                "하학한할함합항해핸허헥현헬혜호혹홍화환활황회횟후훈휴희" +
                "원금융은행카드체크출입송이승인결제"
        
        for (char in koreanChars) {
            put(char.toString(), idx++)
        }
        
        // 영문자 (은행 이름용)
        for (c in 'A'..'Z') put(c.toString(), idx++)
        for (c in 'a'..'z') put(c.toString(), idx++)
        
        // 실제 금융 알림에 나오는 특수문자만
        val specialChars = " .,:-()[]{}/*"
        for (char in specialChars) {
            put(char.toString(), idx++)
        }
    }
    
    /**
     * 한국어 텍스트를 문자 레벨 토큰 배열로 변환
     * 
     * @param text 변환할 텍스트
     * @return 토큰 ID 배열 (길이: MAX_SEQUENCE_LENGTH)
     */
    fun tokenizeText(text: String): IntArray {
        Log.d(TAG, "🔤 Character-level tokenizing text: $text")
        
        val tokens = mutableListOf<Int>()
        
        // 문자별 토큰화
        for (char in text) {
            val tokenId = charVocab[char.toString()] ?: UNK_TOKEN
            tokens.add(tokenId)
            
            if (tokens.size >= KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH) break
        }
        
        // 패딩 추가
        while (tokens.size < KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH) {
            tokens.add(PAD_TOKEN)
        }
        
        val result = tokens.take(KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH).toIntArray()
        Log.d(TAG, "✅ Character tokenization complete: ${result.size} tokens, vocab_size: ${charVocab.size}")
        
        return result
    }
    
    /**
     * 토큰 배열을 TensorFlow Lite 입력 버퍼로 변환
     * 
     * @param tokens 토큰 ID 배열
     * @return Float 형태의 ByteBuffer
     */
    fun createInputBuffer(tokens: IntArray): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        for (token in tokens) {
            inputBuffer.putFloat(token.toFloat())
        }
        
        Log.d(TAG, "🔄 Input buffer created: ${inputBuffer.capacity()} bytes")
        return inputBuffer
    }
    
    /**
     * 텍스트에서 금액 추출 (패턴 매칭 보조)
     */
    fun extractAmountPattern(text: String): Long? {
        val amountPattern = Regex("([0-9,]+)(?=원|\\s)")
        return amountPattern.find(text)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
    }
    
    /**
     * 텍스트에서 가맹점명 추출 (패턴 매칭 보조)
     */
    fun extractMerchantPattern(text: String): String? {
        val merchantPattern = Regex("([가-힣]{2,10})(?=\\s+(출금|결제|이체|승인))")
        return merchantPattern.find(text)?.groupValues?.get(1)
    }
    
    /**
     * 텍스트에서 거래 유형 추출 (패턴 매칭 보조)
     */
    fun extractTransactionType(text: String): String? {
        for ((keyword, type) in KoreanFinancialVocabulary.TRANSACTION_TYPES) {
            if (text.contains(keyword)) {
                return type
            }
        }
        return null
    }
}