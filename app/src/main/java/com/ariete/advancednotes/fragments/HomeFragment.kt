package com.ariete.advancednotes.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.ariete.advancednotes.R
import com.ariete.advancednotes.databinding.FragmentHomeBinding
import com.ariete.advancednotes.activities.MainActivity
import com.ariete.advancednotes.model.Note
import com.ariete.advancednotes.adapters.NoteAdapter
import com.ariete.advancednotes.viewmodel.NoteViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment :
    Fragment(R.layout.fragment_home),
    View.OnClickListener,
    SearchView.OnQueryTextListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var mView: View

    private lateinit var noteAdapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(
            inflater,
            container,
            false
        )

        animation()
        attach_an_action_to_back_button()
        setting_up_searchItem()

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        noteViewModel = (activity as MainActivity).noteViewModel
        mView = view

        assign_bottomNavigation_item()

        setupRecyclerView()
        setting_up_click_addNote()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
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

    private fun `attach_an_action_to_back_button`() {
        val callback = object : OnBackPressedCallback(true) {
            /**
                * The OnBackPressedCallback class is used to handle the back button
                * press event.
                * -----------------------------------------------------------------
                * Класс OnBackPressedCallback используется для обработки
                * нажатия на кнопку back.
            */
            override fun handleOnBackPressed() {
                /**
                    * The function handleOnBackPressed() is used
                    * to override the default behavior
                    * when the user presses the back button.
                    * --------------------------------------------------------
                    * Функция handleOnBackPressed() используется для переопределения
                    * изначального поведения,
                    * когда пользовватель нажимает на кнопку
                    * "back".
                */
                requireActivity().finish()
                /**
                    * The requireActivity() function is used to retrieve the activity
                    * associated with a fragment.
                    * ---------------------------------------------------------------
                    * Функция requireActivity() используется для получения активности,
                    * связанной с фрагментом.
                */
            }
        }
        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, callback)
        callback.isEnabled = true
        /**
            * We set this property to true for
            * indicating that the callback should be enabled and active.
            * ----------------------------------------------------------
            * Мы устанавливаем для этого свойства значение true для
            * указания на то, что обратный вызов должен быть включен и активен.
        */
    }

    private fun `setting_up_searchItem`() {
        val searchItem = binding.inputSearch

        searchItem.isSubmitButtonEnabled = true
        /**
            * We set this property to true for
            * display submit button of query.
            * -------------------------------
            * Мы устанавливаем в это свойство значение true для
            * отображения кнопки отправки запроса.
        */
        searchItem.setOnQueryTextListener(this)
        /**
            * This fuction is used to set a listener
            * that will be notified when the user submits a search query or
            * when the query text changes.
            * -------------------------------------------------------------
            * Эта функция используется для установки прослушивателя, который
            * будет уведомляться, когда пользователь отправляет поисковый запрос
            * или когда текст запроса изменяется.
        */
    }

    private fun `assign_bottomNavigation_item`() {
        bottomNavigation = (activity as MainActivity).bottomNavigation
        bottomNavigation
            .menu
            .findItem(R.id.notes)
            .isChecked = true
    }

    private fun `setting_up_click_addNote`() {
        binding.addNote.setOnClickListener(this)
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(requireActivity().baseContext)

        binding.noteRecyclerView.apply {

            layoutManager = StaggeredGridLayoutManager(
                2,
                StaggeredGridLayoutManager.VERTICAL
            )

            setHasFixedSize(true)

            adapter = noteAdapter
        }

        activity?.let {
            noteViewModel
                .getAllNotes()
                .observe(viewLifecycleOwner) { notes ->
                    noteAdapter
                        .differ
                        .submitList(notes)
                    /**
                     * The submitList() passed a new List to the AdapterHelper.
                     * Adapter updates will be computed in the background.
                     *
                     * If a List is already present,
                     * a diff also will be computed asynchronously.
                     *
                     * When the difference is calculated, the list changes.
                     * --------------------------------------------------------
                     * Функция submitList() передает новый список AdapterHelper - у.
                     * Обновления адаптера "будут вычисляться" в фоновом режиме.
                     *
                     * Если список уже существует,
                     * разница также будет вычисляться асинхронно.
                     *
                     * Когда разница вычислена, список изменяется.
                     */
                    updateUI(notes)
                }
        }
    }

    private fun updateUI(notes: List<Note>) {
        if (notes.isNotEmpty()) {
            binding.noteRecyclerView.visibility = View.VISIBLE
            binding.textNoNotes.visibility = View.GONE
        } else {
            binding.noteRecyclerView.visibility = View.GONE
            binding.textNoNotes.visibility = View.VISIBLE
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        /**
         * This function сalled when the user submits the query.
         * -----------------------------------------------------
         * Эта функция вызывается, когда пользователь отправляет запрос.
         */

        query?.let {
            searchNotes(query)
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        /**
         * This function called when the query text is changed by the user.
         * ----------------------------------------------------------------
         * Эта функция вызывается, когда пользователь изменяет текст запроса.
         */

        newText?.let {
            searchNotes(newText)
        }
        return true
    }

    private fun searchNotes(query: String?) {
        val searchQuery = "%$query%"

        noteViewModel
            .searchNotes(searchQuery)
            .observe(viewLifecycleOwner) { list ->
                noteAdapter
                    .differ
                    .submitList(list)
            }

        /**
            * The expression "%$query%" is being passed instead
            * of the original query parameter to the searchNotes() function
            * in order to perform a wildcard search in the database.
            *
            * The "%" character is a wildcard character that represents any sequence of
            * characters.
            * By enclosing the original query parameter within "%" characters,
            * we are essentially telling the database to search for any notes that contain
            * the query string as a substring, rather than an exact match.
            * ----------------------------------------------------------------------------
            * Выражение "%$query%" передается вместо query в функцию searchNotes()
            * в целях выполнения поиска по шаблону в базе данных.
            *
            * Символ "%" - это символ шаблона, который представляет любую
            * последовательность символов.
            * Заключая query в символы "%", мы "говорим" базе данных
            * искать любые заметки, которые содержат query как подстроку,
            * а не точное совпадение.
        */
    }

    override fun onClick(v: View?) {
        if (v == binding.addNote) {
            val bundle = Bundle()

            bundle.putString("message", "note creation")

            mView.findNavController().navigate(
                R.id.action_homeFragment_to_noteFragment,
                bundle
            )

            noteViewModel.status = 0 // note creation
        }
    }
}