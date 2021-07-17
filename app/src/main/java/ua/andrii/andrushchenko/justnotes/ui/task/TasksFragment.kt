package ua.andrii.andrushchenko.justnotes.ui.task

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.databinding.FragmentTasksBinding
import ua.andrii.andrushchenko.justnotes.domain.Task
import ua.andrii.andrushchenko.justnotes.ui.base.BaseFragment
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import ua.andrii.andrushchenko.justnotes.utils.onQueryTextChanged

@AndroidEntryPoint
class TasksFragment : BaseFragment<FragmentTasksBinding>(FragmentTasksBinding::inflate) {

    private val viewModel: TasksViewModel by viewModels()

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

        with(binding) {
            recyclerView.apply {
                adapter = tasksAdapter
                layoutManager = LinearLayoutManager(requireContext())
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

            todoListTitleInputLayout.editText?.setText(viewModel.todoList?.title)

            btnDone.setOnClickListener {
                viewModel.onChangeTodoListTitleClicked(
                    todoListTitleInputLayout.editText?.text.toString()
                )
            }
        }

        setFragmentResultListener("add_edit_request") { _, bundle ->
            val result = bundle.getInt("add_edit_result")
            viewModel.onAddEditResult(result)
        }

        viewModel.tasks.observe(viewLifecycleOwner) {
            tasksAdapter.submitList(it)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.tasksEvent.collect { event ->
                when (event) {
                    is TasksViewModel.TasksEvent.ShowUndoDeleteTaskMessage -> {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.task_deleted),
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(getString(R.string.undo)) {
                                viewModel.onUndoDeleteClicked(event.task)
                            }.show()
                    }
                    is TasksViewModel.TasksEvent.NavigateToAddTaskScreen -> {
                        val direction =
                            TasksFragmentDirections.actionTasksFragmentToAddEditTaskDialog(
                                null,
                                "New Task",
                                viewModel.todoList!!.id
                            )
                        findNavController().navigate(direction)
                    }
                    is TasksViewModel.TasksEvent.NavigateToEditTaskScreen -> {
                        val direction =
                            TasksFragmentDirections.actionTasksFragmentToAddEditTaskDialog(
                                event.task,
                                "Edit Task",
                                viewModel.todoList!!.id
                            )
                        findNavController().navigate(direction)
                    }
                    is TasksViewModel.TasksEvent.ShowTaskSavedConfirmationMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                    is TasksViewModel.TasksEvent.NavigateToDeleteAllCompletedInTodoListScreen -> {
                        val direction =
                            TasksFragmentDirections.actionGlobalDeleteAllCompletedDialogFragment(viewModel.todoList!!.id)
                        findNavController().navigate(direction)
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_tasks, menu)

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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
                viewModel.onHideCompletedClick(item.isChecked)
                true
            }
            R.id.action_tasks_delete_all_completed_tasks -> {
                viewModel.onDeleteAllCompletedInTodoListClicked()
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