package com.monfrigo.courses.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.monfrigo.courses.data.model.CourseItem
import com.monfrigo.courses.databinding.ItemCourseBinding

/**
 * Adapter compact pour afficher la liste de courses avec swipe-to-dismiss
 * REMPLACE COMPLÈTEMENT le fichier CoursesAdapter.kt existant
 */
class CoursesAdapter(
    private val onItemSwiped: (CourseItem) -> Unit,
    private val onQuantityChanged: (CourseItem, Double) -> Unit
) : ListAdapter<CourseItem, CoursesAdapter.CourseViewHolder>(CourseDiffCallback()) {

    private var showCompleted = false
    private var filteredList: List<CourseItem> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(getFilteredItem(position))
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    private fun getFilteredItem(position: Int): CourseItem {
        return filteredList[position]
    }

    /**
     * Soumet une nouvelle liste et applique le filtre
     */
    override fun submitList(list: List<CourseItem>?) {
        super.submitList(list)
        updateFilteredList(list ?: emptyList())
    }

    /**
     * Configure le toggle pour afficher/masquer les articles achetés
     */
    fun setShowCompleted(show: Boolean) {
        showCompleted = show
        updateFilteredList(currentList)
    }

    /**
     * Met à jour la liste filtrée selon le toggle
     */
    private fun updateFilteredList(list: List<CourseItem>) {
        filteredList = if (showCompleted) {
            list
        } else {
            list.filter { !it.achete }
        }
        notifyDataSetChanged()
    }

    inner class CourseViewHolder(
        private val binding: ItemCourseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: CourseItem? = null
        private var isUpdating = false

        fun bind(item: CourseItem) {
            currentItem = item

            binding.apply {
                // Nom de l'ingrédient
                tvIngredientNom.text = item.ingredient_nom

                // Unité
                tvUnite.text = item.unite

                // Catégorie (badge)
                if (!item.categorie.isNullOrEmpty()) {
                    tvCategorie.text = item.categorie
                    tvCategorie.visibility = android.view.View.VISIBLE
                } else {
                    tvCategorie.visibility = android.view.View.GONE
                }

                // Quantité achetée
                isUpdating = true
                etQuantiteAchetee.setText(String.format("%.1f", item.quantite_achetee))
                isUpdating = false

                // TextWatcher pour mise à jour en temps réel
                etQuantiteAchetee.removeTextChangedListener(null)
                etQuantiteAchetee.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable?) {
                        if (isUpdating) return

                        val quantite = s.toString().toDoubleOrNull()
                        if (quantite != null && quantite >= 0) {
                            onQuantityChanged(item, quantite)
                        }
                    }
                })

                // Style visuel selon l'état (acheté ou non)
                root.alpha = if (item.achete) 0.6f else 1.0f

                // Couleur de fond différente si acheté
                if (item.achete) {
                    root.setCardBackgroundColor(
                        android.graphics.Color.parseColor("#e8f5e9")
                    )
                } else {
                    root.setCardBackgroundColor(
                        android.graphics.Color.WHITE
                    )
                }
            }
        }
    }

    /**
     * DiffUtil callback pour optimiser les mises à jour
     */
    class CourseDiffCallback : DiffUtil.ItemCallback<CourseItem>() {
        override fun areItemsTheSame(oldItem: CourseItem, newItem: CourseItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CourseItem, newItem: CourseItem): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * ItemTouchHelper.Callback pour gérer le swipe-to-dismiss
 */
class SwipeToMarkCallback(
    private val onItemSwiped: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        onItemSwiped(position)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.7f // 70% de swipe nécessaire
    }
}