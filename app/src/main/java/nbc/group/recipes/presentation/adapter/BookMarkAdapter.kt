package nbc.group.recipes.presentation.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import nbc.group.recipes.R
import nbc.group.recipes.data.model.dto.Recipe
import nbc.group.recipes.data.model.entity.RecipeEntity
import nbc.group.recipes.databinding.BookmarkItemBinding
import nbc.group.recipes.presentation.fragment.FROM_FIREBASE

class BookMarkAdapter(
    private val onClick : (RecipeEntity, Int) -> Unit,
    private val onLongClick : (RecipeEntity, Int) -> Unit
): ListAdapter<RecipeEntity, BookMarkAdapter.BookMarkViewHolder>(BookMarkDiffUtil){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BookMarkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bookmark_item, parent, false)
        return BookMarkViewHolder(BookmarkItemBinding.bind(view))
    }

    override fun onBindViewHolder(holder: BookMarkViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

        holder.itemView.setOnClickListener {
            onClick(item, position)
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(item, position)
            true
        }

    }

    class BookMarkViewHolder(
        private var binding : BookmarkItemBinding
    ) : RecyclerView.ViewHolder(binding.root){

        fun bind(recipeEntity : RecipeEntity){

            Glide.with(itemView.context)
                .load(recipeEntity.recipeImg)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.recipeImage)

            binding.recipeName.text = recipeEntity.recipeName
            binding.recipeDescription.text = recipeEntity.explain
            binding.recipeLevel.text = recipeEntity.difficulty
            binding.recipeTime.text = recipeEntity.time

        }
    }


    object BookMarkDiffUtil : DiffUtil.ItemCallback<RecipeEntity>(){
        override fun areItemsTheSame(oldItem: RecipeEntity, newItem: RecipeEntity): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: RecipeEntity, newItem: RecipeEntity): Boolean {
            return oldItem == newItem
        }

    }

}