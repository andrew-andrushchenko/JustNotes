package ua.andrii.andrushchenko.justnotes.utils

import android.app.Activity

object Constants {

    const val EXTRA_ID = "ua.andrii.andrushchenko.justnotes.EXTRA_ID"

    const val EXTRA_TITLE = "ua.andrii.andrushchenko.justnotes.EXTRA_TITLE"

    const val EXTRA_CONTENT = "ua.andrii.andrushchenko.justnotes.EXTRA_CONTENT"

    const val ADD_RESULT_OK = Activity.RESULT_FIRST_USER

    const val ADD_RESULT_FAIL = Activity.RESULT_FIRST_USER + 1

    const val EDIT_RESULT_OK = Activity.RESULT_FIRST_USER + 2

    const val ADD_EDIT_TASK_REQUEST = "ADD_EDIT_TASK_REQUEST"

    const val ADD_EDIT_TASK_RESULT = "ADD_EDIT_TASK_RESULT"

    const val ADD_EDIT_TODO_LIST_REQUEST = "ADD_EDIT_TODO_LIST_REQUEST"

    const val ADD_EDIT_TODO_LIST_RESULT = "ADD_EDIT_TODO_LIST_RESULT"

    const val ADD_EDIT_NOTE_REQUEST = "ADD_EDIT_NOTE_REQUEST"

    const val ADD_EDIT_NOTE_RESULT = "ADD_EDIT_NOTE_RESULT"

}