package ua.andrii.andrushchenko.justnotes.di

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ua.andrii.andrushchenko.justnotes.data.JustNotesDatabase
import ua.andrii.andrushchenko.justnotes.data.note.NoteDao
import ua.andrii.andrushchenko.justnotes.data.task.TaskDao
import ua.andrii.andrushchenko.justnotes.data.todolist.TodoListDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJustNotesDatabase(application: Application): JustNotesDatabase =
        Room.databaseBuilder(application, JustNotesDatabase::class.java, "just_notes_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNoteDao(db: JustNotesDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideTodoListDao(db: JustNotesDatabase): TodoListDao = db.todoListDao()

    @Provides
    fun provideTaskDao(db: JustNotesDatabase): TaskDao = db.taskDao()

}