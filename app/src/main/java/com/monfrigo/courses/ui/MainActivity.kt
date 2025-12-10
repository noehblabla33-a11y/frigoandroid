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
import androidx.recyclerview.widget.ItemTouchHelper
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

/**
 * MainActivity - Version qui correspond au layout Git actuel
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
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[CoursesViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = CoursesAdapter(
            onItemSwiped = { item ->
                viewModel.toggleItemAchete(item)
                val message = if (item.achete) {
                    "✓ ${item.ingredient_nom} retiré des achats"
                } else {
                    "✓ ${item.ingredient_nom} marqué comme acheté"
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            },
            onQuantityChanged = { item, quantity ->
                viewModel.updateQuantite(item, quantity)
            }
        )

        binding.recyclerViewCourses.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter

            // Ajouter le swipe-to-dismiss
            val swipeHandler = SwipeToMarkCallback { position ->
                val item = this@MainActivity.adapter.getItemAt(position)
                if (item != null) {
                    viewModel.toggleItemAchete(item)
                    Snackbar.make(
                        binding.root,
                        "✓ ${item.ingredient_nom}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
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
            adapter.submitList(items)
            updateEmptyView(items.isEmpty())
            updateStats(items)
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
                    .setAction("Réessayer") {
                        viewModel.loadCourses()
                    }
                    .show()
                viewModel.clearError()
            }
        }

        // Observer les messages de succès
        viewModel.successMessage.observe(this) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
            }
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

            try {
                adapter.setShowCompleted(isChecked)
            } catch (e: Exception) {
                binding.toggleShowCompleted.isChecked = !isChecked
                Snackbar.make(
                    binding.root,
                    "Erreur lors du changement d'affichage",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
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

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewCourses.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            binding.statsCard.visibility = View.GONE
        } else {
            binding.recyclerViewCourses.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            binding.statsCard.visibility = View.VISIBLE
        }
    }

    private fun updateStats(items: List<com.monfrigo.courses.data.model.CourseItem>) {
        val restants = items.count { !it.achete }
        val achetes = items.count { it.achete }
        val totalEstime = items.filter { !it.achete }.sumOf {
            it.quantite_achetee * it.prix_unitaire
        }

        // Mettre à jour les statistiques
        binding.tvStatsRestants.text = "$restants restants"
        binding.tvStatsAchetes.text = "$achetes achetés"
        binding.tvTotalEstime.text = String.format("%.2f €", totalEstime)
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