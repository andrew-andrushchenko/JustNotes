package ua.andrii.andrushchenko.justnotes.ui.todolist.addedittodolist

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.databinding.FragmentAddEditTodoListBinding
import ua.andrii.andrushchenko.justnotes.domain.Task
import ua.andrii.andrushchenko.justnotes.ui.base.BaseFragment
import ua.andrii.andrushchenko.justnotes.ui.task.TasksAdapter
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_TASK_REQUEST
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_TASK_RESULT
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_TODO_LIST_REQUEST
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_TODO_LIST_RESULT
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import ua.andrii.andrushchenko.justnotes.utils.onQueryTextChanged
import ua.andrii.andrushchenko.justnotes.utils.setOnTextChangedListener
import ua.andrii.andrushchenko.justnotes.utils.setupLinearLayoutManager

@AndroidEntryPoint
class AddEditTodoListFragment :
    BaseFragment<FragmentAddEditTodoListBinding>(FragmentAddEditTodoListBinding::inflate) {

    private val viewModel: AddEditTodoListViewModel by viewModels()

    private lateinit var searchView: SearchView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tasksAdapter = TasksAdapter(object : TasksAdapter.OnItemClickListener {
            override fun onItemClick(task: Task) {
                viewModel.onTaskSelected(task)
            }

            override fun onCheckBoxClick(task: Task, isChecked: Boolean) {
                viewModel.onTaskCheckedChanged(task, isChecked)
            }
        })

        setupToolbar()
        setupOnSystemNavigationBackPressed()

        with(binding) {
            todoListTitleEdittext.apply {
                setText(viewModel.newTodoListTitle)
                setOnTextChangedListener {
                    viewModel.newTodoListTitle = it
                }
            }

            recyclerView.apply {
                adapter = tasksAdapter
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
                    val task = tasksAdapter.currentList[viewHolder.adapterPosition]
                    viewModel.onTaskSwiped(task)
                }
            }).attachToRecyclerView(recyclerView)

            fabAddTask.setOnClickListener {
                viewModel.onAddNewTaskClicked()
            }
        }

        setFragmentResultListener(ADD_EDIT_TASK_REQUEST) { _, bundle ->
            val result = bundle.getInt(ADD_EDIT_TASK_RESULT)
            viewModel.onAddEditTaskResult(result)
        }

        viewModel.tasks.observe(viewLifecycleOwner) {
            tasksAdapter.submitList(it)
            toggleTextViewEmpty(it.isEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.tasksEvent.collect { event ->
                when (event) {
                    is AddEditTodoListViewModel.AddEditTodoListEvent.ShowUndoDeleteTaskMessage -> {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.task_deleted),
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(getString(R.string.undo)) {
                                viewModel.onUndoDeleteClicked(event.task)
                            }.show()
                    }
                    is AddEditTodoListViewModel.AddEditTodoListEvent.NavigateToAddTaskScreen -> {
                        val direction =
                            AddEditTodoListFragmentDirections.actionAddEditTodoListFragmentToAddEditTaskDialog(
                                task = null,
                                title = getString(R.string.add_task),
                                todoListId = viewModel.todoList!!.id
                            )
                        findNavController().navigate(direction)
                    }
                    is AddEditTodoListViewModel.AddEditTodoListEvent.NavigateToEditTaskScreen -> {
                        val direction =
                            AddEditTodoListFragmentDirections.actionAddEditTodoListFragmentToAddEditTaskDialog(
                                task = event.task,
                                title = getString(R.string.edit_task),
                                todoListId = viewModel.todoList!!.id
                            )
                        findNavController().navigate(direction)
                    }
                    is AddEditTodoListViewModel.AddEditTodoListEvent.ShowTaskSavedConfirmationMessage -> {
                        Snackbar.make(requireView(), getString(event.msg), Snackbar.LENGTH_SHORT)
                            .show()
                    }
                    is AddEditTodoListViewModel.AddEditTodoListEvent.NavigateToDeleteAllCompletedInTodoListScreen -> {
                        val direction =
                            AddEditTodoListFragmentDirections.actionAddEditTodoListFragmentToDeleteAllCompletedDialogFragment(
                                viewModel.todoList!!.id
                            )
                        findNavController().navigate(direction)
                    }
                    is AddEditTodoListViewModel.AddEditTodoListEvent.NavigateBackWithResult -> {
                        setFragmentResult(
                            ADD_EDIT_TODO_LIST_REQUEST,
                            bundleOf(ADD_EDIT_TODO_LIST_RESULT to event.result)
                        )
                        findNavController().popBackStack()
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
            inflateMenu(R.menu.menu_add_edit_todo_list)

            // Setup menu
            val searchItem = menu.findItem(R.id.action_tasks_search)
            searchView = searchItem.actionView as SearchView

            val pendingQuery = viewModel.tasksSearchQuery.value
            if (pendingQuery != null && pendingQuery.isNotEmpty()) {
                searchItem.expandActionView()
                searchView.setQuery(pendingQuery, false)
            }

            searchView.onQueryTextChanged {
                viewModel.tasksSearchQuery.value = it
            }

            viewLifecycleOwner.lifecycleScope.launch {
                menu.findItem(R.id.action_tasks_hide_completed_tasks).isChecked =
                    viewModel.preferencesFlow.first().hideCompleted
            }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_tasks_sort_by_name -> {
                        viewModel.onSortOrderSelected(SortOrder.BY_NAME)
                        true
                    }
                    R.id.action_tasks_sort_by_date_created -> {
                        viewModel.onSortOrderSelected(SortOrder.BY_DATE)
                        true
                    }
                    R.id.action_tasks_hide_completed_tasks -> {
                        item.isChecked = !item.isChecked
                        viewModel.onHideCompletedClicked(item.isChecked)
                        true
                    }
                    R.id.action_tasks_delete_all_completed_tasks -> {
                        viewModel.onDeleteAllCompletedInTodoListClicked()
                        true
                    }
                    else -> super.onOptionsItemSelected(item)
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
            setNavigationOnClickListener {
                viewModel.saveTodoListAndNavigateBack()
            }
        }
    }

    private fun setupOnSystemNavigationBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.saveTodoListAndNavigateBack()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchView.setOnQueryTextListener(null)
    }
}