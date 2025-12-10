package com.monfrigo.courses.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.monfrigo.courses.R
import com.monfrigo.courses.data.api.RetrofitClient
import com.monfrigo.courses.data.local.CoursesDatabase
import com.monfrigo.courses.data.repository.CoursesRepository
import com.monfrigo.courses.databinding.ActivitySettingsBinding
import com.monfrigo.courses.utils.PreferencesManager
import kotlinx.coroutines.launch

/**
 * Activity pour configurer les paramètres de l'application
 * Permet de configurer l'URL de l'API et la clé API
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferencesManager: PreferencesManager

    // 🆕 Initialiser le repository avec le DAO
    private val repository by lazy {
        val database = CoursesDatabase.getDatabase(applicationContext)
        CoursesRepository(database.courseDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        preferencesManager = PreferencesManager(this)
        loadSettings()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.settings)
        }
    }

    /**
     * Charge les paramètres sauvegardés
     */
    private fun loadSettings() {
        lifecycleScope.launch {
            preferencesManager.getApiUrl().collect { url ->
                binding.etApiUrl.setText(url)
            }
        }

        lifecycleScope.launch {
            preferencesManager.getApiKey().collect { key ->
                binding.etApiKey.setText(key)
            }
        }
    }

    private fun setupButtons() {
        // Bouton tester la connexion
        binding.btnTesterConnexion.setOnClickListener {
            testConnection()
        }

        // Bouton enregistrer
        binding.btnEnregistrer.setOnClickListener {
            saveSettings()
        }
    }

    /**
     * Teste la connexion à l'API avec les paramètres actuels
     */
    private fun testConnection() {
        val url = binding.etApiUrl.text.toString().trim()
        val key = binding.etApiKey.text.toString().trim()

        if (url.isEmpty() || key.isEmpty()) {
            Snackbar.make(
                binding.root,
                "Veuillez renseigner l'URL et la clé API",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        // Configurer temporairement le client pour le test
        RetrofitClient.configure(url, key)

        binding.btnTesterConnexion.isEnabled = false
        binding.btnTesterConnexion.text = "Test en cours..."

        lifecycleScope.launch {
            repository.checkHealth()
                .onSuccess {
                    Snackbar.make(
                        binding.root,
                        "✓ Connexion réussie !",
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.tvStatutConnexion.text = "✓ Connexion réussie"
                    binding.tvStatutConnexion.setTextColor(getColor(R.color.green))
                }
                .onFailure { exception ->
                    Snackbar.make(
                        binding.root,
                        "✗ Échec: ${exception.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.tvStatutConnexion.text = "✗ Connexion échouée"
                    binding.tvStatutConnexion.setTextColor(getColor(R.color.red))
                }

            binding.btnTesterConnexion.isEnabled = true
            binding.btnTesterConnexion.text = getString(R.string.tester_connexion)
        }
    }

    /**
     * Sauvegarde les paramètres
     */
    private fun saveSettings() {
        val url = binding.etApiUrl.text.toString().trim()
        val key = binding.etApiKey.text.toString().trim()

        if (url.isEmpty() || key.isEmpty()) {
            Snackbar.make(
                binding.root,
                "Veuillez renseigner tous les champs",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            preferencesManager.saveApiUrl(url)
            preferencesManager.saveApiKey(key)

            // Reconfigurer le client avec les nouveaux paramètres
            RetrofitClient.configure(url, key)

            Snackbar.make(
                binding.root,
                "✓ Paramètres enregistrés",
                Snackbar.LENGTH_SHORT
            ).show()

            // Fermer l'activité après sauvegarde
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}