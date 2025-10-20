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

    // layoutAdditional
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
        attach_an_action_to_back_button()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteViewModel = (activity as MainActivity).noteViewModel
        mView = view

        assign_bottomNavigation_item()

        init_several_views()

        action_definition_with_note(
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

    private fun `attach_an_action_to_back_button`() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                switching_to_homeFragment()
            }
        }

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, callback)
        callback.isEnabled = true
    }

    private fun `assign_bottomNavigation_item`() {
        bottomNavigationView = (activity as MainActivity).bottomNavigation
        bottomNavigationView
            .menu
            .findItem(R.id.notes)
            .isChecked = true
    }

    private fun `init_several_views`() {
        viewCircuitColorIndicator = binding.circuitColorIndicator
        layoutWebURL = binding.layoutWebURL
    }

    private fun `action_definition_with_note`(message: String) {
        if (message == "note creation") {
            action_at_note_creation()
        } else if (message == "change after save") {
            action_at_note_change()
        }
    }

    private fun `action_at_note_creation`() {
        initial_settings()

        noteViewModel.dataCurrentNote.value = Note(id = 0)

        noteViewModel.dataCurrentNote.value!!.colorOfCircuit = ContextCompat.getColor(
            requireContext(),
            R.color.blue
        )
    }

    private fun `action_at_note_change`() {
        noteViewModel.status = 1
        initial_settings()

        noteViewModel.dataCurrentNote.value = requireArguments().get("note") as Note

        setting_up_noteData()
    }

    private fun `initial_settings`() {
        filling_in_the_lists_of_views()

        setting_up_clicks_views()

        // layoutAdditional
        initialization_layoutAdditional()
        filling_in_the_lists_of_layoutMiscellaneous_views()
        settng_up_clicks_of_layoutMiscellaneous_views()

        setting_up_dialogNameMatchWarning()
        setting_up_dialogAddUrl()

        if (noteViewModel.status == 1) {
            setting_up_dialogDeleteNote()
        }
    }

    private fun `filling_in_the_lists_of_views`() {
        filling_in_the_list_of_textFields()
        filling_in_the_list_of_inputFields()
        filling_in_the_list_of_imageViews()
    }

    private fun `filling_in_the_list_of_textFields`() {
        textFields.add(0, binding.textDateItem)
        textFields.add(1, binding.textWebURL)
    }

    private fun `filling_in_the_list_of_inputFields`() {
        inputFields.add(0, binding.inputNoteTitle)
        inputFields.add(1, binding.inputNoteBody)
    }

    private fun `filling_in_the_list_of_imageViews`() {
        imageViews.add(0, binding.imageBack)
        imageViews.add(1, binding.imageSave)
        imageViews.add(2, binding.imageNote)
        imageViews.add(3, binding.imageRemoveImage)
        imageViews.add(4, binding.imageRemoveWebURL)
    }

    private fun `setting_up_clicks_views`() {
        viewCircuitColorIndicator.setOnClickListener(this)
        imageViews[0].setOnClickListener(this)
        imageViews[1].setOnClickListener(this)
        imageViews[3].setOnClickListener(this)
        imageViews[4].setOnClickListener(this)
    }

    private fun `initialization_layoutAdditional`() {
        init_layoutAdditional()
        init_textAdditional()
        init_bottom_sheet_behavior()
    }

    private fun `init_layoutAdditional`() {
        layoutAdditional = binding.layoutAdditional
    }

    private fun `init_textAdditional`() {
        textAdditional = layoutAdditional.textAdditional
    }

    private fun `init_bottom_sheet_behavior`() {
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

    private fun `filling_in_the_lists_of_layoutMiscellaneous_views`() {
        filling_in_the_list_of_layouts()
    }

    private fun `filling_in_the_list_of_layouts`() {
        layouts.add(0, layoutAdditional.layoutChangeColor)
        layouts.add(1, layoutAdditional.layoutAddImage)
        layouts.add(2, layoutAdditional.layoutAddUrl)
        if (noteViewModel.status == 1) {
            layouts.add(3, layoutAdditional.layoutDeleteNote)
            layoutAdditional.layoutDeleteNote.visibility = View.VISIBLE
        }
    }

    private fun `settng_up_clicks_of_layoutMiscellaneous_views`() {
        setting_up_click_textMiscellaneous()
        setting_up_clicks_layouts()
    }

    private fun `setting_up_click_textMiscellaneous`() {
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

    private fun `setting_up_clicks_layouts`() {
        layouts[0].setOnClickListener {
            bottomSheetBehavior.state =
                BottomSheetBehavior.STATE_COLLAPSED
            showColorPickerDialog()
        }

        layouts[1].setOnClickListener {
            setImage()
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

    private fun `setting_up_dialogNameMatchWarning`() {
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
        setting_the_selected_color(color)
    }

    private fun `setting_the_selected_color`(color: Int) {
        viewCircuitColorIndicator.setCardBackgroundColor(color)
    }

    private fun `setting_up_dialogAddUrl`() {
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

    private fun `setting_up_dialogDeleteNote`() {
        val builder = AlertDialog.Builder(context)
        val view = LayoutDeleteNoteBinding.inflate(LayoutInflater.from(context))

        builder.setView(view.root)
        dialogDeleteNote = builder.create()

        view.delete.setOnClickListener {
            noteViewModel.deleteNote(
                noteViewModel.dataCurrentNote.value!!
            )
            switching_to_homeFragment()
            dialogDeleteNote.dismiss()
        }

        view.cancel.setOnClickListener {
            dialogDeleteNote.dismiss()
        }
    }

    private fun setImage() {
        if (all_permissions_granted()) {
            /**
                * permissions предоставлены.
                * --------------------------
                * permisions granted.
            */
            selectImage()
        } else {
            /**
                * ActivityCompat - помощник для доступа к функциям Activity.
                * ----------------------------------------------------------
                * ActivityCompat - a helper for accessing to Activity's functions.
            */
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                // список запрашиваемых permissions (the list of requested permissions)
                REQUEST_CODE_STORAGE_PERMISSION
            )
        }
    }

    private fun `all_permissions_granted`(): Boolean {
        return activity?.let {
            /**
                * ContextCompat - a helper for accessing to functions in context.
                * --------------------------------------------------------------
                * ContextCompat - помощник для доступа к функциям в контексте (context).
            */

            /**
                * checkSelfPermission() - the function(), which defines
                * were you granted specific permission.
                * -----------------------------------------------------
                * checkSelfPermission() - функция, которая определяет,
                * было ли вам предоставлено конкретное разрешение.
            */

            /**
                * PacketManager - a class for extraction different information
                * that apply to the packages of appps,
                * which at this moment setting on devce
                * ----------------------------------------------------------------
                * PacketManager - класс для извлечения различного рода информации,
                * относящейся к пакетам приложений,
                * которые в данный момент установлены на устройстве.
            */
            ContextCompat.checkSelfPermission(
                it.applicationContext,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } == PackageManager.PERMISSION_GRANTED
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
                callback_for_save_note(noteTitle)
                switching_to_homeFragment()
            }
        }
    }

    // TODO give an another name to this function
    private fun `callback_for_save_note`(noteTitle: String) {
        noteViewModel.dataCurrentNote.value!!.noteTitle = noteTitle

        installation_dateTime()

        noteViewModel.addNote(noteViewModel.dataCurrentNote.value!!)
    }

    private fun `installation_dateTime`() {
        val dateTime = SimpleDateFormat(
            "EEEE, dd MMMM yyyy HH:mm",
            Locale.getDefault()
        )
            .format(Date())
            .toString()

        /**
            * This code sets the text for the textDateTime object,
            * displaying the current date and time in the specified format.
            *
            * the string "EEEE" indicates
            * that you need to display the day of the week in full.
            * Example, Tuesday.
            *
            * the string "dd" indicates
            * that you need to display the day of the month.
            * Example, 20.
            *
            * the string "MMMM" indicates
            * that you need to display the name of the month in full.
            * Example, june.
            *
            * the string "yyyy" indicates
            * that you need to display the year in four digit format.
            * Example, 2023.
            *
            * the string "HH" indicates
            * that you need to display hours in 24 hour format.
            * Example, 22.
            *
            * the string "mm" indicates
            * that you need to display mminutes.
            * Example, 30.
            *
            * The code "Locale.getDefault()" is used to set
            * regional settings.
            * ------------------------------------------------------
            * Этот код устанавливает текст для объекта textDateTime,
            * отображая текущую дату и время в указанном формате.
            *
            * "EEEE" указывает,
            * что нужно отображать день недели полностью.
            * Например, вторник.
            *
            * "dd" указывает,
            * что нужно отображать день месяца.
            * например, 20.
            *
            * "MMMM" указывает,
            * что нужно отображать название месяца полностью.
            * например, июня.
            *
            * "yyyy" указывает,
            * что нужно отображать год в четырехзначном формате.
            * например, 2023.
            *
            * "HH" указывает,
            * что нужно отображать часы в 24 - часовом формате.
            * например, 22.
            *
            * "mm" указывает,
            * что нужно отображать минуты.
            * например, 30.
            *
            * Locale.getDefault() используется для задания
            * региональных настроек.
        */

        noteViewModel.dataCurrentNote.value!!.dateTime = dateTime
    }

    private fun `setting_up_noteData`() {
        noteViewModel.dataCurrentNote.value.let { note ->
            inputFields[0].setText(note!!.noteTitle)

            initialNoteTitle = note.noteTitle

            inputFields[1].setText(note.noteBody)

            setting_the_selected_color(note.colorOfCircuit!!)

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

    private fun updateNote() {
        val noteTitle = inputFields[0].text.toString()
        val noteBody = inputFields[1].text.toString()

        noteViewModel.dataCurrentNote.value!!.noteBody = noteBody

        if (noteTitle == initialNoteTitle) {
            someOperation(noteTitle)
        } else {
            checkNoteTitle(noteTitle) {
                if (!it) {
                    someOperation(noteTitle)
                }
            }
        }
    }

    private fun someOperation(noteTitle: String) {
        noteViewModel.dataCurrentNote.value!!.noteTitle = noteTitle
        noteViewModel.updateNote(noteViewModel.dataCurrentNote.value!!)
        switching_to_homeFragment()
    }

    private fun `switching_to_homeFragment`() {
        clearing_of_data()
        mView.findNavController().navigate(
            R.id.action_noteFragment_to_homeFragment
        )
    }

    private fun `clearing_of_data`() {
        if (textFields[0].visibility == View.GONE)
            clearing_a_data_time()

        if (imageViews[2].visibility == View.VISIBLE)
            deleting_a_data_of_image()

        if (layoutWebURL.visibility == View.VISIBLE)
            deleting_a_data_of_webURL()

        noteViewModel.dataCurrentNote.value = null
    }

    private fun `clearing_a_data_time`() {
        textFields[0].text = ""
        textFields[0].visibility = View.GONE
    }

    private fun `deleting_a_data_of_image`() {
        imageViews[2].setImageBitmap(null)
        imageViews[2].visibility = View.GONE
        imageViews[3].visibility = View.GONE
    }

    private fun `deleting_a_data_of_webURL`() {
        textFields[1].text = null
        layoutWebURL.visibility = View.GONE
    }

    override fun onClick(view: View?) {
        when (view) {
            viewCircuitColorIndicator -> showColorPickerDialog()

            imageViews[0] -> switching_to_homeFragment()

            imageViews[1] -> if (noteViewModel.status == 0) saveNote() else updateNote()

            imageViews[3] -> deleting_a_data_of_image()

            imageViews[4] -> deleting_a_data_of_webURL()
        }
    }
}