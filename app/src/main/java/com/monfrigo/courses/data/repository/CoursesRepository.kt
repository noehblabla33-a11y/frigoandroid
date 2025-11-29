package com.monfrigo.courses.data.repository

import com.monfrigo.courses.data.api.RetrofitClient
import com.monfrigo.courses.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository pour gérer les données des courses
 */
class CoursesRepository {

    private val apiService = RetrofitClient.getApiService()

    /**
     * Vérifie la santé de l'API
     */
    suspend fun checkHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.checkHealth()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("API non disponible: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Récupère la liste de courses depuis l'API
     */
    suspend fun getCourses(): Result<CoursesResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCourses()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur lors de la récupération: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Synchronise les achats avec le serveur
     */
    suspend fun syncCourses(achats: List<CourseItem>): Result<SyncResponse> = withContext(Dispatchers.IO) {
        try {
            val syncRequest = SyncRequest(
                achats = achats.map { item ->
                    Achat(
                        id = item.id,
                        quantite_achetee = item.quantite_achetee,
                        achete = item.achete
                    )
                }
            )

            val response = apiService.syncCourses(syncRequest)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur de synchronisation: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Supprime un item de la liste
     */
    suspend fun deleteItem(itemId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteItem(itemId)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Erreur de suppression: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}