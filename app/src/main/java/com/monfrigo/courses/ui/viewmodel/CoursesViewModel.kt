package com.monfrigo.courses.ui.viewmodel

import android.util.Log
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

    private val TAG = "CoursesViewModel"
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
                    Log.d(TAG, "Courses chargées: ${response.count} items")
                    _items.value = response.items
                    _totalEstime.value = response.total_estime
                    updateCounters()
                }
                .onFailure { exception ->
                    Log.e(TAG, "Erreur de chargement: ${exception.message}", exception)
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

            Log.d(TAG, "Synchronisation de ${itemsAchetes.size} articles")

            repository.syncCourses(itemsAchetes)
                .onSuccess { response ->
                    Log.d(TAG, "Sync réussie: ${response.message}")
                    _successMessage.value = response.message

                    // Recharger la liste après synchro pour avoir l'état à jour
                    loadCourses()
                }
                .onFailure { exception ->
                    Log.e(TAG, "Erreur de sync: ${exception.message}", exception)
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
            val newAcheteState = !item.achete

            // Si on coche, mettre quantite_achetee = quantite_restante par défaut
            val newItem = if (newAcheteState) {
                item.copy(
                    achete = true,
                    quantite_achetee = item.quantite_restante
                )
            } else {
                // Si on décoche, remettre à l'état initial
                item.copy(
                    achete = false,
                    quantite_achetee = item.quantite_restante
                )
            }

            currentList[index] = newItem

            Log.d(TAG, "Item ${item.ingredient_nom} acheté: $newAcheteState")
            _items.value = currentList
            updateCounters()
        }
    }

    /**
     * Met à jour la quantité achetée ET calcule la quantité restante
     */
    fun updateQuantite(item: CourseItem, nouvelleQuantite: Double) {
        val currentList = _items.value?.toMutableList() ?: return
        val index = currentList.indexOfFirst { it.id == item.id }

        if (index != -1) {
            // Limiter la quantité achetée à la quantité restante
            val quantiteLimitee = nouvelleQuantite.coerceAtMost(item.quantite_restante)

            currentList[index] = item.copy(quantite_achetee = quantiteLimitee)

            Log.d(TAG, "Quantité de ${item.ingredient_nom} mise à jour: $quantiteLimitee (restante: ${item.quantite_restante})")
            _items.value = currentList
            updateTotalEstime()
        }
    }

    /**
     * Supprime un item de la liste
     */
    fun deleteItem(item: CourseItem) {
        viewModelScope.launch {
            Log.d(TAG, "Suppression de l'item ${item.ingredient_nom}")

            repository.deleteItem(item.id)
                .onSuccess {
                    val currentList = _items.value?.toMutableList() ?: return@launch
                    currentList.removeIf { it.id == item.id }
                    _items.value = currentList
                    _successMessage.value = "${item.ingredient_nom} retiré de la liste"
                    updateCounters()
                    updateTotalEstime()
                }
                .onFailure { exception ->
                    Log.e(TAG, "Erreur de suppression: ${exception.message}", exception)
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
        Log.d(TAG, "Compteurs mis à jour: ${_itemsAchetes.value} / ${list.size}")
    }

    /**
     * Recalcule le total estimé basé sur les quantités restantes
     */
    private fun updateTotalEstime() {
        val list = _items.value ?: emptyList()
        val total = list.sumOf { it.quantite_restante * it.prix_unitaire }
        _totalEstime.value = total
        Log.d(TAG, "Total estimé: $total €")
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
            ?.sumOf { it.quantite_achetee * it.prix_unitaire }
            ?: 0.0
    }
}