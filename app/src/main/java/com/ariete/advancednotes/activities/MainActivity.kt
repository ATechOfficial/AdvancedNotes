package com.ariete.advancednotes.activities

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.ariete.advancednotes.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ariete.advancednotes.database.NoteDatabase
import com.ariete.advancednotes.databinding.ActivityMainBinding
import com.ariete.advancednotes.repository.NoteRepository
import com.ariete.advancednotes.viewmodel.NoteViewModel
import com.ariete.advancednotes.viewmodel.NoteViewModelProviderFactory
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

        setUpViewModel()
    }

    override fun onStart() {
        super.onStart()

        navController = currentFragment.findNavController()
        setting_bottom_naviagtion()
    }

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

    private fun `setting_bottom_naviagtion`() {
        bottomNavigation.itemIconTintList = null

        bottomNavigation.setOnItemSelectedListener {

            val homeFragmentContext = findViewById<View>(R.id.fragment_home)?.context
            val noteFragmentContext = findViewById<View>(R.id.fragment_note)?.context
            val settingsFragmentContext = findViewById<View>(R.id.fragment_settings)?.context


            when(it.itemId) {
                R.id.notes -> switching_to_homeFragment(
                    settingsFragmentContext,
                    navController
                )

                R.id.settings -> switching_to_settingsFragment(
                    homeFragmentContext,
                    noteFragmentContext,
                    navController
                )
            }

            true
            /**
                * Return true to indicate the item (operation) is selected (applied).
                * -------------------------------------------------------------------
                * Возврщаем true, чтобы указать что элемент (операция) выбран (применена).
            */
        }
    }

    private fun `switching_to_homeFragment`(
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

    private fun `switching_to_settingsFragment`(
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

    private fun setUpViewModel() {
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