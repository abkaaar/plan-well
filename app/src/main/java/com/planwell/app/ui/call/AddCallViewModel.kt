package com.planwell.app.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planwell.app.domain.model.Contact
import com.planwell.app.domain.model.ScheduledCall
import com.planwell.app.domain.repository.ContactsRepository
import com.planwell.app.domain.usecase.call.ScheduleCallUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddCallUiState(
    val contactQuery: String = "",
    val contacts: List<Contact> = emptyList(),
    val contactsPermissionGranted: Boolean = false,
    val selectedContactId: String? = null,
    val displayName: String = "",
    val phoneNumber: String = "",
    val scheduledAtMs: Long? = null,
    val notes: String = "",
    val isSearching: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class AddCallViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val scheduleCall: ScheduleCallUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCallUiState())
    val uiState: StateFlow<AddCallUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onContactsPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                contactsPermissionGranted = granted,
                contacts = if (granted) it.contacts else emptyList(),
            )
        }
        if (granted) {
            searchContacts(_uiState.value.contactQuery)
        }
    }

    fun onContactQueryChange(query: String) {
        _uiState.update { it.copy(contactQuery = query, error = null) }
        if (_uiState.value.contactsPermissionGranted) {
            searchContacts(query)
        }
    }

    fun onContactSelected(contact: Contact) {
        _uiState.update {
            it.copy(
                selectedContactId = contact.id,
                displayName = contact.displayName,
                phoneNumber = contact.phoneNumber,
                contactQuery = contact.displayName,
                contacts = emptyList(),
                error = null,
            )
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update {
            it.copy(
                displayName = value,
                selectedContactId = null,
                error = null,
            )
        }
    }

    fun onPhoneNumberChange(value: String) {
        _uiState.update {
            it.copy(
                phoneNumber = value,
                selectedContactId = null,
                error = null,
            )
        }
    }

    fun onScheduledAtChange(ms: Long) {
        _uiState.update { it.copy(scheduledAtMs = ms, error = null) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun save() {
        val state = _uiState.value
        val phone = state.phoneNumber.trim()
        val name = state.displayName.trim().ifBlank { phone }
        val whenMs = state.scheduledAtMs
        if (phone.isBlank()) {
            _uiState.update { it.copy(error = "Enter a phone number") }
            return
        }
        if (whenMs == null) {
            _uiState.update { it.copy(error = "Pick a date and time") }
            return
        }
        if (whenMs <= System.currentTimeMillis()) {
            _uiState.update { it.copy(error = "Pick a time in the future") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            runCatching {
                scheduleCall(
                    ScheduledCall(
                        contactId = state.selectedContactId,
                        displayName = name,
                        phoneNumber = phone,
                        scheduledAtMs = whenMs,
                        notes = state.notes.trim().ifBlank { null },
                    ),
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Could not schedule call",
                    )
                }
            }
        }
    }

    private fun searchContacts(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(SEARCH_DEBOUNCE_MS)
            val results = runCatching { contactsRepository.search(query) }.getOrDefault(emptyList())
            _uiState.update { it.copy(contacts = results, isSearching = false) }
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
