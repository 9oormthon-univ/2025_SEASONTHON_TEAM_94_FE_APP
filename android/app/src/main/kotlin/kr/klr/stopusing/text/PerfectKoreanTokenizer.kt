package kr.klr.stopusing.text

import android.util.Log
import kr.klr.stopusing.config.KoreanFinancialVocabulary
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 완벽한 한국어 Dense AI 모델용 토크나이저
 * Python 모델과 100% 호환되는 문자 매핑
 */
class PerfectKoreanTokenizer {
    
    companion object {
        private const val TAG = "PerfectKoreanTokenizer"
        private const val UNK_TOKEN = 1
        private const val PAD_TOKEN = 0
    }
    
    /**
     * Python 모델과 완전히 동일한 문자 매핑 (metadata 기반)
     */
    private val charVocab = mapOf(
        "[PAD]" to 0, "[UNK]" to 1, " " to 2, "(" to 3, ")" to 4, "*" to 5, "+" to 6, "," to 7, "-" to 8, "." to 9,
        "/" to 10, "0" to 11, "1" to 12, "2" to 13, "3" to 14, "4" to 15, "5" to 16, "6" to 17, "7" to 18, "8" to 19,
        "9" to 20, ":" to 21, "A" to 22, "B" to 23, "C" to 24, "D" to 25, "E" to 26, "F" to 27, "G" to 28, "H" to 29,
        "I" to 30, "J" to 31, "K" to 32, "L" to 33, "M" to 34, "N" to 35, "O" to 36, "P" to 37, "Q" to 38, "R" to 39,
        "S" to 40, "T" to 41, "U" to 42, "V" to 43, "W" to 44, "X" to 45, "Y" to 46, "Z" to 47, "[" to 48, "]" to 49,
        "a" to 50, "b" to 51, "c" to 52, "d" to 53, "e" to 54, "f" to 55, "g" to 56, "h" to 57, "i" to 58, "j" to 59,
        "k" to 60, "l" to 61, "m" to 62, "n" to 63, "o" to 64, "p" to 65, "q" to 66, "r" to 67, "s" to 68, "t" to 69,
        "u" to 70, "v" to 71, "w" to 72, "x" to 73, "y" to 74, "z" to 75, "{" to 76, "}" to 77, "←" to 78, "→" to 79,
        "★" to 80, "가" to 81, "각" to 82, "간" to 83, "갈" to 84, "감" to 85, "갑" to 86, "강" to 87, "개" to 88,
        "객" to 89, "거" to 90, "건" to 91, "걸" to 92, "검" to 93, "겁" to 94, "게" to 95, "격" to 96, "견" to 97,
        "결" to 98, "경" to 99, "계" to 100, "고" to 101, "곤" to 102, "골" to 103, "공" to 104, "과" to 105, "관" to 106,
        "광" to 107, "구" to 108, "국" to 109, "군" to 110, "굴" to 111, "금" to 112, "급" to 113, "긱" to 114, "날" to 115,
        "남" to 116, "내" to 117, "너" to 118, "네" to 119, "노" to 120, "농" to 121, "누" to 122, "눈" to 123, "단" to 124,
        "달" to 125, "담" to 126, "답" to 127, "당" to 128, "대" to 129, "댁" to 130, "더" to 131, "덕" to 132, "던" to 133,
        "데" to 134, "도" to 135, "독" to 136, "돈" to 137, "돌" to 138, "동" to 139, "두" to 140, "둔" to 141, "뒤" to 142,
        "드" to 143, "득" to 144, "든" to 145, "등" to 146, "디" to 147, "라" to 148, "락" to 149, "란" to 150, "랑" to 151,
        "래" to 152, "랜" to 153, "러" to 154, "럭" to 155, "런" to 156, "럴" to 157, "레" to 158, "령" to 159, "로" to 160,
        "록" to 161, "론" to 162, "롯" to 163, "료" to 164, "루" to 165, "룩" to 166, "룬" to 167, "룰" to 168, "리" to 169,
        "린" to 170, "립" to 171, "마" to 172, "막" to 173, "만" to 174, "말" to 175, "맘" to 176, "맛" to 177, "망" to 178,
        "매" to 179, "맥" to 180, "맨" to 181, "머" to 182, "먹" to 183, "멘" to 184, "면" to 185, "모" to 186, "목" to 187,
        "몰" to 188, "몸" to 189, "무" to 190, "문" to 191, "물" to 192, "미" to 193, "믹" to 194, "민" to 195, "바" to 196,
        "박" to 197, "반" to 198, "받" to 199, "발" to 200, "방" to 201, "배" to 202, "백" to 203, "버" to 204, "벅" to 205,
        "번" to 206, "벌" to 207, "본" to 208, "볼" to 209, "부" to 210, "북" to 211, "분" to 212, "불" to 213, "비" to 214,
        "빈" to 215, "빌" to 216, "빛" to 217, "사" to 218, "삭" to 219, "산" to 220, "살" to 221, "삼" to 222, "상" to 223,
        "새" to 224, "색" to 225, "서" to 226, "석" to 227, "선" to 228, "설" to 229, "성" to 230, "세" to 231, "센" to 232,
        "소" to 233, "속" to 234, "손" to 235, "솔" to 236, "송" to 237, "수" to 238, "숙" to 239, "순" to 240, "술" to 241,
        "숨" to 242, "승" to 243, "시" to 244, "식" to 245, "신" to 246, "실" to 247, "심" to 248, "안" to 249, "알" to 250,
        "암" to 251, "압" to 252, "앙" to 253, "앞" to 254, "애" to 255, "액" to 256, "야" to 257, "약" to 258, "얀" to 259,
        "양" to 260, "어" to 261, "억" to 262, "언" to 263, "얼" to 264, "업" to 265, "에" to 266, "여" to 267, "연" to 268,
        "영" to 269, "예" to 270, "오" to 271, "옥" to 272, "온" to 273, "올" to 274, "용" to 275, "우" to 276, "욱" to 277,
        "운" to 278, "울" to 279, "원" to 280, "위" to 281, "유" to 282, "육" to 283, "융" to 284, "은" to 285, "을" to 286,
        "음" to 287, "응" to 288, "의" to 289, "이" to 290, "인" to 291, "일" to 292, "임" to 293, "입" to 294, "잇" to 295,
        "자" to 296, "작" to 297, "잔" to 298, "잠" to 299, "장" to 300, "재" to 301, "전" to 302, "절" to 303, "정" to 304,
        "제" to 305, "조" to 306, "족" to 307, "존" to 308, "좀" to 309, "종" to 310, "주" to 311, "죽" to 312, "준" to 313,
        "중" to 314, "즉" to 315, "지" to 316, "직" to 317, "진" to 318, "질" to 319, "집" to 320, "차" to 321, "착" to 322,
        "찬" to 323, "찻" to 324, "창" to 325, "천" to 326, "철" to 327, "첫" to 328, "체" to 329, "초" to 330, "총" to 331,
        "추" to 332, "출" to 333, "충" to 334, "치" to 335, "카" to 336, "크" to 337, "타" to 338, "탁" to 339, "탄" to 340,
        "탈" to 341, "탑" to 342, "택" to 343, "테" to 344, "토" to 345, "통" to 346, "투" to 347, "트" to 348, "특" to 349,
        "파" to 350, "팍" to 351, "팬" to 352, "페" to 353, "폐" to 354, "폰" to 355, "표" to 356, "푸" to 357, "풀" to 358,
        "하" to 359, "학" to 360, "한" to 361, "할" to 362, "함" to 363, "합" to 364, "항" to 365, "해" to 366, "핸" to 367,
        "행" to 368, "허" to 369, "헥" to 370, "헬" to 371, "현" to 372, "혜" to 373, "호" to 374, "혹" to 375, "홍" to 376,
        "화" to 377, "환" to 378, "활" to 379, "황" to 380, "회" to 381, "횟" to 382, "후" to 383, "훈" to 384, "휴" to 385,
        "희" to 386
    )
    
