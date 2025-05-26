package nbc.group.recipes.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
//import nbc.group.recipes.GlideApp
import nbc.group.recipes.data.model.entity.RecipeEntity
import nbc.group.recipes.databinding.ItemMypageRecipeBinding
import nbc.group.recipes.presentation.adapter.diff_util.RecipeDiffUtil
import nbc.group.recipes.presentation.fragment.MypageFragment
import java.util.concurrent.ThreadLocalRandom.current

class MyPageRecipeAdapter(
    private val fragment: Fragment,
    private val onClick : (RecipeEntity, Int) -> Unit,
) : ListAdapter<RecipeEntity, MyPageRecipeAdapter.MyRecipeViewHolder>(RecipeDiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecipeViewHolder {
        val binding =
            ItemMypageRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyRecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyRecipeViewHolder, position: Int) {
        val current = getItem(position)
        Glide.with(fragment)
            .load(Firebase.storage.reference.child(current.recipeImg))
            .into(holder.binding.ivRecipe)

        holder.itemView.setOnClickListener {
            onClick(current, position)
        }

    }

    inner class MyRecipeViewHolder(
        val binding: ItemMypageRecipeBinding
    ) : ViewHolder(binding.root)
}