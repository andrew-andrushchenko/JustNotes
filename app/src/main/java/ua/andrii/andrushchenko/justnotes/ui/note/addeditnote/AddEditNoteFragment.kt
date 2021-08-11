package ua.andrii.andrushchenko.justnotes.ui.note.addeditnote

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.databinding.FragmentAddEditNoteBinding
import ua.andrii.andrushchenko.justnotes.ui.base.BaseFragment
import ua.andrii.andrushchenko.justnotes.utils.ReminderHelper
import java.util.*

@AndroidEntryPoint
class AddEditNoteFragment :
    BaseFragment<FragmentAddEditNoteBinding>(FragmentAddEditNoteBinding::inflate) {

    private val viewModel: AddEditNoteViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            notesTitleInputLayout.editText?.setText(viewModel.noteTitle)
            notesContentInputLayout.editText?.setText(viewModel.noteContent)
            checkBoxImportant.isChecked = viewModel.noteIsUrgent
            checkBoxImportant.jumpDrawablesToCurrentState()

            notesTitleInputLayout.editText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    charSequence: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    viewModel.noteTitle = charSequence.toString()
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            notesContentInputLayout.editText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.noteContent = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {}
            })

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
            }

            initAlarmTimeDropdownMenu()

            btnDone.setOnClickListener {
                viewModel.onSaveClicked()
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.addEditNoteEvent.collect { event ->
                when (event) {
                    is AddEditNoteViewModel.AddEditNoteEvent.NavigateBackWithResult -> {
                        with(binding) {
                            notesTitleInputLayout.clearFocus()
                            notesContentInputLayout.clearFocus()
                        }
                        setFragmentResult(
                            "add_edit_note_request",
                            bundleOf("add_edit_note_result" to event.result)
                        )
                        findNavController().popBackStack()
                    }
                    is AddEditNoteViewModel.AddEditNoteEvent.ShowInvalidInputMessage -> {
                        Toast.makeText(requireContext(), event.msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
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
        val calendarConstraints = CalendarConstraints.Builder().setStart(System.currentTimeMillis() - 1000).build()
        val materialDatePicker = MaterialDatePicker.Builder.datePicker().apply {
            setTitleText("Select date")
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
            setTitleText("Select reminder time")
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
