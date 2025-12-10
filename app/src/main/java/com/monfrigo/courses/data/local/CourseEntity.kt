package com.monfrigo.courses.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.monfrigo.courses.data.model.CourseItem

/**
 * Entité Room pour stocker les items de courses localement
 * Persiste les données entre les sessions de l'application
 */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey
    val id: Int,
    val ingredient_id: Int,
    val ingredient_nom: String,
    val quantite: Double,
    val unite: String,
    val prix_unitaire: Double,
    val prix_estime: Double,
    val image: String?,
    val categorie: String?,
    val achete: Boolean,
    val quantite_achetee: Double,
    val quantite_restante: Double,
    val timestamp: Long = System.currentTimeMillis() // Pour savoir quand les données ont été sauvegardées
) {
    /**
     * Convertit une entité Room en modèle de domaine
     */
    fun toCourseItem(): CourseItem {
        return CourseItem(
            id = id,
            ingredient_id = ingredient_id,
            ingredient_nom = ingredient_nom,
            quantite = quantite,
            unite = unite,
            prix_unitaire = prix_unitaire,
            prix_estime = prix_estime,
            image = image,
            categorie = categorie,
            achete = achete,
            quantite_achetee = quantite_achetee,
            quantite_restante = quantite_restante
        )
    }

    companion object {
        /**
         * Crée une entité Room depuis un modèle de domaine
         */
        fun fromCourseItem(item: CourseItem): CourseEntity {
            return CourseEntity(
                id = item.id,
                ingredient_id = item.ingredient_id,
                ingredient_nom = item.ingredient_nom,
                quantite = item.quantite,
                unite = item.unite,
                prix_unitaire = item.prix_unitaire,
                prix_estime = item.prix_estime,
                image = item.image,
                categorie = item.categorie,
                achete = item.achete,
                quantite_achetee = item.quantite_achetee,
                quantite_restante = item.quantite_restante
            )
        }
    }
}