package nbc.group.recipes.presentation.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
//import nbc.group.recipes.GlideApp
import nbc.group.recipes.R
import nbc.group.recipes.data.model.dto.Recipe
import nbc.group.recipes.data.model.entity.RecipeEntity
import nbc.group.recipes.data.network.NetworkResult
import nbc.group.recipes.databinding.CustomDialogBinding
import nbc.group.recipes.databinding.FragmentMypageBinding
import nbc.group.recipes.presentation.MainActivity
import nbc.group.recipes.presentation.adapter.MyPageRecipeAdapter
import nbc.group.recipes.presentation.adapter.decoration.GridSpacingItemDecoration
import nbc.group.recipes.viewmodel.MainViewModel

@AndroidEntryPoint
class MypageFragment : Fragment(){

    companion object {
        const val TAG = "MypageFragment"
    }

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    private var _adapter: MyPageRecipeAdapter? = null
    private val adapter get() = _adapter!!

    private val viewModel: MainViewModel by activityViewModels()

    private val recipeEntities = mutableListOf<RecipeEntity>()

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val inputStream = requireActivity().contentResolver.openInputStream(uri)
                viewModel.putImage(inputStream!!)
                binding.ivUserProfile.setImageURI(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _adapter = MyPageRecipeAdapter(this,
            onClick = {item, position ->
//                recipeEntity = item
//
//                val recipeEntity = RecipeEntity(
//                    id = recipeEntity.id,
//                    recipeImg = recipeEntity.recipeImg,
//                    recipeName = recipeEntity.recipeName,
//                    explain = recipeEntity.explain,
//                    step = recipeEntity.step,
//                    ingredient = recipeEntity.ingredient,
//                    difficulty = recipeEntity.difficulty,
//                    time = recipeEntity.time
//                )
//                navigateToRecipeDetail(recipeEntity)
                navigateToRecipeDetail(item)
            }
        )

        with(binding) {
            btSignIn.setOnClickListener(signInButtonClickListener)
            tvResignButton.setOnClickListener(resignButtonClickListener)
            tvLogOutButton.setOnClickListener(logOutButtonClickListener)
            ivUserProfile.setOnClickListener(userProfileImageClickListener)
            rvUserRecipe.layoutManager = GridLayoutManager(activity, 2)
            rvUserRecipe.adapter = adapter
            rvUserRecipe.addItemDecoration(
                GridSpacingItemDecoration(
                    2,
                    (20 * resources.displayMetrics.density + 0.5f).toInt(),
                    false
                )
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.signInFlow.collect { nullable ->
                if (nullable == null) {
                    binding.clUser.visibility = View.GONE
                    binding.clNonUser.visibility = View.VISIBLE
                }
                nullable?.let { nonNull ->
                    when (nonNull) {
                        is NetworkResult.Success -> {
                            binding.clNonUser.visibility = View.GONE
                            binding.clUser.visibility = View.VISIBLE
                            initUser()
                        }

                        is NetworkResult.Failure -> {
                            binding.clUser.visibility = View.GONE
                            binding.clNonUser.visibility = View.VISIBLE
                        }

                        is NetworkResult.Loading -> {

                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userMetaData.collectLatest { nullable ->
                nullable?.let { nonNull ->
                    when (nonNull) {
                        is NetworkResult.Success -> {
                            Log.e(TAG, "onViewCreated: get recipeIds: Success")
                            val recipeIds = nonNull.result.recipeIds
                            recipeIds.forEach { recipeId ->
                                val temp = viewModel.getRecipeFromFirebaseById(recipeId)
                                if(temp is NetworkResult.Success) {
                                    recipeEntities.add(temp.result)
                                    adapter.submitList(recipeEntities)
                                    adapter.notifyItemInserted(adapter.itemCount - 1)
                                }
                            }
                            // 수정필요
//                            adapter.submitList(nonNull.result.recipeIds)
                        }

                        is NetworkResult.Failure -> {
                            Log.e(
                                TAG,
                                "onViewCreated: get recipeIds: Failure: ${nonNull.exception}",
                            )
                        }

                        is NetworkResult.Loading -> {
                            Log.e(TAG, "onViewCreated: get recipeIds: Loading")
                        }
                    }
                }
            }
        }
    }

    private fun initUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser?.let { currentUser ->
                binding.tvUserName.text = currentUser.displayName
                Glide.with(this@MypageFragment)
                    .load(
                        Firebase.storage.reference
                            .child("userProfile/${currentUser.uid}/profile.jpg")
                    )
                    .error(R.drawable.ic_appbar_mypage)
                    .into(binding.ivUserProfile)
                viewModel.getUserMeta(currentUser.uid)
            }
        }
    }

    // 작성한 레시피 클릭
    private fun navigateToRecipeDetail(recipeEntity: RecipeEntity) {
        val bundle = Bundle().apply {
            putParcelable("recipeDetail", recipeEntity)
        }
        (activity as? MainActivity)?.moveToRecipeDetailFragment(bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _adapter = null

        recipeEntities.clear()
    }

    private val signInButtonClickListener: (View) -> Unit = {
        (activity as MainActivity).moveToSignInFragment()
    }

    private val resignButtonClickListener: (View) -> Unit = {
        ResignDialog()
    }

    private val logOutButtonClickListener: (View) -> Unit = {
        LogoutDialog()
    }

    private val userProfileImageClickListener: (View) -> Unit = {
        pickMedia.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun LogoutDialog() {
        val dialog = Dialog(requireContext())
        val dialogBinding = CustomDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.dialogTv.text = "로그아웃 하시겠습니까?"
        dialogBinding.dialogCancelBtn.text = "아니오"
        dialogBinding.dialogDeleteBtn.text = "네"

        dialogBinding.dialogCancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.dialogDeleteBtn.setOnClickListener {
            viewModel.logout()
            dialog.dismiss()
        }
        dialog.show()
    }


    private fun ResignDialog() {
        val dialog = Dialog(requireContext())
        val dialogBinding = CustomDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.dialogTv.text = "회원탈퇴를 하시겠습니까?"
        dialogBinding.dialogCancelBtn.text = "아니오"
        dialogBinding.dialogDeleteBtn.text = "네"

        dialogBinding.dialogCancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.dialogDeleteBtn.setOnClickListener {
            viewModel.resign()
            viewModel.logout()
            dialog.dismiss()
        }
        dialog.show()
    }
}