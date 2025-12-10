package com.monfrigo.courses.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object pour les opérations sur la base de données locale
 */
@Dao
interface CourseDao {

    /**
     * Insère ou remplace tous les items de courses
     * Utilisé quand on récupère une nouvelle liste du serveur
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<CourseEntity>)

    /**
     * Met à jour un item spécifique
     * Utilisé quand l'utilisateur coche un article
     */
    @Update
    suspend fun update(course: CourseEntity)

    /**
     * Supprime tous les items de courses
     * Utilisé après une synchronisation réussie
     */
    @Query("DELETE FROM courses")
    suspend fun deleteAll()

    /**
     * Supprime les items marqués comme achetés
     * Utilisé après synchronisation des achats
     */
    @Query("DELETE FROM courses WHERE achete = 1")
    suspend fun deleteAchetes()

    /**
     * Récupère tous les items de courses
     * Retourne un Flow pour observer les changements en temps réel
     */
    @Query("SELECT * FROM courses ORDER BY ingredient_nom ASC")
    fun getAllFlow(): Flow<List<CourseEntity>>

    /**
     * Récupère tous les items de courses (version simple)
     */
    @Query("SELECT * FROM courses ORDER BY ingredient_nom ASC")
    suspend fun getAll(): List<CourseEntity>

    /**
     * Récupère seulement les items non achetés
     */
    @Query("SELECT * FROM courses WHERE achete = 0 ORDER BY ingredient_nom ASC")
    suspend fun getNonAchetes(): List<CourseEntity>

    /**
     * Récupère seulement les items achetés
     */
    @Query("SELECT * FROM courses WHERE achete = 1 ORDER BY ingredient_nom ASC")
    suspend fun getAchetes(): List<CourseEntity>

    /**
     * Compte le nombre total d'items
     */
    @Query("SELECT COUNT(*) FROM courses")
    suspend fun getCount(): Int

    /**
     * Vérifie si la base de données locale contient des données
     */
    @Query("SELECT EXISTS(SELECT 1 FROM courses LIMIT 1)")
    suspend fun hasData(): Boolean

    /**
     * Récupère le timestamp de la dernière mise à jour
     */
    @Query("SELECT MAX(timestamp) FROM courses")
    suspend fun getLastUpdateTimestamp(): Long?
}