package com.planwell.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.data.preferences.UserPreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesStore,
) : ViewModel() {

    val fetchLinkPreviews: StateFlow<Boolean> = preferences.fetchLinkPreviews
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setFetchLinkPreviews(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setFetchLinkPreviews(enabled)
        }
    }
}
