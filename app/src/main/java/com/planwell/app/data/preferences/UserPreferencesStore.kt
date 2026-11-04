package com.planwell.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "plan_well_user_prefs",
)

@Singleton
class UserPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val fetchLinkPreviews: Flow<Boolean> = context.userDataStore.data.map { prefs ->
        prefs[KEY_FETCH_LINK_PREVIEWS] ?: true
    }

    suspend fun setFetchLinkPreviews(enabled: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_FETCH_LINK_PREVIEWS] = enabled
        }
    }

    suspend fun fetchLinkPreviewsOnce(): Boolean = fetchLinkPreviews.first()

    companion object {
        private val KEY_FETCH_LINK_PREVIEWS = booleanPreferencesKey("fetch_link_previews")
    }
}
