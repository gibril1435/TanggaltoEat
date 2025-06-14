package com.example.appl.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.appl.R
import com.example.appl.data.Recipe
import java.io.File

class RecipeAdapter(
    private val recipes: List<Recipe>,
    private val onItemClick: (Recipe) -> Unit,
    private val onItemLongClick: ((Int) -> Unit)? = null,
    private val isDeleteMode: () -> Boolean = { false },
    private val isSelected: (Int) -> Boolean = { false },
    private val onSelectToggle: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recipe, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(recipes[position], onItemClick, onItemLongClick, isDeleteMode(), isSelected(position), onSelectToggle)
    }

    override fun getItemCount() = recipes.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.ivFood)
        private val recipeName: TextView = view.findViewById(R.id.tvRecipeName)
        private val authorName: TextView = view.findViewById(R.id.tvAuthor)
        private val selectionOverlay: View = view.findViewById(R.id.selectionOverlay)
        private val checkmark: ImageView = view.findViewById(R.id.checkmark)

        fun bind(
            recipe: Recipe,
            onClick: (Recipe) -> Unit,
            onLongClick: ((Int) -> Unit)? = null,
            deleteMode: Boolean,
            selected: Boolean,
            onSelectToggle: ((Int) -> Unit)?
        ) {
            recipeName.text = recipe.name
            authorName.text = "by ${recipe.author}"
            Glide.with(imageView.context)
                .load(File(recipe.imagePath))
                .centerCrop()
                .into(imageView)

            if (deleteMode) {
                selectionOverlay.visibility = if (selected) View.VISIBLE else View.GONE
                checkmark.visibility = if (selected) View.VISIBLE else View.GONE
                itemView.setOnClickListener { onSelectToggle?.invoke(adapterPosition) }
            } else {
                selectionOverlay.visibility = View.GONE
                checkmark.visibility = View.GONE
            itemView.setOnClickListener { onClick(recipe) }
            }
            itemView.setOnLongClickListener {
                onLongClick?.invoke(adapterPosition)
                true
            }
        }
    }
} 