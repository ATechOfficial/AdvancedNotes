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
        setupBackButtonAction()
        setupSearchView()

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        noteViewModel = (activity as MainActivity).noteViewModel
        mView = view

        assignBottomNavigationItem()

        setupRecyclerView()
        setupAddNoteClickListener()
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

    private fun setupBackButtonAction() {

        // The OnBackPressedCallback class handles the back button press event.
        val callback = object : OnBackPressedCallback(true) {

            // The function redefines back button standard press behaviour
            override fun handleOnBackPressed() { requireActivity().finish() }
        }

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, callback)

        // The callback should be enabled and active
        callback.isEnabled = true
    }

    // TODO: Delete redundant submit button or search after a query submission only
    private fun setupSearchView() {
        val searchItem = binding.inputSearch

        // use true to display the query submit button
        searchItem.isSubmitButtonEnabled = true

        // set a listener that will be notified when the user submits a search query or
        // when the query text changes
        searchItem.setOnQueryTextListener(this)
    }

    private fun assignBottomNavigationItem() {
        bottomNavigation = (activity as MainActivity).bottomNavigation
        bottomNavigation
            .menu
            .findItem(R.id.notes)
            .isChecked = true
    }

    private fun setupAddNoteClickListener() {
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
                     * The submitList() passes a new List to the AdapterHelper.
                     * Adapter's updates will be computed asynchronously
                     * */
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

    // TODO: Check connection with redundant? query submit button
    override fun onQueryTextSubmit(query: String?): Boolean {
        // user query submit handling
        query?.let {
            searchNotes(query)
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        // dynamic user query submit handling
        newText?.let {
            searchNotes(newText)
        }
        return true
    }

    private fun searchNotes(query: String?) {
        /* "%$query%" expression represents wildcard search for any appropriate sequence
         * in both notes' titles and contents of the database
         */
        val searchQuery = "%$query%"

        noteViewModel
            .searchNotes(searchQuery)
            .observe(viewLifecycleOwner) { list ->
                noteAdapter
                    .differ
                    .submitList(list)
            }
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