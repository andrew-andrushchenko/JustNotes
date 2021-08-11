package ua.andrii.andrushchenko.justnotes.ui.note.cancelallreminders

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CancelAllRemindersDialogFragment : DialogFragment() {

    private val viewModel: CancelAllRemindersViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm cancellation")
            .setMessage("Do you really want to cancel all reminders?")
            .setNegativeButton("No", null)
            .setPositiveButton("Yes") { _, _ ->
                viewModel.onConfirmClick()
            }
            .create()
}