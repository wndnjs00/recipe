package nbc.group.recipes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nbc.group.recipes.convertToOfficial
import nbc.group.recipes.data.model.dto.Item
import nbc.group.recipes.data.model.dto.SpecialtyResponse
import nbc.group.recipes.data.repository.SpecialtyRepository
import javax.inject.Inject

@HiltViewModel
class MapSharedViewModel @Inject constructor(
    private val repository: SpecialtyRepository,
) : ViewModel(){


    // 지역이름
    private val _specialtie = MutableStateFlow<List<Item>?>(null)
    val specialtie : StateFlow<List<Item>?> = _specialtie

    fun getSpecialtie(ariaName : String) = viewModelScope.launch {
        val getAriaData = repository.getAreaName(ariaName)
        val filteredItems = getAriaData.body.items.item?.mapNotNull { item ->
            try {
                item.cntntsSj = convertToOfficial(item.cntntsSj ?: "")
                item
            } catch (e: Exception) {
                null
            }
        }?.filter {
            // 제거해야되는 특산물데이터 필터링
            when(ariaName){
                "양주" -> it.cntntsSj !in setOf("반려식물", "요거트", "기타","전통장류/기름류","건강차/즙/환/분말","꽃차","현미","쌈채류","과채류")
                "통영" -> it.cntntsSj !in setOf("고구마 빼떼기죽", "미나리진액", "동백·유자오일","고구마 쫀득이","동백·유자오일","유자청")
                "대전" -> it.cntntsSj !in setOf("황토햇쌀","친환경 미르쌀","화훼", "과수", "국화", "관엽류", "절화", "초화류", "색이 있는 아침한술")
                "군산" -> it.cntntsSj !in setOf("바다향","내고향시푸드", "효송그린푸드", "영국빵집", "서해푸드 주식회사", "더 미들래", "옹고집영농종합법인", "풍년보리원", "어울림", "회현 농협 RPC", "옥구 농협 RPC", "생금들","통영풍난","아리울수산","K-FOOD","아리울현푸드","옥산한과","중수비영농조합법인")
                "파주" -> it.cntntsSj !in setOf("장미", "파주농특산물 홍보관")
                "인천" -> it.cntntsSj !in setOf("식용곤충", "쌈채류단지","용유고추단지","영종고추단지","배(탑프루트)단지","강화고추","강화속노랑고구마","토마토단지","강화섬쌀","강화섬포도")
                "서산" -> it.cntntsSj !in setOf("양난", "서산육쪽마늘","생강조청","생강한과","서산육쪽마늘")
                "함양" -> it.cntntsSj !in setOf("함양농업가공사업소", "지리산마천농협", "우리가", "장충동 B&F", "옻","인삼죽염","삼민목장","내수면 철갑상어","산양삼","지리산산골흑돼지","감말랭이","새송이 버섯")
                "영암" -> it.cntntsSj !in setOf("영암도기", "영암어란", "금정토하젓", "달빛 무화과 쌀빵", "기타특산물","매력한우","대봉감","달빛 무화과 쌀빵")
                "봉화" -> it.cntntsSj !in setOf("파인토피아 봉화", "화훼","한약우")
                "창녕" -> it.cntntsSj !in setOf("파인토피아 봉화", "화훼","한약우","공정육묘","창녕고추","창녕 송이버섯")
                "진주" -> it.cntntsSj !in setOf("㈜석광아이티에스", "대원산업㈜", "(주)한국바이오케미칼", "㈜한국바이오케미칼", "금포영농조합법인", "민희영농조합법인", "㈜참진정", "진주시농협쌀조합공동사업법인", "송이식품", "문산머쉬영농조합법인", "햇살담은자연마을영농조합법인", "대호영농조합법인", "주흥미곡처리장", "㈜한일식품", "건웅식품㈜", "하봉정푸드", "반성전통한과", "싱싱단감농장","㈜실키안","대나무숯","호접란","우엉마영농조합법인","꽈리고추","싱싱단감농장")
                "포항" -> it.cntntsSj !in setOf("화전", "송화밀수", "토란탕", "구름떡","팥죽","팥시루떡","노비송편","약식","떡산적","부추해물 잡채")
                "대구" -> it.cntntsSj !in setOf("친환경농산물", "시설채소", "화원 본리화훼","유가 한정양파","다사 이천참외","옥포 참외","옥포 수박","논공 찰토마토","하빈 기곡포도","구지 예현마늘")
                "김해" -> it.cntntsSj !in setOf("서양란", "금어초", "숙근안개초", "거베라", "카네이션", "국화", "장미","딸기")
                "여수" -> it.cntntsSj !in setOf("향토 지키미", "여수동백화장품","금오도잎방풍","거문도해풍쑥",)
                "전주" -> it.cntntsSj !in setOf("장미", "국화")
                "횡성" -> it.cntntsSj != "안흥찐빵"
                "보령" -> it.cntntsSj !in setOf("감미료", "전통식품", "냉품삼", "머드제품","유제품","한과","전통주")
                "제천" -> it.cntntsSj !in setOf("박달재 식품", "홍화", "황초와우", "약초향기주머니","참옻된장","토종흑염소중탕","제천약초","곡류")
                "완도" -> it.cntntsSj !in setOf("비파", "삼지구엽초")
                "남해" -> it.cntntsSj !in setOf("남해삼베", "만복당향", "한방향", "우렁쉥이", "비자", "멸치액젓","우리밀국수","유자주","멸치액젓","유자주")
                "연천" -> it.cntntsSj !in setOf("남土북水쌀","양계", "양돈", "누에환, 동충하초, 뽕잎차","오색소반김치")
                "합천" -> it.cntntsSj !in setOf("기능성소금선물세트", "도자기", "황매산 친환경 밤묵", "전통 장류", "합천 육묘장", "봉산건강원", "자생담","발효차","허굴농차","황토소금","우렁이쌀","황매산 친환경 밤묵","양떡메(양파즙, 떡가래, 메주)")
                "산청" -> it.cntntsSj !in setOf("양계", "양돈", "누에환, 동충하초, 뽕잎차")
                "세종" -> it.cntntsSj !in setOf("인삼포크진생원")
                "울릉" -> it.cntntsSj !in setOf("부지갱이","미역취","서덜취","음나무","섬초롱꽃","참고비(섬고사리)","삼나물(눈개승마)","두메부추","땅두릅","참고비(섬고사리)")
                "부산" -> it.cntntsSj !in setOf("기장채소")
                "평택" -> it.cntntsSj !in setOf("이앤미떡","방울토마토")
                "거제" -> it.cntntsSj !in setOf("하청면  맹종죽(죽순), 풋고추, 유자, 치자")
                "정읍" -> it.cntntsSj != "단풍미인방울토마토"
                else -> true
            }
        }
        _specialtie.emit(filteredItems)
    }



    // 클릭한 특산물 데이터
    private val _selectedSpecialty = MutableStateFlow<Item?>(null)
    val selectedSpecialty: StateFlow<Item?> = _selectedSpecialty

    fun getSelectedSpecialty(item: Item?) {
        _selectedSpecialty.value = item
    }

}