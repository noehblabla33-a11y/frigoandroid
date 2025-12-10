package com.monfrigo.courses.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.monfrigo.courses.data.local.CoursesDatabase
import com.monfrigo.courses.data.model.CourseItem
import com.monfrigo.courses.data.repository.CoursesRepository
import kotlinx.coroutines.launch

/**
 * ViewModel pour gérer l'état de la liste de courses
 * Version avec cache local persistant via Room
 *
 * Les données sont stockées localement et persistent entre les sessions
 * jusqu'à la prochaine synchronisation des achats
 */
class CoursesViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "CoursesViewModel"

    // Initialiser la base de données et le repository
    private val database = CoursesDatabase.getDatabase(application)
    private val repository = CoursesRepository(database.courseDao())

    // Liste des items
    private val _items = MutableLiveData<List<CourseItem>>()
    val items: LiveData<List<CourseItem>> = _items

    // État de chargement
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Messages d'erreur
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Message de succès
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Total estimé
    private val _totalEstime = MutableLiveData<Double>()
    val totalEstime: LiveData<Double> = _totalEstime

    // Compteurs
    private val _itemsAchetes = MutableLiveData<Int>()
    val itemsAchetes: LiveData<Int> = _itemsAchetes

    private val _itemsRestants = MutableLiveData<Int>()
    val itemsRestants: LiveData<Int> = _itemsRestants

    // Indicateur de données en cache
    private val _hasLocalData = MutableLiveData<Boolean>()
    val hasLocalData: LiveData<Boolean> = _hasLocalData

    init {
        // Au démarrage, charger depuis le cache local s'il existe
        checkLocalData()
    }

    /**
     * Vérifie si des données existent en cache local
     */
    private fun checkLocalData() {
        viewModelScope.launch {
            val hasData = repository.hasCachedData()
            _hasLocalData.value = hasData

            if (hasData) {
                Log.d(TAG, "Données trouvées en cache local, chargement...")
                loadFromCache()
            }
        }
    }

    /**
     * Charge les données depuis le cache local
     */
    private fun loadFromCache() {
        viewModelScope.launch {
            _isLoading.value = true

            repository.getCoursesFromCacheSync()
                .onSuccess { response ->
                    Log.d(TAG, "Courses chargées depuis cache: ${response.count} items")
                    _items.value = response.items
                    _totalEstime.value = response.total_estime
                    updateCounters()
                }
                .onFailure { exception ->
                    Log.e(TAG, "Erreur lecture cache: ${exception.message}", exception)
                    _error.value = "Erreur de chargement du cache local"
                }

            _isLoading.value = false
        }
    }

    /**
     * Charge la liste de courses (depuis cache ou API selon disponibilité)
     * Stratégie : cache d'abord, puis synchronisation en arrière-plan
     */
    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getCourses()
                .onSuccess { response ->
                    Log.d(TAG, "Courses chargées: ${response.count} items")
                    _items.value = response.items
                    _totalEstime.value = response.total_estime
                    _hasLocalData.value = true
                    updateCounters()
                }
                .onFailure { exception ->
                    Log.e(TAG, "Erreur de chargement: ${exception.message}", exception)
                    _error.value = exception.message ?: "Erreur de chargement"

                    // En cas d'erreur réseau, essayer de charger depuis le cache
                    if (repository.hasCachedData()) {
                        Log.d(TAG, "Chargement depuis cache après échec réseau")
                        loadFromCache()
                    }
                }

            _isLoading.value = false
        }
    }

    /**
     * Récupère manuellement la liste depuis l'API (pour le bouton)
     * Force le téléchargement depuis le serveur et écrase le cache local
     */
    fun fetchListFromServer() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.fetchCoursesFromServer()
                .onSuccess { response ->
                    Log.d(TAG, "Liste récupérée du serveur: ${response.count} items")
                    _items.value = response.items
                    _totalEstime.value = response.total_estime
                    _hasLocalData.value = true
                    updateCounters()
                    _successMessage.value = "✓ Liste récupérée : ${response.count} article(s)"
                }
                .onFailure { exception ->
                    Log.e(TAG, "Erreur de récupération: ${exception.message}", exception)
                    _error.value = "Impossible de récupérer la liste : ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    /**
     * Marque un item comme acheté ou non
     * Met à jour le cache local immédiatement
     */
    fun toggleItemAchete(item: CourseItem) {
        viewModelScope.launch {
            val updatedItems = _items.value?.map {
                if (it.id == item.id) {
                    it.copy(achete = !it.achete)
                } else {
                    it
                }
            }

            _items.value = updatedItems ?: emptyList()
            updateCounters()

            // Mettre à jour le cache local
            updatedItems?.find { it.id == item.id }?.let { updatedItem ->
                repository.updateCourseLocally(updatedItem)
            }
        }
    }

    /**
     * Met à jour la quantité achetée d'un item
     * Met à jour le cache local immédiatement
     */
    fun updateQuantite(item: CourseItem, quantite: Double) {
        viewModelScope.launch {
            val updatedItems = _items.value?.map {
                if (it.id == item.id) {
                    it.copy(quantite_achetee = quantite)
                } else {
                    it
                }
            }

            _items.value = updatedItems ?: emptyList()

            // Mettre à jour le cache local
            updatedItems?.find { it.id == item.id }?.let { updatedItem ->
                repository.updateCourseLocally(updatedItem)
            }
        }
    }

    /**
     * Synchronise les achats avec le serveur
     * Supprime les items synchronisés du cache local
     */
    fun syncAchats() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val itemsAchetes = _items.value?.filter { it.achete } ?: emptyList()

            if (itemsAchetes.isEmpty()) {
                _error.value = "Aucun article acheté à synchroniser"
                _isLoading.value = false
                return@launch
            }

            repository.syncCourses(itemsAchetes)
                .onSuccess { response ->
                    Log.d(TAG, "Synchronisation réussie: ${response.message}")
                    _successMessage.value = "✓ ${response.items_modifies} article(s) synchronisé(s)"

                    // Retirer les items synchronisés de la liste locale
                    val remainingItems = _items.value?.filterNot { it.achete } ?: emptyList()
                    _items.value = remainingItems
                    updateCounters()

                    // Le cache local a déjà été mis à jour par le repository
                }
                .onFailure { exception ->
                    Log.e(TAG, "Erreur de synchronisation: ${exception.message}", exception)
                    _error.value = "Erreur de synchronisation : ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    /**
     * Supprime un item de la liste
     */
    fun deleteItem(item: CourseItem) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.deleteItem(item.id)
                .onSuccess {
                    Log.d(TAG, "Item supprimé: ${item.ingredient_nom}")
                    val updatedItems = _items.value?.filterNot { it.id == item.id } ?: emptyList()
                    _items.value = updatedItems
                    updateCounters()
                    _successMessage.value = "✓ ${item.ingredient_nom} retiré de la liste"
                }
                .onFailure { exception ->
                    Log.e(TAG, "Erreur de suppression: ${exception.message}", exception)
                    _error.value = "Erreur de suppression : ${exception.message}"
                }

            _isLoading.value = false
        }
    }

    /**
     * Vide le cache local
     * Utile en cas de déconnexion ou de reset
     */
    fun clearLocalData() {
        viewModelScope.launch {
            repository.clearCache()
            _items.value = emptyList()
            _hasLocalData.value = false
            updateCounters()
            _successMessage.value = "Cache local vidé"
        }
    }

    /**
     * Réinitialise le message de succès
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Réinitialise le message d'erreur
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Met à jour les compteurs
     */
    private fun updateCounters() {
        val items = _items.value ?: emptyList()
        _itemsAchetes.value = items.count { it.achete }
        _itemsRestants.value = items.count { !it.achete }
    }
}