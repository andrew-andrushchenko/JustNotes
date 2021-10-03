package ua.andrii.andrushchenko.justnotes.ui.note.addeditnote

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.databinding.FragmentAddEditNoteBinding
import ua.andrii.andrushchenko.justnotes.ui.base.BaseFragment
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_NOTE_REQUEST
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_NOTE_RESULT
import ua.andrii.andrushchenko.justnotes.utils.ReminderHelper
import ua.andrii.andrushchenko.justnotes.utils.setOnTextChangedListener
import java.util.*

@AndroidEntryPoint
class AddEditNoteFragment :
    BaseFragment<FragmentAddEditNoteBinding>(FragmentAddEditNoteBinding::inflate) {

    private val viewModel: AddEditNoteViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()

        with(binding) {
            notesTitleEditText.apply {
                setText(viewModel.noteTitle)
                setOnTextChangedListener {
                    viewModel.noteTitle = it
                }
            }
            notesContentEdittext.apply {
                setText(viewModel.noteContent)
                setOnTextChangedListener {
                    viewModel.noteContent = it
                }
            }

            checkBoxImportant.isChecked = viewModel.noteIsUrgent
            checkBoxImportant.jumpDrawablesToCurrentState()

            checkBoxImportant.setOnCheckedChangeListener { _, isChecked ->
                viewModel.noteIsUrgent = isChecked
            }

            checkBoxSetReminder.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        spinnerReminderTime.visibility = View.VISIBLE
                        viewModel.noteHasReminder = true
                    } else {
                        spinnerReminderTime.visibility = View.GONE
                        viewModel.noteHasReminder = false
                        viewModel.cancelReminder()
                    }
                }
                isChecked = viewModel.noteHasReminder
                spinnerReminderTime.visibility = if (viewModel.noteHasReminder) View.VISIBLE else View.GONE
            }

            initAlarmTimeDropdownMenu()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.addEditNoteEvent.collect { event ->
                when (event) {
                    is AddEditNoteViewModel.AddEditNoteEvent.NavigateBackWithResult -> {
                        with(binding) {
                            notesTitleEditText.clearFocus()
                            notesContentEdittext.clearFocus()
                        }
                        setFragmentResult(
                            ADD_EDIT_NOTE_REQUEST,
                            bundleOf(ADD_EDIT_NOTE_RESULT to event.result)
                        )
                        findNavController().popBackStack()
                    }
                    is AddEditNoteViewModel.AddEditNoteEvent.ShowInvalidInputMessage -> {
                        Toast.makeText(requireContext(), getString(event.msg), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            inflateMenu(R.menu.menu_add_edit_note)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_add_edit_note_save -> {
                        viewModel.onSaveClicked()
                        true
                    }
                    else -> true
                }
            }

            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.notesFragment,
                    R.id.todoListsFragment,
                )
            )
            val navController = findNavController()
            setupWithNavController(navController, appBarConfiguration)
        }
    }

    private fun initAlarmTimeDropdownMenu() = with(binding) {
        val arrayAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.reminder_time_for_note,
            android.R.layout.simple_spinner_dropdown_item
        )
        spinnerReminderTime.apply {
            adapter = arrayAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    when (position) {
                        0 -> viewModel.setReminderDateTimeByPeriod(ReminderHelper.ReminderPeriod.MORNING)
                        1 -> viewModel.setReminderDateTimeByPeriod(ReminderHelper.ReminderPeriod.AFTERNOON)
                        2 -> viewModel.setReminderDateTimeByPeriod(ReminderHelper.ReminderPeriod.EVENING)
                        3 -> viewModel.setReminderDateTimeByPeriod(ReminderHelper.ReminderPeriod.LATE_EVENING)
                        4 -> pickDate()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        }
    }

    private fun pickDate() {
        val calendarConstraints =
            CalendarConstraints.Builder().setStart(System.currentTimeMillis() - 1000).build()
        val materialDatePicker = MaterialDatePicker.Builder.datePicker().apply {
            setTitleText(getString(R.string.select_date))
            setCalendarConstraints(calendarConstraints)
            setSelection(MaterialDatePicker.todayInUtcMilliseconds())
        }.build()

        materialDatePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDateTime = Calendar.getInstance()
            selectedDateTime.timeInMillis = selection
            viewModel.savedYear = selectedDateTime[Calendar.YEAR]
            viewModel.savedMonth = selectedDateTime[Calendar.MONTH]
            viewModel.savedDay = selectedDateTime[Calendar.DAY_OF_MONTH]
            pickTime()
        }
        materialDatePicker.show(childFragmentManager, MaterialDatePicker::class.java.canonicalName)
    }

    private fun pickTime() {
        val materialTimePicker = MaterialTimePicker.Builder().apply {
            val calendar = Calendar.getInstance()
            setTitleText(getString(R.string.select_time))
            setTimeFormat(TimeFormat.CLOCK_24H)
            setHour(calendar[Calendar.HOUR])
            setMinute(calendar[Calendar.MINUTE])
        }.build()

        materialTimePicker.addOnPositiveButtonClickListener {
            viewModel.savedHour = materialTimePicker.hour
            viewModel.savedMinute = materialTimePicker.minute
            viewModel.saveReminderDateTimeMillis()
        }

        materialTimePicker.show(childFragmentManager, MaterialTimePicker::class.java.canonicalName)
    }
}
