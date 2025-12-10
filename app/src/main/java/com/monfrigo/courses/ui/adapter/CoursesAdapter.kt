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
 * Adapter pour afficher la liste de courses
 * Version corrigée qui garde la liste complète en mémoire
 */
class CoursesAdapter(
    private val onItemSwiped: (CourseItem) -> Unit,
    private val onQuantityChanged: (CourseItem, Double) -> Unit
) : ListAdapter<CourseItem, CoursesAdapter.CourseViewHolder>(CourseDiffCallback()) {

    private var showCompleted = false
    private var fullList: List<CourseItem> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        if (position >= 0 && position < currentList.size) {
            holder.bind(getItem(position))
        }
    }

    /**
     * Configure le toggle pour afficher/masquer les articles achetés
     */
    fun setShowCompleted(show: Boolean) {
        if (showCompleted != show) {
            showCompleted = show
            // Réappliquer le filtre avec la liste complète
            applyFilter()
        }
    }

    /**
     * Surcharge de submitList pour sauvegarder la liste complète
     */
    override fun submitList(list: List<CourseItem>?) {
        // Sauvegarder la liste complète
        fullList = list ?: emptyList()

        // Appliquer le filtre
        applyFilter()
    }

    /**
     * Applique le filtre selon l'état du toggle
     */
    private fun applyFilter() {
        val filteredList = if (showCompleted) {
            // Afficher tous les items
            fullList
        } else {
            // Filtrer pour ne garder que les items non achetés
            fullList.filter { !it.achete }
        }
        super.submitList(filteredList)
    }

    /**
     * Récupère un item de façon sécurisée
     */
    fun getItemAt(position: Int): CourseItem? {
        return if (position >= 0 && position < currentList.size) {
            getItem(position)
        } else {
            null
        }
    }

    inner class CourseViewHolder(
        private val binding: ItemCourseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentItem: CourseItem? = null
        private var isUpdating = false

        fun bind(item: CourseItem) {
            currentItem = item

            binding.apply {
                // Nom de l'ingrédient avec indicateur si acheté
                tvIngredientNom.text = if (item.achete) {
                    "✓ ${item.ingredient_nom}"
                } else {
                    item.ingredient_nom
                }

                // Unité
                tvUnite.text = item.unite

                // Catégorie (si présente dans le layout)
                tvCategorie?.apply {
                    if (!item.categorie.isNullOrEmpty()) {
                        text = item.categorie
                        visibility = android.view.View.VISIBLE
                    } else {
                        visibility = android.view.View.GONE
                    }
                }

                // Quantité achetée
                isUpdating = true
                val quantiteText = if (item.quantite_achetee % 1.0 == 0.0) {
                    item.quantite_achetee.toInt().toString()
                } else {
                    String.format("%.1f", item.quantite_achetee)
                }
                etQuantiteAchetee.setText(quantiteText)
                isUpdating = false

                // TextWatcher pour mise à jour
                etQuantiteAchetee.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable?) {
                        if (isUpdating) return

                        val quantite = s.toString().toDoubleOrNull()
                        if (quantite != null && quantite >= 0 && currentItem != null) {
                            onQuantityChanged(currentItem!!, quantite)
                        }
                    }
                })

                // Style visuel selon l'état (acheté ou non)
                root.alpha = if (item.achete) 0.6f else 1.0f

                // Click sur la carte pour toggle
                root.setOnClickListener {
                    currentItem?.let { onItemSwiped(it) }
                }
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
        if (position != RecyclerView.NO_POSITION) {
            onItemSwiped(position)
        }
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.7f // 70% de swipe nécessaire
    }
}