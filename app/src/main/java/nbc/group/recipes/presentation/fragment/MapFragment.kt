package nbc.group.recipes.presentation.fragment

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State.*
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraPosition
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTransition
import com.kakao.vectormap.label.Transition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nbc.group.recipes.data.model.dto.BaseMapResponse
import nbc.group.recipes.ChipType
import nbc.group.recipes.R
import nbc.group.recipes.data.model.dto.SearchDocumentsResponse
import nbc.group.recipes.data.network.KAKAO_MAP_KEY
import nbc.group.recipes.databinding.FragmentMapBinding
import nbc.group.recipes.databinding.RegionAlramDialogBinding
import nbc.group.recipes.viewmodel.MapSharedViewModel
import nbc.group.recipes.viewmodel.MapViewModel
import java.lang.Exception
import java.util.Locale

@AndroidEntryPoint
class MapFragment : Fragment() {

    /**
     *
     * 검색어를 입력하고 확인을 누르면
     * 뷰모델의 stateflow에 값을 emit한다.
     *
     * 해당 stateflow를 관찰하고 있다가
     * 값이 emit되면 collect의 람다에서 마커를 지도에 그리는 로직을 수행한다.
     * stateflow의 값이 null이면 지도에 있는 마커를 제거한다.
     *
     *
     * */

    private val binding get() = _binding!!
    private var _binding: FragmentMapBinding? = null

    private lateinit var mapView: MapView
    private var kakaoMap : KakaoMap? = null

    private val mapViewModel: MapViewModel by viewModels()
    private val mapSharedViewModel : MapSharedViewModel by activityViewModels()