    /**
     * 완벽한 문자 레벨 토큰화 (Python 모델과 100% 호환)
     */
    fun tokenizeText(text: String): IntArray {
        Log.d(TAG, "🔤 Perfect tokenizing: $text")
        
        val tokens = mutableListOf<Int>()
        
        // 문자별 토큰화 (Python 모델과 동일)
        for (char in text) {
            val tokenId = charVocab[char.toString()] ?: UNK_TOKEN
            tokens.add(tokenId)
            
            if (tokens.size >= KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH) break
        }
        
        // 패딩 추가 (Python 모델과 동일)
        while (tokens.size < KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH) {
            tokens.add(PAD_TOKEN)
        }
        
        val result = tokens.take(KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH).toIntArray()
        Log.d(TAG, "✅ Perfect tokenization: ${result.size} tokens, vocab_size: ${charVocab.size}")
        
        return result
    }
    
    /**
     * TensorFlow Lite 입력 버퍼 생성
     */
    fun createInputBuffer(tokens: IntArray): ByteBuffer {
        val inputBuffer = ByteBuffer.allocateDirect(KoreanFinancialVocabulary.MAX_SEQUENCE_LENGTH * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        Log.d(TAG, "🔤 Token validation: length=${tokens.size}, max_vocab=${KoreanFinancialVocabulary.VOCAB_SIZE}")
        
        var validTokens = 0
        var invalidTokens = 0
        
        for (token in tokens) {
            if (token >= 0 && token < KoreanFinancialVocabulary.VOCAB_SIZE) {
                validTokens++
            } else {
                invalidTokens++
                Log.w(TAG, "⚠️ Invalid token: $token (out of vocab range)")
            }
            inputBuffer.putFloat(token.toFloat())
        }
        
        Log.d(TAG, "🔄 Perfect input buffer: ${inputBuffer.capacity()} bytes")
        Log.d(TAG, "📊 Token stats: valid=$validTokens, invalid=$invalidTokens")
        return inputBuffer
    }
    
    /**
     * 패턴 매칭 백업 (AI 실패시에만 사용) - 개선된 버전
     */
    fun extractAmountPattern(text: String): Long? {
        Log.d(TAG, "🔍 [BACKUP] Extracting amount from: $text")
        
        // 더 포괄적인 금액 패턴들
        val patterns = listOf(
            // 기본: 숫자+원
            Regex("([0-9,]+)원"),
            // 숫자+공백+원
            Regex("([0-9,]+)\\s*원"),
            // 숫자만 (연속 3자리 이상)
            Regex("([0-9,]{3,})"),
            // 콤마 포함 숫자
            Regex("([0-9]{1,3}(?:,[0-9]{3})*)원?")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1]
                val amount = amountStr.replace(",", "").toLongOrNull()
                
                // 유효성 검증 (1원 ~ 1억원)
                if (amount != null && amount > 0 && amount <= 100_000_000) {
                    Log.d(TAG, "✅ [BACKUP] Found amount: $amount")
                    return amount
                }
            }
        }
        
