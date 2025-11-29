package com.monfrigo.courses.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.monfrigo.courses.data.model.CourseItem
import com.monfrigo.courses.databinding.ItemCourseBinding

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

        fun bind(item: CourseItem) {
            binding.apply {
                // Nom de l'ingrédient
                tvIngredientNom.text = item.ingredient_nom

                // Quantité nécessaire
                tvQuantiteNecessaire.text = String.format(
                    "%.2f %s nécessaire",
                    item.quantite,
                    item.unite
                )

                // Prix estimé
                if (item.prix_estime > 0) {
                    tvPrixEstime.text = String.format("≈ %.2f €", item.prix_estime)
                } else {
                    tvPrixEstime.text = "Prix non défini"
                }

                // Catégorie (badge)
                if (!item.categorie.isNullOrEmpty()) {
                    tvCategorie.text = item.categorie
                    tvCategorie.visibility = android.view.View.VISIBLE
                } else {
                    tvCategorie.visibility = android.view.View.GONE
                }

                // Checkbox acheté
                checkboxAchete.isChecked = item.achete
                checkboxAchete.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != item.achete) {
                        onItemChecked(item)
                    }
                }

                // Quantité achetée
                etQuantiteAchetee.setText(String.format("%.2f", item.quantite_achetee))
                etQuantiteAchetee.isEnabled = item.achete

                // Bouton pour valider la quantité
                btnValiderQuantite.setOnClickListener {
                    val quantiteStr = etQuantiteAchetee.text.toString()
                    val quantite = quantiteStr.toDoubleOrNull()
                    if (quantite != null && quantite > 0) {
                        onQuantityChanged(item, quantite)
                    }
                }
                btnValiderQuantite.isEnabled = item.achete

                // Bouton supprimer
                btnSupprimer.setOnClickListener {
                    onItemDelete(item)
                }

                // Style différent si acheté
                root.alpha = if (item.achete) 0.7f else 1.0f
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