package com.monfrigo.courses.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de données Room pour l'application
 * Stocke les courses localement et persiste entre les sessions
 */
@Database(
    entities = [CourseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CoursesDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao

    companion object {
        // Singleton pour éviter d'avoir plusieurs instances de la base de données
        @Volatile
        private var INSTANCE: CoursesDatabase? = null

        /**
         * Récupère l'instance de la base de données (singleton)
         */
        fun getDatabase(context: Context): CoursesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CoursesDatabase::class.java,
                    "courses_database"
                )
                    .fallbackToDestructiveMigration() // En cas de changement de schéma, supprime et recrée
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}