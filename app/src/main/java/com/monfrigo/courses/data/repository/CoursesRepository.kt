package com.monfrigo.courses.data.repository

import android.util.Log
import com.monfrigo.courses.data.api.RetrofitClient
import com.monfrigo.courses.data.local.CourseDao
import com.monfrigo.courses.data.local.CourseEntity
import com.monfrigo.courses.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository pour gérer les données des courses
 * Gère à la fois l'API distante et le cache local
 *
 * Stratégie :
 * - Charger depuis le cache local d'abord
 * - Synchroniser avec le serveur en arrière-plan
 * - Garder les données en local jusqu'à synchronisation des achats
 */
class CoursesRepository(private val courseDao: CourseDao) {

    private val TAG = "CoursesRepository"
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
     * Récupère la liste de courses depuis le cache local
     * Retourne un Flow pour observer les changements en temps réel
     */
    fun getCoursesFromCache(): Flow<List<CourseItem>> {
        return courseDao.getAllFlow().map { entities ->
            entities.map { it.toCourseItem() }
        }
    }

    /**
     * Récupère la liste de courses depuis le cache local (version simple)
     */
    suspend fun getCoursesFromCacheSync(): Result<CoursesResponse> = withContext(Dispatchers.IO) {
        try {
            val entities = courseDao.getAll()
            val items = entities.map { it.toCourseItem() }
            val total = items.sumOf { it.prix_estime }

            Result.success(
                CoursesResponse(
                    success = true,
                    items = items,
                    count = items.size,
                    total_estime = total
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lecture cache: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Vérifie si le cache local contient des données
     */
    suspend fun hasCachedData(): Boolean = withContext(Dispatchers.IO) {
        try {
            courseDao.hasData()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Récupère la liste de courses depuis l'API et met à jour le cache local
     */
    suspend fun fetchCoursesFromServer(): Result<CoursesResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCourses()
            if (response.isSuccessful && response.body() != null) {
                val coursesResponse = response.body()!!

                // Sauvegarder dans le cache local
                saveCourses(coursesResponse.items)

                Log.d(TAG, "Liste récupérée du serveur et sauvegardée : ${coursesResponse.count} items")
                Result.success(coursesResponse)
            } else {
                Result.failure(Exception("Erreur lors de la récupération: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur réseau: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Stratégie hybride : charge depuis le cache, puis synchronise avec le serveur
     * Retourne les données du cache immédiatement, puis met à jour en arrière-plan
     */
    suspend fun getCourses(): Result<CoursesResponse> = withContext(Dispatchers.IO) {
        try {
            // 1. Essayer de charger depuis le cache d'abord
            val hasCachedData = hasCachedData()

            if (hasCachedData) {
                Log.d(TAG, "Chargement depuis le cache local")
                val cachedResult = getCoursesFromCacheSync()

                // 2. Synchroniser avec le serveur en arrière-plan (ne pas bloquer)
                try {
                    val serverResponse = apiService.getCourses()
                    if (serverResponse.isSuccessful && serverResponse.body() != null) {
                        saveCourses(serverResponse.body()!!.items)
                        Log.d(TAG, "Cache mis à jour avec les données du serveur")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Impossible de synchroniser avec le serveur : ${e.message}")
                    // Pas grave, on continue avec le cache
                }

                return@withContext cachedResult
            } else {
                // 3. Pas de cache : charger depuis le serveur obligatoirement
                Log.d(TAG, "Pas de cache, chargement depuis le serveur")
                return@withContext fetchCoursesFromServer()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur dans getCourses: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sauvegarde les courses dans le cache local
     */
    private suspend fun saveCourses(items: List<CourseItem>) {
        try {
            val entities = items.map { CourseEntity.fromCourseItem(it) }

            // Remplacer toutes les données par les nouvelles
            courseDao.deleteAll()
            courseDao.insertAll(entities)

            Log.d(TAG, "${items.size} items sauvegardés dans le cache local")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur sauvegarde cache: ${e.message}", e)
        }
    }

    /**
     * Met à jour un item dans le cache local (quand l'utilisateur coche un article)
     */
    suspend fun updateCourseLocally(item: CourseItem) = withContext(Dispatchers.IO) {
        try {
            val entity = CourseEntity.fromCourseItem(item)
            courseDao.update(entity)
            Log.d(TAG, "Item ${item.ingredient_nom} mis à jour localement")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur mise à jour locale: ${e.message}", e)
        }
    }

    /**
     * Synchronise les achats avec le serveur
     * Supprime les items synchronisés du cache local
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
                val syncResponse = response.body()!!

                // Supprimer les items achetés du cache local après synchronisation réussie
                courseDao.deleteAchetes()
                Log.d(TAG, "Items achetés supprimés du cache local après synchronisation")

                Result.success(syncResponse)
            } else {
                Result.failure(Exception("Erreur de synchronisation: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur sync: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Supprime un item de la liste (locale et serveur)
     */
    suspend fun deleteItem(itemId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteItem(itemId)
            if (response.isSuccessful) {
                // Aussi supprimer du cache local
                // (sera fait automatiquement lors du prochain rechargement)
                Log.d(TAG, "Item $itemId supprimé")
                Result.success(true)
            } else {
                Result.failure(Exception("Erreur de suppression: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur suppression: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Vide complètement le cache local
     * Utilisé en cas de déconnexion ou de reset
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            courseDao.deleteAll()
            Log.d(TAG, "Cache local vidé")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur vidage cache: ${e.message}", e)
        }
    }
}