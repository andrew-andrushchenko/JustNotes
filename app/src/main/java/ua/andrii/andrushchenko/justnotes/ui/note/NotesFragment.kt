package ua.andrii.andrushchenko.justnotes.ui.note

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.databinding.FragmentNotesBinding
import ua.andrii.andrushchenko.justnotes.domain.Note
import ua.andrii.andrushchenko.justnotes.ui.base.BaseFragment
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import ua.andrii.andrushchenko.justnotes.utils.onQueryTextChanged
import ua.andrii.andrushchenko.justnotes.utils.setupStaggeredGridLayoutManager

@AndroidEntryPoint
class NotesFragment : BaseFragment<FragmentNotesBinding>(FragmentNotesBinding::inflate) {

    private val viewModel: NotesViewModel by viewModels()

    private lateinit var searchView: SearchView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val notesAdapter = NotesAdapter(object : NotesAdapter.OnItemClickListener {
            override fun onItemClick(note: Note) {
                viewModel.onNoteSelected(note)
            }
        })

        with(binding) {
            recyclerView.apply {
                adapter = notesAdapter
                setupStaggeredGridLayoutManager(
                    resources.getDimensionPixelSize(R.dimen.indent_8dp)
                )
                setHasFixedSize(true)
            }

            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val note = notesAdapter.currentList[viewHolder.adapterPosition]
                    viewModel.onNoteSwiped(note)
                }
            }).attachToRecyclerView(recyclerView)

            fabAddNote.setOnClickListener {
                viewModel.onAddNewNoteClicked()
            }
        }

        setFragmentResultListener("add_edit_note_request") { _, bundle ->
            val result = bundle.getInt("add_edit_note_result")
            viewModel.onAddEditResult(result)
        }

        viewModel.notes.observe(viewLifecycleOwner) {
            notesAdapter.submitList(it)
            toggleTextViewEmpty(it.isEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.notesEvent.collect { event ->
                when (event) {
                    is NotesViewModel.NoteEvent.NavigateToAddNoteScreen -> {
                        val direction =
                            NotesFragmentDirections.actionNotesFragmentToAddEditNoteFragment(
                                note = null,
                                title = getString(R.string.add_note)
                            )
                        findNavController().navigate(direction)
                    }
                    is NotesViewModel.NoteEvent.NavigateToEditNoteScreen -> {
                        val direction =
                            NotesFragmentDirections.actionNotesFragmentToAddEditNoteFragment(
                                note = event.note,
                                title = getString(R.string.edit_note)
                            )
                        findNavController().navigate(direction)
                    }
                    is NotesViewModel.NoteEvent.NavigateToDeleteAllNotes -> {

                    }
                    is NotesViewModel.NoteEvent.ShowUndoDeleteNoteMessage -> {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.note_deleted),
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(getString(R.string.undo)) {
                                viewModel.onUndoDeleteClicked(event.note)
                            }.show()
                    }
                    is NotesViewModel.NoteEvent.ShowNoteSavedConfirmationMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                    is NotesViewModel.NoteEvent.NavigateToCancelAllReminders -> {
                        val direction = NotesFragmentDirections.actionNotesFragmentToCancelAllRemindersDialogFragment()
                        findNavController().navigate(direction)
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    private fun toggleTextViewEmpty(isVisible: Boolean) {
        with(binding) {
            textViewEmpty.visibility = if (isVisible) View.VISIBLE else View.GONE
            recyclerView.visibility = if (!isVisible) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_notes, menu)

        val searchItem = menu.findItem(R.id.action_notes_search)
        searchView = searchItem.actionView as SearchView

        val pendingQuery = viewModel.notesSearchQuery.value
        if (pendingQuery != null && pendingQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(pendingQuery, false)
        }

        searchView.onQueryTextChanged {
            viewModel.notesSearchQuery.value = it
        }

        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_notes_hide_not_important).isChecked =
                viewModel.preferencesFlow.first().hideNotImportant
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notes_sort_by_name -> {
                viewModel.onSortOrderSelected(SortOrder.BY_NAME)
                true
            }
            R.id.action_notes_sort_by_date_last_edited -> {
                viewModel.onSortOrderSelected(SortOrder.BY_DATE)
                true
            }
            R.id.action_notes_hide_not_important -> {
                item.isChecked = !item.isChecked
                viewModel.onHideNotImportantClicked(item.isChecked)
                true
            }
            R.id.action_notes_hide_without_reminders -> {
                item.isChecked = !item.isChecked
                viewModel.onHideNotesWithoutReminders(item.isChecked)
                true
            }
            R.id.action_notes_cancel_all_reminders -> {
                viewModel.onCancelAllRemindersClicked()
                true
            }
            R.id.action_tasks_delete_all_completed_tasks -> {
                viewModel.onDeleteAllNotesClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchView.setOnQueryTextListener(null)
    }
}