    private var searchDocumentsResponse: SearchDocumentsResponse? = null
    private var lastClickTime : Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showMapView()
        setSearch()
        observeViewModel()
        chipGroup()
        deleteText()
    }


    // 지도 띄우는 함수
    private fun showMapView() {

        mapView = binding.mapView

        // KakaoMapSDK 초기화!!
        KakaoMapSdk.init(requireContext(), KAKAO_MAP_KEY)

        mapView.start(object : MapLifeCycleCallback() {

            override fun onMapDestroy() {
                Log.d("KakaoMap", "onMapDestroy")
            }

            override fun onMapError(p0: Exception?) {
                Log.e("KakaoMap", "onMapError")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaomap: KakaoMap) {

                kakaoMap = kakaomap
                kakaomap.setGestureEnable(GestureType.OneFingerDoubleTap, false)
                kakaomap.setGestureEnable(GestureType.Zoom, false)
                kakaomap.setGestureEnable(GestureType.OneFingerZoom, false)

                kakaomap.setOnLabelClickListener { kakaoMap, labelLayer, label ->

                    val currentTime = SystemClock.elapsedRealtime() // currentTime에 현재시점을 저장
                    // 마지막 클릭시간인 lastClickTime과 햔재시점 currentTime의 차이를 게산 => 이 값이 3초보다 작으면 연속클릭으로 간주하여 이벤트를 종료
                    if(currentTime - lastClickTime < 3000){
                        // 클릭 이벤트 종료
                        return@setOnLabelClickListener
                    }
                    // 이벤트가 정상적으로 처리되었을때, lastClickTime을 현재시간으로 갱신
                    lastClickTime = currentTime

                    val searchText = binding.searchEt.text.toString()
                    val latitude = label.position.latitude
                    val longitude = label.position.longitude

                    val regionName = if (searchText.isNotEmpty()) {
                        getRegionName(latitude, longitude)
                    } else {
                        AllgetRegionName(latitude, longitude)
                    }
                    binding.searchEt.setText(regionName)

                    if (getRegionName(latitude, longitude).isNotEmpty()) {
                        mapSharedViewModel.getSpecialtie(getRegionName(latitude, longitude))

                        val bottomSheetFragment = BottomSheetFragment.newInstance(regionName)
                        bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
                    } else {
                        Snackbar.make(binding.searchEt, "해당 지역은 특산물 데이터가 없습니다", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }

            override fun getZoomLevel() = 8
        })
    }


    private fun setSearch() = with(binding){

        searchEt.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_SEARCH){

                val searchText = searchEt.text.toString()

                // 검색할 수 있는 지역이 포함되어있으면
                if (AllcontainsRegoin(searchText)){
                    mapViewModel.getRegionSearch(searchText)
                }else{
                    // 지역이 아닌 다른것이 포함되어있으면 (영어,기호,외계어..)
                    Snackbar.make(searchEt, "검색할수없는 지역입니다", Snackbar.LENGTH_SHORT).show()
                }

                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(requireActivity().window.decorView.applicationWindowToken, 0)

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }


    // 해당위치로 이동, 라벨 표시
    private fun setMapData(searchDocumentsResponse: SearchDocumentsResponse) {
        // 좌표값 포맷팅
        val latitude = searchDocumentsResponse.y
        val longitude = searchDocumentsResponse.x
        val latitude_formatter = String.format("%.6f", latitude).toDouble()
        val longitude_formatter = String.format("%.6f", longitude).toDouble()

        // 이동할 위치(위도,경도) 설정
        val camera = CameraUpdateFactory.newCenterPosition(LatLng.from(latitude_formatter, longitude_formatter))
        // 해당위치로 지도 이동
        kakaoMap?.moveCamera(camera, CameraAnimation.from(500,true,true))
        kakaoMap?.labelManager?.clearAll()

        kakaoMap?.setOnCameraMoveEndListener { kakaoMap, cameraPosition, gestureType ->
            val styles = kakaoMap.labelManager?.addLabelStyles(
                LabelStyles.from(LabelStyle.from(R.drawable.ic_map_red)
                    .setIconTransition(LabelTransition.from(Transition.Scale, Transition.Scale))
                )
            )

            // styles가 null이 아닐때만, LabelOptions 생성하고 라벨추가
            if (styles != null) {
                val options = LabelOptions.from(LatLng.from(latitude_formatter, longitude_formatter)).setStyles(styles)
                val layer = kakaoMap.labelManager?.getLayer()
                layer?.addLabel(options)
            } else {
                Log.e("kakaoMap", "LabelStyles null값 에러")
            }
        }
    }


    private fun observeViewModel(){
        viewLifecycleOwner.lifecycleScope.launch{

            mapViewModel.regionSearch.flowWithLifecycle(viewLifecycleOwner.lifecycle, STARTED).collectLatest{

                // 검색결과에 documents가 포함되어있으면, 그 documents값중 첫번째값을 가져옴
                it?.documents?.firstOrNull()?.let { document ->
                    searchDocumentsResponse = document
                    setMapData(document)
                }
            }
        }
    }

    private fun chipGroup(){
        binding.chipGroup.setOnCheckedStateChangeListener { chipGroup, checkedId ->
            val selectChip = chipGroup.checkedChipId

            when(selectChip){
                R.id.fist_type -> chipType(ChipType.FIRST)
                R.id.second_type -> chipType(ChipType.SECOND)
                R.id.three_type -> chipType(ChipType.THIRD)
                R.id.four_type -> chipType(ChipType.FOURTH)
                R.id.five_type -> chipType(ChipType.FIFTH)
                R.id.six_type -> chipType(ChipType.SIXTH)
                else -> Log.d("chipGroup", "Unknown chip selected")
            }
        }
    }

    private fun chipType(type: ChipType){
        when(type){
            ChipType.FIRST -> {
                mapViewModel.getRegionSearch(query = "통영")
                binding.searchEt.setText("통영")
            }
            ChipType.SECOND -> {
                mapViewModel.getRegionSearch(query = "대전")
                binding.searchEt.setText("대전")
            }
            ChipType.THIRD -> {
                mapViewModel.getRegionSearch(query = "안성")
                binding.searchEt.setText("안성")
            }
            ChipType.FOURTH -> {
                mapViewModel.getRegionSearch(query = "파주")
                binding.searchEt.setText("파주")
            }
            ChipType.FIFTH -> {
                mapViewModel.getRegionSearch(query = "서산")
                binding.searchEt.setText("서산")
            }
            ChipType.SIXTH -> {
                mapViewModel.getRegionSearch(query = "세종")
                binding.searchEt.setText("세종")
            }
        }
    }


    // 특산품에 대한 데이터가 있는 지역리스트
    private val regions = listOf("통영","대전","군산","김포","양주","논산","안성","파주","세종","인천","원주",
        "서산","함양","성주","영암","부산","봉화","정읍","횡성","삼척","평택","창녕","거제","진주","울릉","포항",
        "충주","상주","김천","대구","경주","울산","김해","여수","나주","전주","보령","제천","영주","완도","남해",
        "계룡","연천","합천","산청","함안")


    // 모든 지역리스트
    private val Allregions = listOf("통영","대전","군산","김포","양주","논산","대전","안성","파주","세종","인천",
        "원주","서산","함양","성주","영암","부산","봉화","정읍","횡성","삼척","평택","창녕","거제","진주","울릉","포항",
        "충주","상주","김천","대구","영천","경주","울산","김해","여수","나주","전주","보령","제천","영주","완도","포천",
        "수원","서울","과천","광주","포천","화성","오산","이천","춘천","속초","강릉","동해","당진","천안","태백","문경",
        "안동","구미","밀양","양산","창원","광양","목포","남원","김제","익산","청주","진도","남해","계룡","연천","합천"
        ,"산청","함안")



    // 위도,경도를 통해 지역명 가져오는 함수
    private fun getRegionName(latitude: Double, longitude: Double) : String {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        // 주소 정보에서 "시" 단위 지역명 추출
        val locality = addresses?.firstOrNull()?.locality ?: "${addresses}"
        // 지역명이 regions 리스트에 포함되어 있는지 확인하고 반환
        return regions.firstOrNull { locality.contains(it) } ?: ""
    }


    // 위도,경도를 통해 지역명 가져오는 함수(전체)
    private fun AllgetRegionName(latitude: Double, longitude: Double) : String {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        val locality = addresses?.firstOrNull()?.locality ?: "${addresses}"
        // 지역명이 Allregions 리스트에 포함되어 있는지 확인하고 반환
        return Allregions.firstOrNull { locality.contains(it) } ?: ""
    }

    private fun deleteText() = with(binding){
        deleteIv.setOnClickListener {
            searchEt.setText("")
            Snackbar.make(mapView, "검색어가 삭제되었습니다", Snackbar.LENGTH_SHORT).show()
        }

        alertIv.setOnClickListener {
            alertDialog()
        }
    }


    // 특산품에 대한 데이터가 있는 지역리스트
//    fun containsRegoin(searchText: String): Boolean {
//        return regions.any { searchText.contains(it) }
//    }


    // 검색이 되는 모든 지역리스트
    fun AllcontainsRegoin(searchText: String): Boolean {
        return Allregions.any { searchText.contains(it) }
    }


    fun alertDialog(){
        val dialog = Dialog(requireContext())
        val dialogBinding = RegionAlramDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.dialogClose.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.dialogRegion.text = "$regions"

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        binding.searchEt.setText("")
        binding.chipGroup.clearCheck()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        kakaoMap = null
        mapView.finish()
    }

    fun moveToRecipeFragment() {
        (requireParentFragment().parentFragment as MainFragment).moveToRecipeFragment()
    }

    fun moveToRecipeGraphFragment() {
        (requireParentFragment().parentFragment as MainFragment).moveToRecipeGraphFragment()
    }
}