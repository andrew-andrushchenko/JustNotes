package ua.andrii.andrushchenko.justnotes.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ua.andrii.andrushchenko.justnotes.utils.ReminderHelper
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemindersModule {

    @Provides
    @Singleton
    fun provideReminderHelper(@ApplicationContext context: Context): ReminderHelper =
        ReminderHelper(context)

}