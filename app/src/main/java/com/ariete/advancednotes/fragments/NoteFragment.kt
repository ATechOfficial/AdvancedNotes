package com.ariete.advancednotes.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.ariete.advancednotes.R
import com.ariete.advancednotes.activities.MainActivity
import com.ariete.advancednotes.databinding.FragmentNoteBinding
import com.ariete.advancednotes.databinding.LayoutAddUrlBinding
import com.ariete.advancednotes.databinding.LayoutAdditionalBinding
import com.ariete.advancednotes.databinding.LayoutDeleteNoteBinding
import com.ariete.advancednotes.databinding.LayoutNameWarningBinding
import com.ariete.advancednotes.helper.toast
import com.ariete.advancednotes.model.Note
import com.ariete.advancednotes.viewmodel.NoteViewModel
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

class NoteFragment :
    Fragment(R.layout.fragment_note),
    OnClickListener {

    private lateinit var initialNoteTitle: String

    companion object {
        const val REQUEST_CODE_STORAGE_PERMISSION = 99
    }

    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var mView: View

    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var viewCircuitColorIndicator: CardView
    private lateinit var layoutWebURL: LinearLayout

    private val textFields = mutableListOf<TextView>()
    private val inputFields = mutableListOf<EditText>()
    private val imageViews = mutableListOf<ImageView>()

    private lateinit var layoutAdditional: LayoutAdditionalBinding

    private lateinit var textAdditional: TextView

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private val layouts = mutableListOf<LinearLayout>()

    // dialogs
    private lateinit var dialogNameMatchWarning: AlertDialog
    private lateinit var dialogAddURL: AlertDialog
    private lateinit var dialogDeleteNote: AlertDialog

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleResult(result)
    }
    /**
        * registerForActivityResult() is a function provided by the AndroidX ActivityResult library.
        * It is used to register a callback that will handle the result of an activity.
        *
        * It takes one argument,
        * which is an instance of ActivityResultContract.
        * In this case,
        * we are using ActivityResultContracts.StartActivityForResult(),
        * which is an activity result contract
        * for starting an activity and receiving a result.
        * ---------------------------------------------------------------------------------------------
        * registerForActivityResult() - это функция, предоставляемая библиотекой AndroidX ActivityResult.
        * Она используется для регистрации функции обратного вызова,
        * которая будет обрабатывать результат aктивности.
        *
        * Функция принимает один аргумент - экземпляр класса ActivityResultContract.
        * В данном случае мы используем ActivityResultContracts.StartActivityForResult(),
        * который представляет собой "контракт на результат активности"
        * для начала активности и получения результата.
    */

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentNoteBinding.inflate(
            inflater,
            container,
            false
        )

        animation()
        setBackButtonAction()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteViewModel = (activity as MainActivity).noteViewModel
        mView = view

        assignBottomNavigationItem()

        initSeveralViews()

        setupNoteScreen(
            requireArguments().getString("message")!!
        )
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

    private fun setBackButtonAction() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                switchToHomeFragment()
            }
        }

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, callback)
        callback.isEnabled = true
    }

    private fun assignBottomNavigationItem() {
        bottomNavigationView = (activity as MainActivity).bottomNavigation
        bottomNavigationView
            .menu
            .findItem(R.id.notes)
            .isChecked = true
    }

    // tip: views shall be initialized in the onViewCreated directly
    private fun initSeveralViews() {
        viewCircuitColorIndicator = binding.circuitColorIndicator
        layoutWebURL = binding.layoutWebURL
    }

    private fun setupNoteScreen(message: String) {
        if (message == "note creation") {
            setupNewNoteScreen()
        } else if (message == "change after save") {
            setupExistingNoteScreen()
        }
    }

    private fun setupNewNoteScreen() {
        initNoteSettings()

        noteViewModel.dataCurrentNote.value = Note(id = 0)

        noteViewModel.dataCurrentNote.value!!.colorOfCircuit = ContextCompat.getColor(
            requireContext(),
            R.color.blue
        )
    }

    private fun setupExistingNoteScreen() {
        noteViewModel.status = 1
        initNoteSettings()

        noteViewModel.dataCurrentNote.value = requireArguments().get("note") as Note

        setupNoteData()
    }

    private fun initNoteSettings() {
        fillInViewsLists()

        setupClicksViews()

        // layoutAdditional
        initLayoutAdditional()
        fillInLayoutsList()
        setupClicksLayoutForMiscellaneousViews()

        setupDialogNameMatchWarning()
        setupDialogOnUrlAddition()

        if (noteViewModel.status == 1) {
            setupDialogOnNoteDeletion()
        }
    }

    private fun fillInViewsLists() {
        fillInTextFieldsList()
        fillInInputFieldsList()
        fillInImageViewsList()
    }

    private fun fillInTextFieldsList() {
        textFields.add(0, binding.textDateItem)
        textFields.add(1, binding.textWebURL)
    }

    private fun fillInInputFieldsList() {
        inputFields.add(0, binding.inputNoteTitle)
        inputFields.add(1, binding.inputNoteBody)
    }

    private fun fillInImageViewsList() {
        imageViews.add(0, binding.imageBack)
        imageViews.add(1, binding.imageSave)
        imageViews.add(2, binding.imageNote)
        imageViews.add(3, binding.imageRemoveImage)
        imageViews.add(4, binding.imageRemoveWebURL)
    }

    private fun setupClicksViews() {
        viewCircuitColorIndicator.setOnClickListener(this)
        imageViews[0].setOnClickListener(this)
        imageViews[1].setOnClickListener(this)
        imageViews[3].setOnClickListener(this)
        imageViews[4].setOnClickListener(this)
    }

    private fun initLayoutAdditional() {
        layoutAdditional = binding.layoutAdditional
        textAdditional = layoutAdditional.textAdditional

        initBottomMenuBehaviour()
    }

    private fun initBottomMenuBehaviour() {
        /**
            * The BottomSheetBehavior class is responsible for the behavior
            * bottom sheet on the screen.
            *
            * It can control the opening and hide sheet,
            * it's height
            * and also a treatment of user events.
            * -----------------------------------------------
            * Класс BottomSheetBehavior отвечает за поведение
            * нижнего листа (bottom sheet) на экране.
            *
            * Он может управлять раскрытием и
            * скрытием листа,
            * его высотой,
            * а также обработкой пользовательских событий.
        */
        bottomSheetBehavior = BottomSheetBehavior.from(
            layoutAdditional.additional
        )
        /**
            * The function from() is used to get the BottomSheetBehavior
            * associated with the view.
            * -------------------------------------------------
            * Функция from используется для получения
            * экзепляра класса BottomSheetBehavior,
            * связанного с view.
        */
    }

    private fun fillInLayoutsList() {
        layouts.add(0, layoutAdditional.layoutChangeColor)
        layouts.add(1, layoutAdditional.layoutAddImage)
        layouts.add(2, layoutAdditional.layoutAddUrl)
        if (noteViewModel.status == 1) {
            layouts.add(3, layoutAdditional.layoutDeleteNote)
            layoutAdditional.layoutDeleteNote.visibility = View.VISIBLE
        }
    }

    private fun setupClicksLayoutForMiscellaneousViews() {
        setupClicksTextMiscellaneous()
        setupClicksLayout()
    }

    private fun setupClicksTextMiscellaneous() {
        /**
            * expanded - расширенный
            * collapsed - свернутый
        */

        layoutAdditional.textAdditional.setOnClickListener {
            if (bottomSheetBehavior.state
                != BottomSheetBehavior.STATE_EXPANDED
            ) {
                bottomSheetBehavior.setState(
                    BottomSheetBehavior
                        .STATE_EXPANDED
                )
            } else {
                bottomSheetBehavior.setState(
                    BottomSheetBehavior
                        .STATE_COLLAPSED
                )
            }
        }
    }

    private fun setupClicksLayout() {
        layouts[0].setOnClickListener {
            bottomSheetBehavior.state =
                BottomSheetBehavior.STATE_COLLAPSED
            showColorPickerDialog()
        }

        layouts[1].setOnClickListener {
            setupNoteImage()
        }

        layouts[2].setOnClickListener {
            bottomSheetBehavior.state =
                BottomSheetBehavior.STATE_COLLAPSED
            dialogAddURL.show()
        }

        if (noteViewModel.status == 1) {
            layouts[3].setOnClickListener {
                bottomSheetBehavior.state =
                    BottomSheetBehavior.STATE_COLLAPSED
                dialogDeleteNote.show()
            }
        }
    }

    private fun setupDialogNameMatchWarning() {
        val builder = AlertDialog.Builder(context)

        val view = LayoutNameWarningBinding.inflate(
            LayoutInflater.from(context)
        )

        builder.setView(view.root)
        dialogNameMatchWarning = builder.create()

        view.ok.setOnClickListener {
            dialogNameMatchWarning.dismiss()
        }
    }

    private fun showColorPickerDialog() {
        val colorPickerDialog = ColorPickerDialog.Builder(
            requireContext()
        ).let {
            val colorPallete: ColorPickerDialog.Builder = it
                .setTitle(getString(R.string.selection_of_color))
                .setPositiveButton(
                    getPaintText(
                        getString(R.string.confirm),
                        R.color.red
                    ),
                    ColorEnvelopeListener { envelope, _ ->
                        val selectedColor = envelope.color
                        handleColorSelected(selectedColor)
                    }
                )
                .setNegativeButton(
                    getPaintText(
                        getString(R.string.cancel),
                        R.color.blue
                    ),
                ) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }

            colorPallete
        }
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true)
            .setBottomSpace(4)
            .create()

        colorPickerDialog
            .window!!
            .setBackgroundDrawableResource(
                R.drawable.background_color_picker_view
            )

        colorPickerDialog.setOnShowListener {
            colorPickerDialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .also { positive ->
                    positive.setBackgroundResource(
                        R.drawable.selector_color_confirm
                    )

                    val typeFace = ResourcesCompat.getFont(
                        requireContext(),
                        R.font.ubuntu_medium
                    )
                    positive.typeface = typeFace
                    positive.includeFontPadding = false

                    positive.setPadding(8, 8, 8, 8)

                    val textSizeInSp = 16f
                    positive.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeInSp)
                    positive.isAllCaps = false
                }

            colorPickerDialog
                .getButton(AlertDialog.BUTTON_NEGATIVE)
                .also { negative ->
                    negative.setBackgroundResource(
                        R.drawable.selector_color_cancel
                    )

                    val typeFace = ResourcesCompat.getFont(
                        requireContext(),
                        R.font.ubuntu_medium
                    )
                    negative.typeface = typeFace
                    negative.includeFontPadding = false

                    (negative.layoutParams as ViewGroup.MarginLayoutParams)
                        .setMargins(0, 0, 32,0)

                    negative.setPadding(8, 8, 8, 8)

                    val textSizeInSp = 16f
                    negative.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeInSp)
                    negative.isAllCaps = false
                }
        }

        colorPickerDialog.show()
    }

    private fun getPaintText(
        text: String,
        color: Int
    ): SpannableString {
        val spannableString = SpannableString(text)

        val fcsRed = ForegroundColorSpan(
            ContextCompat.getColor(
                requireContext(),
                color
            )
        )

        spannableString.setSpan(
            fcsRed,
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }

    private fun handleColorSelected(color: Int) {
        noteViewModel.dataCurrentNote.value!!.colorOfCircuit = color
        setupDialogOnColorSelection(color)
    }

    private fun setupDialogOnColorSelection(color: Int) {
        viewCircuitColorIndicator.setCardBackgroundColor(color)
    }

    private fun setupDialogOnUrlAddition() {
        val builder = AlertDialog.Builder(context)
        val view = LayoutAddUrlBinding.inflate(LayoutInflater.from(context))

        builder.setView(view.root)
        dialogAddURL = builder.create()

        dialogAddURL
            .window!!
            .setBackgroundDrawableResource(R.drawable.background_dialog_add_url)

        val inputURL = view.dialogContent
        inputURL.requestFocus()
        /**
            * The function requestFocus() is used to set
            * the focus on specific element.
            * ------------------------------------------
            * Функция requestFocus() используется для того чтобы
            * установить фокус на определенном элементе.
        */

        view.add.setOnClickListener {
            if (inputURL.text
                    .toString()
                    .trim()
                    .isEmpty()
            ) {
                /**
                    * The trim() function is used to remove
                    * any leading and trailing whitespaces from the string.
                    * -----------------------------------------------------
                    * Функция trim() используется для удаления
                    * начальных и конечных пробелов из строки.
                */
                activity?.toast("Enter URL")
            }
            else if (!Patterns.WEB_URL
                    .matcher(inputURL.text.toString())
                    .matches()
            ) activity?.toast("Enter valid URL")
            else {
                inputURL.text.toString().let {
                    noteViewModel.dataCurrentNote.value!!.webLink = it
                    textFields[1].text = it
                }

                dialogAddURL.dismiss()

                layoutWebURL.visibility = View.VISIBLE
                inputURL.setText("")
            }
        }

        view.cancel.setOnClickListener {
            dialogAddURL.dismiss()
        }
    }

    private fun setupDialogOnNoteDeletion() {
        val builder = AlertDialog.Builder(context)
        val view = LayoutDeleteNoteBinding.inflate(LayoutInflater.from(context))

        builder.setView(view.root)
        dialogDeleteNote = builder.create()

        view.delete.setOnClickListener {
            noteViewModel.deleteNote(
                noteViewModel.dataCurrentNote.value!!
            )
            switchToHomeFragment()
            dialogDeleteNote.dismiss()
        }

        view.cancel.setOnClickListener {
            dialogDeleteNote.dismiss()
        }
    }

    private fun setupNoteImage() = if (checkPermissionsGranted()) {
        // when all the required permissions have been granted
        selectImage()
    } else {
        // for version differences reference see checkPermissionsGranted()
        val permissionToRequest = when {
            // Android 13 (API 33) and higher
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                android.Manifest.permission.READ_MEDIA_IMAGES
            }

            // Android 12 (API 32) and lower
            else -> {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }

        // ActivityCompat - a helper for accessing to Activity's functions.
        ActivityCompat.requestPermissions(
            requireActivity(),
            // the required permissions
            arrayOf(permissionToRequest),
            // the list of requested permissions
            REQUEST_CODE_STORAGE_PERMISSION
        )
    }

    private fun checkPermissionsGranted(): Boolean {
        val context = requireContext()

        // ContextCompat - a helper for accessing to functions in context.

        /**
         * checkSelfPermission() - the function(), which defines
         * whether you've been granted a specific permission.
         */

        /**
         * PacketManager - a class for extracting different information
         * that refers to the packages of apps,
         * which are set on the device at the moment
         */

        return when {
            // Android 14 (API 34) and higher
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ) == PackageManager.PERMISSION_GRANTED
            }

            // Android 13 (API 33) and higher
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                /*
                    Android 13 (API 33) and higher require granulated permissions instead of
                    a simple request for the whole storage media types data access
                 */
                return ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_MEDIA_IMAGES,
                ) == PackageManager.PERMISSION_GRANTED
            }

            // Android 12 (API 32) and lower
            else -> {
                return activity?.let {
                    ContextCompat.checkSelfPermission(
                        it.applicationContext,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                } == PackageManager.PERMISSION_GRANTED
            }
        }

    }


    private fun selectImage() {
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

        activityResultLauncher.launch(intent)
    }

    private fun handleResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let {
                val selectedImageUri = it.data
                selectedImageUri?.also {
                    try {
                        selectedImageUri.toString().let { uri ->
                            noteViewModel.dataCurrentNote.value!!.imagePath = uri
                            Glide.with(requireContext())
                                .load(uri)
                                .into(imageViews[2])
                        }

                        imageViews[2].visibility = View.VISIBLE
                        imageViews[3].visibility = View.VISIBLE
                    } catch (exception: Exception) {
                        activity?.toast(
                            exception.message.toString()
                        )
                    }
                }
            }
        }
    }

    private fun checkNoteTitle (
        noteTitle: String,
        callback: (Boolean) -> Unit
    ) {
        noteViewModel
            .searchNotes(noteTitle)
            .observe(viewLifecycleOwner) { list ->
                if (noteTitle.isEmpty()) {
                    activity?.toast("Note title can't be empty!")
                    callback(true)
                }
                else if (list.isNotEmpty()) {
                    dialogNameMatchWarning.show()
                    callback(true)
                }
                else callback(false)
            }
    }

    private fun saveNote() {
        val noteTitle = inputFields[0].text.toString()
        val noteBody = inputFields[1].text.toString()

        noteViewModel.dataCurrentNote.value!!.noteBody = noteBody

        checkNoteTitle(noteTitle) {
            if (!it) {
                callbackOnNoteSave(noteTitle)
                switchToHomeFragment()
            }
        }
    }

    private fun callbackOnNoteSave(noteTitle: String) {
        noteViewModel.dataCurrentNote.value!!.noteTitle = noteTitle

        setupNoteDatetime()

        noteViewModel.addNote(noteViewModel.dataCurrentNote.value!!)
    }

    private fun setupNoteDatetime() {
        noteViewModel.dataCurrentNote.value!!.timestamp = System.currentTimeMillis()
    }

    private fun getFormattedDatetime(timestamp: Long): String {
        val date = Date(timestamp)
        val dateFormat = SimpleDateFormat(
            "EEEE, dd MMMM yyyy HH:mm",
            Locale.getDefault()
        )

        return dateFormat.format(date)
    }

    private fun setupNoteData() {
        noteViewModel.dataCurrentNote.value.let { note ->
            inputFields[0].setText(note!!.noteTitle)

            initialNoteTitle = note.noteTitle

            inputFields[1].setText(note.noteBody)

            textFields[0].text = getFormattedDatetime(note.timestamp)

            setupDialogOnColorSelection(note.colorOfCircuit!!)

            note.imagePath?.let {
                Glide.with(requireContext())
                    .load(it)
                    .into(imageViews[2])

                imageViews[2].visibility = View.VISIBLE
                imageViews[3].visibility = View.VISIBLE
            }

            note.webLink?.let {
                textFields[1].text = it
                layoutWebURL.visibility = View.VISIBLE
            }
        }
    }

    private fun updateNoteAndSwitchHome() {
        val noteTitle = inputFields[0].text.toString()
        val noteBody = inputFields[1].text.toString()

        noteViewModel.dataCurrentNote.value!!.noteBody = noteBody

        if (noteTitle == initialNoteTitle) {
            saveNoteAndSwitchHome(noteTitle)
        } else {
            checkNoteTitle(noteTitle) {
                if (!it) {
                    saveNoteAndSwitchHome(noteTitle)
                }
            }
        }
    }

    private fun saveNoteAndSwitchHome(noteTitle: String) {
        noteViewModel.dataCurrentNote.value!!.noteTitle = noteTitle
        noteViewModel.updateNote(noteViewModel.dataCurrentNote.value!!)
        switchToHomeFragment()
    }

    private fun switchToHomeFragment() {
        clearNoteAttachmentsData()
        mView.findNavController().navigate(
            R.id.action_noteFragment_to_homeFragment
        )
    }

    private fun clearNoteAttachmentsData() {
        if (textFields[0].visibility == View.GONE)
            clearTimeData()

        if (imageViews[2].visibility == View.VISIBLE)
            clearImageData()

        if (layoutWebURL.visibility == View.VISIBLE)
            clearWebURLData()

        noteViewModel.dataCurrentNote.value = null
    }

    private fun clearTimeData() {
        textFields[0].text = ""
        textFields[0].visibility = View.GONE
    }

    private fun clearImageData() {
        imageViews[2].setImageBitmap(null)
        imageViews[2].visibility = View.GONE
        imageViews[3].visibility = View.GONE
    }

    private fun clearWebURLData() {
        textFields[1].text = null
        layoutWebURL.visibility = View.GONE
    }

    override fun onClick(view: View?) {
        when (view) {
            viewCircuitColorIndicator -> showColorPickerDialog()

            imageViews[0] -> switchToHomeFragment()

            imageViews[1] -> if (noteViewModel.status == 0) saveNote() else updateNoteAndSwitchHome()

            imageViews[3] -> clearImageData()

            imageViews[4] -> clearWebURLData()
        }
    }
}