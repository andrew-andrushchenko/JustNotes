package ua.andrii.andrushchenko.justnotes.ui.task.addedittask

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.databinding.BottomSheetAddEditTaskBinding
import ua.andrii.andrushchenko.justnotes.ui.base.BaseBottomSheetDialogFragment
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_TASK_REQUEST
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_TASK_RESULT

@AndroidEntryPoint
class AddEditTaskDialog :
    BaseBottomSheetDialogFragment<BottomSheetAddEditTaskBinding>(BottomSheetAddEditTaskBinding::inflate) {

    private val viewModel: AddEditTaskViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            addEditTaskTitle.text = viewModel.title
            editTextTaskNameInputLayout.editText?.apply {
                setText(viewModel.taskName)
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        viewModel.taskName = s.toString()
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })
            }

            checkBoxImportant.apply {
                isChecked = viewModel.taskImportance
                jumpDrawablesToCurrentState()
                setOnCheckedChangeListener { _, isChecked ->
                    viewModel.taskImportance = isChecked
                }
            }


            textViewDateCreated.apply {
                isVisible = viewModel.task != null
                text = getString(
                    R.string.created, viewModel.task?.createdDateFormatted
                )
            }

            btnDone.setOnClickListener {
                dismiss()
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.addEditTaskEvent.collect { event ->
                when (event) {
                    is AddEditTaskViewModel.AddEditTaskEvent.ShowInvalidInputMessage -> {
                        Toast.makeText(requireContext(), getString(event.msg), Toast.LENGTH_LONG).show()
                    }
                    is AddEditTaskViewModel.AddEditTaskEvent.NavigateBackWithResult -> {
                        binding.editTextTaskNameInputLayout.clearFocus()
                        setFragmentResult(
                            ADD_EDIT_TASK_REQUEST,
                            bundleOf(ADD_EDIT_TASK_RESULT to event.result)
                        )
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onSaveClicked()
    }
}