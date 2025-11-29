package com.monfrigo.courses.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monfrigo.courses.data.model.CourseItem
import com.monfrigo.courses.data.repository.CoursesRepository
import kotlinx.coroutines.launch

/**
 * ViewModel pour gérer l'état de la liste de courses
 */
class CoursesViewModel : ViewModel() {

    private val repository = CoursesRepository()

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

    /**
     * Charge la liste de courses depuis l'API
     */
    fun loadCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getCourses()
                .onSuccess { response ->
                    _items.value = response.items
                    _totalEstime.value = response.total_estime
                    updateCounters()
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Erreur de chargement"
                }

            _isLoading.value = false
        }
    }

    /**
     * Synchronise les achats avec le serveur
     */
    fun syncAchats() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val itemsAchetes = _items.value?.filter { it.achete } ?: emptyList()

            if (itemsAchetes.isEmpty()) {
                _error.value = "Aucun article à synchroniser"
                _isLoading.value = false
                return@launch
            }

            repository.syncCourses(itemsAchetes)
                .onSuccess { response ->
                    _successMessage.value = response.message
                    // Recharger la liste après synchro
                    loadCourses()
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Erreur de synchronisation"
                }

            _isLoading.value = false
        }
    }

    /**
     * Marque un item comme acheté/non acheté
     */
    fun toggleItemAchete(item: CourseItem) {
        val currentList = _items.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.id == item.id }

        if (index != -1) {
            currentList[index] = item.copy(achete = !item.achete)
            _items.value = currentList
            updateCounters()
        }
    }

    /**
     * Met à jour la quantité achetée
     */
    fun updateQuantite(item: CourseItem, nouvelleQuantite: Double) {
        val currentList = _items.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.id == item.id }

        if (index != -1) {
            currentList[index] = item.copy(quantite_achetee = nouvelleQuantite)
            _items.value = currentList
        }
    }

    /**
     * Supprime un item de la liste
     */
    fun deleteItem(item: CourseItem) {
        viewModelScope.launch {
            repository.deleteItem(item.id)
                .onSuccess {
                    val currentList = _items.value?.toMutableList() ?: return@launch
                    currentList.removeIf { it.id == item.id }
                    _items.value = currentList
                    _successMessage.value = "${item.ingredient_nom} retiré de la liste"
                    updateCounters()
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Erreur de suppression"
                }
        }
    }

    /**
     * Met à jour les compteurs
     */
    private fun updateCounters() {
        val list = _items.value ?: emptyList()
        _itemsAchetes.value = list.count { it.achete }
    }

    /**
     * Efface le message d'erreur
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Efface le message de succès
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Calcule le total des items achetés
     */
    fun getTotalAchete(): Double {
        return _items.value
            ?.filter { it.achete }
            ?.sumOf { it.prixTotal() }
            ?: 0.0
    }
}