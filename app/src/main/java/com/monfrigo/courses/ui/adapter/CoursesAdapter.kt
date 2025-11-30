package com.monfrigo.courses.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.monfrigo.courses.data.model.CourseItem
import com.monfrigo.courses.databinding.ItemCourseBinding
import kotlin.math.max

/**
 * Adapter pour afficher la liste de courses
 */
class CoursesAdapter(
    private val onItemChecked: (CourseItem) -> Unit,
    private val onQuantityChanged: (CourseItem, Double) -> Unit,
    private val onItemDelete: (CourseItem) -> Unit
) : ListAdapter<CourseItem, CoursesAdapter.CourseViewHolder>(CourseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val binding = ItemCourseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CourseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(getItem(position))
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

                // Quantité restante à acheter
                tvQuantiteNecessaire.text = String.format(
                    "%.2f %s à acheter",
                    item.quantite_restante,
                    item.unite
                )

                // Unité
                tvUniteAchetee.text = item.unite

                // Catégorie (badge)
                if (!item.categorie.isNullOrEmpty()) {
                    tvCategorie.text = item.categorie
                    tvCategorie.visibility = android.view.View.VISIBLE
                } else {
                    tvCategorie.visibility = android.view.View.GONE
                }

                // Quantité achetée (par défaut = quantité restante)
                isUpdating = true
                etQuantiteAchetee.setText(String.format("%.2f", item.quantite_achetee))
                isUpdating = false

                // Bouton moins
                btnMoins.setOnClickListener {
                    val current = etQuantiteAchetee.text.toString().toDoubleOrNull() ?: 0.0
                    val nouvelle = max(0.0, current - 1.0)
                    etQuantiteAchetee.setText(String.format("%.2f", nouvelle))
                    onQuantityChanged(item, nouvelle)
                }

                // Bouton plus
                btnPlus.setOnClickListener {
                    val current = etQuantiteAchetee.text.toString().toDoubleOrNull() ?: 0.0
                    val nouvelle = current + 1.0
                    etQuantiteAchetee.setText(String.format("%.2f", nouvelle))
                    onQuantityChanged(item, nouvelle)
                }

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

                // Checkbox "Acheté"
                checkboxAchete.setOnCheckedChangeListener(null)
                checkboxAchete.isChecked = item.achete
                checkboxAchete.setOnCheckedChangeListener { _, isChecked ->
                    onItemChecked(item)
                    // Animation visuelle
                    root.animate()
                        .alpha(if (isChecked) 0.7f else 1.0f)
                        .setDuration(200)
                        .start()
                }

                // Bouton supprimer
                btnSupprimer.setOnClickListener {
                    onItemDelete(item)
                }

                // Style visuel selon l'état
                root.alpha = if (item.achete) 0.7f else 1.0f

                // Indicateur visuel si complété
                if (item.isCompleted()) {
                    tvQuantiteNecessaire.text = "✓ Complet"
                    tvQuantiteNecessaire.setTextColor(
                        android.graphics.Color.parseColor("#28a745")
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