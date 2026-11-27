package com.alveon.plus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alveon.plus.AppGraph
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val store = AppGraph.store

    val progress = store.progress
    val favorites = store.favorites
    val servers = store.servers
    val locations = store.locations
    val library = AppGraph.library.items

    private var job: Job? = null

    /** Rescans folders / servers in the background; posters fill in as TMDB answers. */
    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch { AppGraph.library.refresh(AppGraph.appContext) }
    }
}
