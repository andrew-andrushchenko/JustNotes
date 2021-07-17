package ua.andrii.andrushchenko.justnotes.data

import androidx.room.Database
import androidx.room.RoomDatabase
import ua.andrii.andrushchenko.justnotes.data.note.NoteDao
import ua.andrii.andrushchenko.justnotes.data.task.TaskDao
import ua.andrii.andrushchenko.justnotes.data.todolist.TodoListDao
import ua.andrii.andrushchenko.justnotes.domain.Note
import ua.andrii.andrushchenko.justnotes.domain.Task
import ua.andrii.andrushchenko.justnotes.domain.TodoList

@Database(entities = [Note::class, TodoList::class, Task::class], version = 1)
abstract class JustNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun todoListDao(): TodoListDao
    abstract fun taskDao(): TaskDao
}