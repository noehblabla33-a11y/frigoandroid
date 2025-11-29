package com.monfrigo.courses

import android.app.Application
import com.monfrigo.courses.data.api.RetrofitClient
import com.monfrigo.courses.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Classe Application pour initialiser l'app
 */
class CoursesApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialiser le client Retrofit avec les préférences sauvegardées
        val preferencesManager = PreferencesManager(this)

        CoroutineScope(Dispatchers.IO).launch {
            val apiUrl = preferencesManager.getApiUrl().first()
            val apiKey = preferencesManager.getApiKey().first()

            RetrofitClient.configure(apiUrl, apiKey)
        }
    }
}