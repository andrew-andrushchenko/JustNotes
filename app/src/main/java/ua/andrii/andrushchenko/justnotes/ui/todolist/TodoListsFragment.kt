package ua.andrii.andrushchenko.justnotes.ui.todolist

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
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.databinding.FragmentTodoListsBinding
import ua.andrii.andrushchenko.justnotes.domain.TodoList
import ua.andrii.andrushchenko.justnotes.ui.base.BaseFragment
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import ua.andrii.andrushchenko.justnotes.utils.onQueryTextChanged

@AndroidEntryPoint
class TodoListsFragment :
    BaseFragment<FragmentTodoListsBinding>(FragmentTodoListsBinding::inflate) {

    private val viewModel: TodoListsViewModel by viewModels()

    private lateinit var searchView: SearchView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val todoListsAdapter = TodoListsAdapter(object : TodoListsAdapter.OnItemClickListener {
            override fun onItemClick(todoList: TodoList) {
                viewModel.onTodoListSelected(todoList)
            }
        })

        with(binding) {
            recyclerView.apply {
                adapter = todoListsAdapter
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
                    val todoList = todoListsAdapter.currentList[viewHolder.adapterPosition]
                    viewModel.onTodoListSwiped(todoList)
                }
            }).attachToRecyclerView(recyclerView)

            fabAddTodoList.setOnClickListener {
                viewModel.onAddNewTodoListClicked()
            }
        }

        setFragmentResultListener("add_todo_list_request") { _, bundle ->
            val result = bundle.getInt("add_todo_list_result")
            viewModel.onAddResult(result)
        }

        viewModel.todoLists.observe(viewLifecycleOwner) {
            todoListsAdapter.submitList(it)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.todoListsEvent.collect { event ->
                when (event) {
                    is TodoListsViewModel.TodoListsEvent.NavigateToCreateTodoListScreen -> {
                        val direction =
                            TodoListsFragmentDirections.actionTodoListsFragmentToAddTodoListDialog()
                        findNavController().navigate(direction)
                    }
                    is TodoListsViewModel.TodoListsEvent.NavigateToTasksScreen -> {
                        val direction =
                            TodoListsFragmentDirections.actionTodoListsFragmentToTasksFragment(event.todoList)
                        findNavController().navigate(direction)
                    }
                    is TodoListsViewModel.TodoListsEvent.ShowUndoDeleteTaskMessage -> {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.todo_list_deleted),
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(getString(R.string.undo)) {
                                viewModel.onUndoDeleteClicked(event.todoList, event.tasks)
                            }.show()
                    }
                    is TodoListsViewModel.TodoListsEvent.ShowTodoListSavedConfirmationMessage -> {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.todo_list_created),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_todo_lists, menu)

        val searchItem = menu.findItem(R.id.action_todo_lists_search)
        searchView = searchItem.actionView as SearchView

        val pendingQuery = viewModel.todoListsSearchQuery.value
        if (pendingQuery != null && pendingQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(pendingQuery, false)
        }

        searchView.onQueryTextChanged {
            viewModel.todoListsSearchQuery.value = it
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_todo_lists_sort_by_name -> {
                viewModel.onSortOrderSelected(SortOrder.BY_NAME)
                true
            }
            R.id.action_todo_lists_sort_by_date_created -> {
                viewModel.onSortOrderSelected(SortOrder.BY_DATE)
                true
            }
            R.id.action_delete_all_todo_lists -> {
                viewModel.onDeleteAllClicked()
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