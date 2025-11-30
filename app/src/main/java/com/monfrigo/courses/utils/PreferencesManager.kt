package com.monfrigo.courses.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Gestionnaire des préférences de l'application
 */
class PreferencesManager(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "settings"
        )

        private val API_URL_KEY = stringPreferencesKey("api_url")
        private val API_KEY_KEY = stringPreferencesKey("api_key")

        // Valeurs par défaut
        // 10.0.2.2 est l'adresse spéciale pour accéder à localhost depuis l'émulateur Android
        private const val DEFAULT_URL = "http://10.0.2.2:5000/api/v1/"
        private const val DEFAULT_KEY = "votre_cle_api_secrete_a_changer"
    }

    /**
     * Sauvegarde l'URL de l'API
     */
    suspend fun saveApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[API_URL_KEY] = normalizeUrl(url)
        }
    }

    /**
     * Récupère l'URL de l'API
     */
    fun getApiUrl(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[API_URL_KEY] ?: DEFAULT_URL
        }
    }

    /**
     * Sauvegarde la clé API
     */
    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY_KEY] = key
        }
    }

    /**
     * Récupère la clé API
     */
    fun getApiKey(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[API_KEY_KEY] ?: DEFAULT_KEY
        }
    }

    /**
     * Normalise l'URL (ajoute http:// et le slash final si nécessaire)
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()

        // Ajouter http:// si manquant
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }

        // Ajouter le slash final si manquant
        if (!normalized.endsWith("/")) {
            normalized = "$normalized/"
        }

        return normalized
    }
}