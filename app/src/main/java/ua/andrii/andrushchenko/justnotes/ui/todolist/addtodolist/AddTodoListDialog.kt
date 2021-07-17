package ua.andrii.andrushchenko.justnotes.ui.todolist.addtodolist

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import ua.andrii.andrushchenko.justnotes.databinding.BottomSheetAddTodoListBinding
import ua.andrii.andrushchenko.justnotes.ui.base.BaseBottomSheetDialogFragment

@AndroidEntryPoint
class AddTodoListDialog : BaseBottomSheetDialogFragment<BottomSheetAddTodoListBinding>(BottomSheetAddTodoListBinding::inflate) {

    private val viewModel: AddTodoListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            editTextTaskNameInputLayout.editText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.todoListTitle = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            btnDone.setOnClickListener {
                dismiss()
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.addTodoListEvent.collect { event ->
                when (event) {
                    is AddTodoListViewModel.AddTodoListEvent.ShowInvalidInputMessage -> {
                        Toast.makeText(requireContext(), event.msg, Toast.LENGTH_LONG).show()
                    }
                    is AddTodoListViewModel.AddTodoListEvent.NavigateBackWithResult -> {
                        binding.editTextTaskNameInputLayout.clearFocus()
                        setFragmentResult(
                            "add_todo_list_request",
                            bundleOf("add_todo_list_result" to event.result)
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