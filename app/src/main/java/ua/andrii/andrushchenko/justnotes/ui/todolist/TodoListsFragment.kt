package ua.andrii.andrushchenko.justnotes.ui.todolist

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
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
import ua.andrii.andrushchenko.justnotes.R
import ua.andrii.andrushchenko.justnotes.databinding.FragmentTodoListsBinding
import ua.andrii.andrushchenko.justnotes.domain.TodoList
import ua.andrii.andrushchenko.justnotes.ui.base.BaseFragment
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_TODO_LIST_REQUEST
import ua.andrii.andrushchenko.justnotes.utils.Constants.ADD_EDIT_TODO_LIST_RESULT
import ua.andrii.andrushchenko.justnotes.utils.SortOrder
import ua.andrii.andrushchenko.justnotes.utils.onQueryTextChanged
import ua.andrii.andrushchenko.justnotes.utils.setupStaggeredGridLayoutManager

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

        setupToolbar()

        with(binding) {
            recyclerView.apply {
                adapter = todoListsAdapter
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
                    val todoList = todoListsAdapter.currentList[viewHolder.adapterPosition]
                    viewModel.onTodoListSwiped(todoList)
                }
            }).attachToRecyclerView(recyclerView)

            fabAddTodoList.setOnClickListener {
                viewModel.onAddNewTodoListClicked()
            }
        }

        setFragmentResultListener(ADD_EDIT_TODO_LIST_REQUEST) { _, bundle ->
            val result = bundle.getInt(ADD_EDIT_TODO_LIST_RESULT)
            viewModel.onAddEditResult(result)
        }

        viewModel.todoLists.observe(viewLifecycleOwner) {
            todoListsAdapter.submitList(it)
            toggleTextViewEmpty(it.isEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.todoListsEvent.collect { event ->
                when (event) {
                    is TodoListsViewModel.TodoListsEvent.NavigateToCreateTodoListScreen -> {
                        val direction =
                            TodoListsFragmentDirections.actionTodoListsFragmentToAddEditTodoListFragment(
                                todoList = event.todoList
                            )
                        findNavController().navigate(direction)
                    }
                    is TodoListsViewModel.TodoListsEvent.NavigateToEditTodoListScreen -> {
                        val direction =
                            TodoListsFragmentDirections.actionTodoListsFragmentToAddEditTodoListFragment(
                                todoList = event.todoList
                            )
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
                            event.msg,
                            Snackbar.LENGTH_LONG
                        ).show()
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
            inflateMenu(R.menu.menu_todo_lists)

            // Setup menu
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

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
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