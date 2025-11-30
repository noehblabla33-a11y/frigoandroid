package com.monfrigo.courses.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import com.monfrigo.courses.ui.viewmodel.CoursesViewModel
import com.monfrigo.courses.utils.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: CoursesViewModel
    private lateinit var adapter: CoursesAdapter
    private lateinit var preferencesManager: PreferencesManager

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

        // Configurer l'API puis charger les courses
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
            onItemChecked = { item ->
                viewModel.toggleItemAchete(item)
            },
            onQuantityChanged = { item, quantity ->
                viewModel.updateQuantite(item, quantity)
            },
            onItemDelete = { item ->
                showDeleteConfirmation(item)
            }
        )

        binding.recyclerViewCourses.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCourses()
        }
    }

    private fun setupButtons() {
        binding.btnSynchroniser.setOnClickListener {
            showSyncConfirmation()
        }
    }

    private fun setupObservers() {
        // Observer les items
        viewModel.items.observe(this) { items ->
            adapter.submitList(items)
            updateEmptyView(items.isEmpty())
        }

        // Observer le chargement
        viewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.btnSynchroniser.isEnabled = !isLoading
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

        // Observer les succès
        viewModel.successMessage.observe(this) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearSuccessMessage()
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewCourses.visibility = android.view.View.GONE
            binding.emptyView.visibility = android.view.View.VISIBLE
        } else {
            binding.recyclerViewCourses.visibility = android.view.View.VISIBLE
            binding.emptyView.visibility = android.view.View.GONE
        }
    }

    private fun showSyncConfirmation() {
        val itemsAchetes = viewModel.items.value?.count { it.achete } ?: 0

        if (itemsAchetes == 0) {
            Snackbar.make(
                binding.root,
                "Cochez les articles achetés avant de synchroniser",
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

    private fun showDeleteConfirmation(item: com.monfrigo.courses.data.model.CourseItem) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer")
            .setMessage("Retirer ${item.ingredient_nom} de la liste ?")
            .setPositiveButton("Supprimer") { _, _ ->
                viewModel.deleteItem(item)
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
            viewModel.loadCourses()
        }
    }
}