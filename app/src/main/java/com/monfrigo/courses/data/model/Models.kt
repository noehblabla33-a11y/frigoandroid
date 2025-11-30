package com.monfrigo.courses.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Modèle pour un item de la liste de courses
 */
@Parcelize
data class CourseItem(
    val id: Int,
    val ingredient_id: Int,
    val ingredient_nom: String,
    val quantite: Double,  // Quantité initialement demandée
    val unite: String,
    val prix_unitaire: Double,
    val prix_estime: Double,
    val image: String?,
    val categorie: String?,
    var achete: Boolean = false,
    var quantite_achetee: Double = quantite,  // Par défaut = quantité demandée
    var quantite_restante: Double = quantite  // Nouvelle propriété
) : Parcelable {

    /**
     * Calcule le prix total de l'item acheté
     */
    fun prixTotal(): Double {
        return if (prix_unitaire > 0) {
            quantite_achetee * prix_unitaire
        } else {
            0.0
        }
    }

    /**
     * Met à jour la quantité restante après achat partiel
     */
    fun updateQuantiteRestante(quantiteAchetee: Double): CourseItem {
        val restante = (quantite_restante - quantiteAchetee).coerceAtLeast(0.0)
        return copy(
            quantite_achetee = quantiteAchetee,
            quantite_restante = restante
        )
    }

    /**
     * Vérifie si l'item est complètement acheté
     */
    fun isCompleted(): Boolean {
        return quantite_restante <= 0.01 // Petite marge pour les erreurs de précision
    }
}

/**
 * Réponse de l'API pour la liste de courses
 */
data class CoursesResponse(
    val success: Boolean,
    val items: List<CourseItem>,
    val count: Int,
    val total_estime: Double
)

/**
 * Payload pour synchroniser les achats
 */
data class SyncRequest(
    val achats: List<Achat>
)

data class Achat(
    val id: Int,
    val quantite_achetee: Double,
    val achete: Boolean
)

/**
 * Réponse de synchronisation
 */
data class SyncResponse(
    val success: Boolean,
    val message: String,
    val items_modifies: Int
)

/**
 * Réponse d'erreur API
 */
data class ErrorResponse(
    val success: Boolean = false,
    val error: String
)