package com.monfrigo.courses.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.monfrigo.courses.R
import com.monfrigo.courses.databinding.ActivityMainBinding
import com.monfrigo.courses.data.api.RetrofitClient
import com.monfrigo.courses.ui.adapter.CoursesAdapter
import com.monfrigo.courses.ui.adapter.SwipeToMarkCallback
import com.monfrigo.courses.ui.viewmodel.CoursesViewModel
import com.monfrigo.courses.utils.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.ItemTouchHelper

/**
 * MainActivity avec support de récupération manuelle de la liste
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: CoursesViewModel
    private lateinit var adapter: CoursesAdapter
    private lateinit var preferencesManager: PreferencesManager
    private var showCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)

        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        setupSwipeRefresh()
        setupObservers()
        setupButtons()

        configureApiAndLoadCourses()
    }

    private fun configureApiAndLoadCourses() {
        lifecycleScope.launch {
            try {
                val apiUrl = preferencesManager.getApiUrl().first()
                val apiKey = preferencesManager.getApiKey().first()

                RetrofitClient.configure(apiUrl, apiKey)
                viewModel.loadCourses()
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Erreur de configuration: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).setAction("Paramètres") {
                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                }.show()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Liste de Courses"
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[CoursesViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = CoursesAdapter(
            onItemSwiped = { item ->
                viewModel.toggleItemAchete(item)
                Snackbar.make(
                    binding.root,
                    "✓ ${item.ingredient_nom} marqué comme acheté",
                    Snackbar.LENGTH_SHORT
                ).show()
            },
            onQuantityChanged = { item, quantity ->
                viewModel.updateQuantite(item, quantity)
            }
        )

        binding.recyclerViewCourses.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter

            // Ajouter le swipe-to-dismiss
            val swipeHandler = SwipeToMarkCallback(
                onItemSwiped = { position ->
                    val allItems = viewModel.items.value ?: emptyList()
                    val displayedItems = if (showCompleted) {
                        allItems
                    } else {
                        allItems.filter { !it.achete }
                    }

                    if (position in displayedItems.indices) {
                        val item = displayedItems[position]
                        viewModel.toggleItemAchete(item)
                        Snackbar.make(
                            binding.root,
                            "✓ ${item.ingredient_nom}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            val itemTouchHelper = ItemTouchHelper(swipeHandler)
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCourses()
        }
    }

    private fun setupObservers() {
        // Observer la liste d'items
        viewModel.items.observe(this) { items ->
            val displayedItems = if (showCompleted) {
                items
            } else {
                items.filter { !it.achete }
            }

            adapter.submitList(displayedItems)

            // Afficher/masquer la vue vide
            binding.emptyView.visibility = if (displayedItems.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // Observer le chargement
        viewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.btnFetchList.isEnabled = !isLoading
            binding.btnSync.isEnabled = !isLoading
        }

        // Observer les erreurs
        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("OK") {
                        viewModel.clearError()
                    }
                    .show()
            }
        }

        // Observer les messages de succès
        viewModel.successMessage.observe(this) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
            }
        }

        // Observer le total estimé
        viewModel.totalEstime.observe(this) { total ->
            binding.tvTotalEstime.text = String.format("%.2f €", total)
        }

        // Observer les compteurs
        viewModel.itemsAchetes.observe(this) { count ->
            binding.tvStatsAchetes.text = "$count achetés"
        }

        viewModel.itemsRestants.observe(this) { count ->
            binding.tvStatsRestants.text = "$count restants"
        }
    }

    private fun setupButtons() {
        // Bouton de récupération de la liste
        binding.btnFetchList.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Récupérer la liste")
                .setMessage("Récupérer la liste de courses depuis le serveur ?\n\nLes données locales seront remplacées.")
                .setPositiveButton("Récupérer") { _, _ ->
                    viewModel.fetchListFromServer()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }

        // Bouton de synchronisation
        binding.btnSync.setOnClickListener {
            syncCourses()
        }

        // Toggle pour afficher/masquer les items achetés
        binding.toggleShowCompleted.setOnCheckedChangeListener { _, isChecked ->
            showCompleted = isChecked
            // Rafraîchir l'affichage
            val items = viewModel.items.value ?: emptyList()
            val displayedItems = if (showCompleted) {
                items
            } else {
                items.filter { !it.achete }
            }
            adapter.submitList(displayedItems)
        }
    }

    private fun syncCourses() {
        val itemsAchetes = viewModel.items.value?.count { it.achete } ?: 0

        if (itemsAchetes == 0) {
            Snackbar.make(
                binding.root,
                "Aucun article acheté à synchroniser",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Synchroniser les achats")
            .setMessage("Synchroniser $itemsAchetes article(s) avec le serveur ?\n\nLe frigo sera mis à jour automatiquement.")
            .setPositiveButton("Synchroniser") { _, _ ->
                viewModel.syncAchats()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.loadCourses()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val apiUrl = preferencesManager.getApiUrl().first()
            val apiKey = preferencesManager.getApiKey().first()
            RetrofitClient.configure(apiUrl, apiKey)
        }
    }
}