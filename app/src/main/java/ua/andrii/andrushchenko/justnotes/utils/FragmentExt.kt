package ua.andrii.andrushchenko.justnotes.utils

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

fun <T> Fragment.setNavigationResult(key: String = "result", result: T) =
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)

fun <T> Fragment.getNavigationResult(key: String = "result") =
    findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)