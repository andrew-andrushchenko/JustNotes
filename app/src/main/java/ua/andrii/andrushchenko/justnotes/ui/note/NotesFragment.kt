package ua.andrii.andrushchenko.justnotes.ui.note

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.databinding.FragmentNotesBinding
import ua.andrii.andrushchenko.justnotes.ui.base.BaseFragment
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_NOTE_REQUEST
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_NOTE_RESULT
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import ua.andrii.andrushchenko.justnotes.utils.onQueryTextChanged
import ua.andrii.andrushchenko.justnotes.utils.setupLinearLayoutManager

@AndroidEntryPoint
class NotesFragment : BaseFragment<FragmentNotesBinding>(FragmentNotesBinding::inflate) {

    private val viewModel: NotesViewModel by viewModels()

    private lateinit var searchView: SearchView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val notesAdapter = NotesAdapter { note ->
            viewModel.onNoteSelected(note)
        }

        setupToolbar()

        with(binding) {
            recyclerView.apply {
                adapter = notesAdapter
                setupLinearLayoutManager(resources.getDimensionPixelSize(R.dimen.indent_8dp))
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

        setFragmentResultListener(ADD_EDIT_NOTE_REQUEST) { _, bundle ->
            val result = bundle.getInt(ADD_EDIT_NOTE_RESULT)
            viewModel.onAddEditResult(result)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notes.collect {
                    notesAdapter.submitList(it)
                    toggleTextViewEmpty(it.isEmpty())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                            ).setAction(getString(R.string.undo)) {
                                viewModel.onUndoDeleteClicked(event.note)
                            }.show()
                        }
                        is NotesViewModel.NoteEvent.ShowNoteSavedConfirmationMessage -> {
                            Snackbar.make(requireView(), getString(event.msg), Snackbar.LENGTH_SHORT)
                                .show()
                        }
                        is NotesViewModel.NoteEvent.NavigateToCancelAllReminders -> {
                            val direction =
                                NotesFragmentDirections.actionNotesFragmentToCancelAllRemindersDialogFragment()
                            findNavController().navigate(direction)
                        }
                    }
                }
            }
        }
    }

    private fun toggleTextViewEmpty(isVisible: Boolean) {
        with(binding) {
            textViewEmpty.visibility = if (isVisible) View.VISIBLE else View.GONE
            recyclerView.visibility = if (!isVisible) View.VISIBLE else View.GONE
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            inflateMenu(R.menu.menu_notes)

            // Setup menu
            val searchItem = menu.findItem(R.id.action_notes_search)
            searchView = searchItem.actionView as SearchView

            val pendingQuery = viewModel.searchQuery.value
            if (pendingQuery.isNotEmpty()) {
                searchItem.expandActionView()
                searchView.setQuery(pendingQuery, false)
            }

            searchView.onQueryTextChanged {
                viewModel.onQueryTextChanged(it)
            }

            viewLifecycleOwner.lifecycleScope.launch {
                menu.findItem(R.id.action_notes_hide_not_important).isChecked =
                    viewModel.preferencesFlow.first().hideNotImportant
            }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
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

            // Finish setup the toolbar
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

    override fun onDestroyView() {
        super.onDestroyView()
        searchView.setOnQueryTextListener(null)
    }
}