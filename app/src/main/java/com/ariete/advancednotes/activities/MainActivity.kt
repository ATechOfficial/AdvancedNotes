package com.ariete.advancednotes.activities

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.ariete.advancednotes.R
import com.ariete.advancednotes.database.NoteDatabase
import com.ariete.advancednotes.databinding.ActivityMainBinding
import com.ariete.advancednotes.repository.NoteRepository
import com.ariete.advancednotes.viewmodel.NoteViewModel
import com.ariete.advancednotes.viewmodel.NoteViewModelProviderFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    lateinit var noteViewModel: NoteViewModel

    lateinit var bottomNavigation: BottomNavigationView

    private lateinit var currentFragment: FragmentContainerView

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedLocale()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentFragment = binding.fragment
        bottomNavigation = binding.bottomNavigation

        setupViewModel()
    }

    override fun onStart() {
        super.onStart()

        navController = currentFragment.findNavController()
        setupBottomNavigation()
    }

    @Suppress("DEPRECATION")
    private fun applySavedLocale() {
        val prefs = getSharedPreferences("Settings", MODE_PRIVATE)
        val lang = prefs.getString("My_Lang", "ru") ?: "ru"

        val locale = Locale(lang)
        Locale.setDefault(locale)

        val resources = resources
        val configuration = resources.configuration

        configuration.setLocale(locale)
        createConfigurationContext(configuration)

        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.itemIconTintList = null

        bottomNavigation.setOnItemSelectedListener {

            val homeFragmentContext = findViewById<View>(R.id.fragment_home)?.context
            val noteFragmentContext = findViewById<View>(R.id.fragment_note)?.context
            val settingsFragmentContext = findViewById<View>(R.id.fragment_settings)?.context

            when(it.itemId) {
                R.id.notes -> switchToHomeFragment(
                    settingsFragmentContext,
                    navController
                )

                R.id.settings -> switchToSettingsFragment(
                    homeFragmentContext,
                    noteFragmentContext,
                    navController
                )
            }

            true // Return true to indicate the item (operation) is selected (applied).
        }
    }

    private fun switchToHomeFragment(
        settingsFragmentContext: Context?,
        navController: NavController
    ) {
        settingsFragmentContext?.let { context ->
            if (currentFragment.context == context) {
                navController.navigate(
                    R.id.action_settingsFragment_to_homeFragment,
                )
            }
        }
    }

    private fun switchToSettingsFragment(
        homeFragmentContext: Context?,
        noteFragmentContext: Context?,
        navController: NavController
    ) {
        homeFragmentContext?.let { context ->
            if (currentFragment.context == context) {
                navController.navigate(
                    R.id.action_homeFragment_to_settingsFragment
                )
            }
        }

        noteFragmentContext?.let { context ->
            if (currentFragment.context == context) {
                navController.navigate(
                    R.id.action_noteFragment_to_settingsFragment
                )
            }
        }
    }

    private fun setupViewModel() {
        val noteRepository = NoteRepository(
            NoteDatabase.invoke(this)
        )

        val viewModelProviderFactory = NoteViewModelProviderFactory(
            application,
            noteRepository
        )

        noteViewModel = ViewModelProvider(
            this,
            viewModelProviderFactory
        )[NoteViewModel::class.java]
    }
}