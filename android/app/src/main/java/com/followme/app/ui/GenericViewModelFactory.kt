package com.followme.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Tiny ViewModelProvider.Factory for hand-rolled DI: just supplies a lambda that constructs the ViewModel with whatever dependencies it needs from AppContainer. */
class GenericViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