        Log.d(TAG, "❌ [BACKUP] No amount found")
        return null
    }
    
    fun extractMerchantPattern(text: String): String? {
        Log.d(TAG, "🔍 [BACKUP] Extracting merchant from: $text")
        
        // 더 포괄적인 백업 패턴들
        val patterns = listOf(
            // 거래자명 패턴 (님 붙은)
            Regex("([가-힣*]{2,})님"),
            // 상호명 패턴 (한글 2-10자)
            Regex("([가-힣]{2,10})(?=\\s*(체크카드|결제|출금|송금|이체))"),
            // 역순 패턴
            Regex("(?:체크카드|결제|출금|송금|이체)\\s*([가-힣]{2,10})"),
            // 일반 상호명 (앞뒤 공백 있는)
            Regex("\\s([가-힣]{3,8})\\s"),
            // 한글만 연속으로
            Regex("([가-힣]{3,})")
        )
        
        val financialKeywords = setOf("은행", "뱅크", "출금", "입금", "이체", "송금", "결제", "승인", 
                                     "잔액", "전자", "금융", "체크", "신용", "카드", "원", "ATM")
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val merchant = match.groupValues[1].replace("*", "").trim()
                
                // 유효성 검증
                if (merchant.isNotBlank() && 
                    merchant.length >= 2 && 
                    !financialKeywords.any { merchant.contains(it) }) {
                    Log.d(TAG, "✅ [BACKUP] Found merchant: $merchant")
                    return merchant
                }
            }
        }
        
        Log.d(TAG, "❌ [BACKUP] No merchant found")
        return null
    }
    
    fun extractTransactionType(text: String): String? {
        Log.d(TAG, "🔍 [BACKUP] Determining transaction type from: $text")
        
        val type = when {
            // 입금 관련 키워드 우선 처리
            text.contains("입금") || text.contains("전자금융입금") -> "입금"
            text.contains("송금") -> "송금" 
            text.contains("이체") -> "이체"
            text.contains("출금") -> "출금"
            text.contains("결제") -> "결제"
            text.contains("승인") -> "결제"
            // 기본값
            else -> "출금"
        }
        
        Log.d(TAG, "✅ [BACKUP] Transaction type determined: $type")
        return type
    }
}