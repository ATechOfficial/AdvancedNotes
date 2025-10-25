package com.ariete.advancednotes.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ariete.advancednotes.R
import com.ariete.advancednotes.activities.MainActivity
import com.ariete.advancednotes.adapters.LanguageCard
import com.ariete.advancednotes.adapters.LanguageSwitchAdapter
import com.ariete.advancednotes.adapters.OnLanguageSelectedListener
import com.ariete.advancednotes.databinding.FragmentSettingsBinding
import com.ariete.advancednotes.databinding.LayoutAboutUsBinding
import com.ariete.advancednotes.databinding.LayoutLanguageSwitchBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.content.edit
import java.util.Locale

class SettingsFragment :
    Fragment(R.layout.fragment_settings),
    View.OnClickListener,
    OnLanguageSelectedListener {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var temporarilySelectedLanguage: LanguageCard? = null

    private lateinit var mView: View

    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var aboutUs: CardView
    private lateinit var languageSwitch: CardView

    private lateinit var dialogAboutUs: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        mView = binding.root

        setupNavigationAndViews()
        setupListeners()
        setupAboutUsView()

        animation()
        return mView
    }


    private fun setupNavigationAndViews() {
        bottomNavigation = (activity as MainActivity).bottomNavigation
        bottomNavigation.menu.findItem(R.id.settings).isChecked = true

        aboutUs = binding.aboutUs
        languageSwitch = binding.languageSwitch
    }

    private fun setupListeners() {
        aboutUs.setOnClickListener(this)
        languageSwitch.setOnClickListener(this)
    }

    private fun setupAboutUsView() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(
                R.layout.layout_about_us,
                binding.root,
                false
            )

        val view = LayoutAboutUsBinding.bind(dialogView)

        dialogAboutUs = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogAboutUs.window!!.setBackgroundDrawableResource(
            R.drawable.background_dialog_about_us
        )

        view.ok.setOnClickListener {
            dialogAboutUs.dismiss()
        }
    }

    private fun animation() {
        lifecycleScope.launch {
            binding.root.alpha = 0f

            delay(500)

            binding.root
                .animate()
                .alpha(1f)
                .duration = 250
        }
    }

    private fun createLanguageCards(): MutableList<LanguageCard> {
        val sharedPrefs = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val currentLangCode = sharedPrefs.getString("My_Lang", "ru") ?: "ru"

        val languageList = listOf(
            Triple(R.string.locale_english_name, R.string.locale_english, R.string.locale_code_english),
            Triple(R.string.locale_russian_name, R.string.locale_russian, R.string.locale_code_russian),
            Triple(R.string.locale_arabian_name, R.string.locale_arabian, R.string.locale_code_arabian),
            Triple(R.string.locale_spanish_name, R.string.locale_spanish, R.string.locale_code_spanish),
            Triple(R.string.locale_french_name, R.string.locale_french, R.string.locale_code_french),
            Triple(R.string.locale_chinese_name, R.string.locale_chinese, R.string.locale_code_chinese)
        )

        return languageList.map { (titleId, hintId, codeId) ->
            val title = getString(titleId)
            val hint = getString(hintId)
            val code = resources.getString(codeId)

            LanguageCard(
                title = title,
                hint = hint,
                code = code,
                isChecked = code == currentLangCode
            )
        }.toMutableList()
    }

    private fun showLanguageSwitchDialog() {
        val languageCards = createLanguageCards()
        val currentSelectedLanguage = languageCards.first { it.isChecked }
        temporarilySelectedLanguage = currentSelectedLanguage

        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.layout_language_switch, binding.root, false
        )

        val languageDialogBinding = LayoutLanguageSwitchBinding.bind(dialogView)
        val languageDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        languageDialog.window?.setBackgroundDrawableResource(
            R.drawable.background_dialog_language_switch
        )

        val recyclerView: RecyclerView = languageDialogBinding.rvLanguages
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = LanguageSwitchAdapter(languageCards, this)

        val btnConfirm: Button = languageDialogBinding.btnConfirm
        val btnCancel: Button = languageDialogBinding.btnCancel

        btnConfirm.setOnClickListener {
            temporarilySelectedLanguage?.let { selectedLang ->
                if (selectedLang.code != currentSelectedLanguage.code) {
                    applyLanguageChange(selectedLang)
                }
            }
            languageDialog.dismiss()
        }

        btnCancel.setOnClickListener { languageDialog.dismiss() }

        languageDialog.show()
    }

    override fun onLanguageSelected(selectedLanguage: LanguageCard) {
        temporarilySelectedLanguage = selectedLanguage
    }

    private fun applyLanguageChange(selectedLanguage: LanguageCard) {
        saveLocale(selectedLanguage.code)
        updateAppLocale(selectedLanguage.code)

        restartApp()
    }

    private fun updateAppLocale(languageCode: String) {
        val resources = requireContext().resources
        val configuration = resources.configuration
        val locale = Locale(languageCode)

        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private fun saveLocale(lang: String) {
        val prefs = requireActivity()
            .getSharedPreferences("Settings", Context.MODE_PRIVATE)
        prefs.edit {
            putString("My_Lang", lang)
        }
    }

    private fun restartApp() {
        val intent = requireActivity().intent
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.aboutUs -> {
                dialogAboutUs.show()
            }
            binding.languageSwitch -> {
                showLanguageSwitchDialog()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        super.onDestroyView()
        _binding = null
    }
}