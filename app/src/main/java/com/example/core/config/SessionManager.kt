package com.example.core.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.models.User
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tariki_session")

object SessionManager {
    private lateinit var dataStore: DataStore<Preferences>
    private val KEY_USER_ID   = stringPreferencesKey("user_id")
    private val KEY_USER_TYPE = stringPreferencesKey("user_type")
    private val KEY_PHONE     = stringPreferencesKey("phone")
    private val KEY_NAME      = stringPreferencesKey("user_name")
    private val KEY_AVATAR    = stringPreferencesKey("user_avatar")

    fun init(context: Context) {
        dataStore = context.dataStore
    }

    suspend fun saveUser(user: User) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID]   = user.id
            prefs[KEY_USER_TYPE] = user.userType
            prefs[KEY_PHONE]     = user.phone
            prefs[KEY_NAME]      = user.fullName ?: ""
            prefs[KEY_AVATAR]    = user.profilePhotoUrl ?: ""
        }
    }

    suspend fun getUser(): Triple<String, String, String>? {
        val prefs = dataStore.data.first()
        val id    = prefs[KEY_USER_ID]   ?: return null
        val type  = prefs[KEY_USER_TYPE] ?: return null
        val phone = prefs[KEY_PHONE]     ?: return null
        return Triple(id, type, phone)
    }

    suspend fun getUserDetails(): Map<String, String> {
        val prefs = dataStore.data.first()
        return mapOf(
            "id" to (prefs[KEY_USER_ID] ?: ""),
            "type" to (prefs[KEY_USER_TYPE] ?: ""),
            "phone" to (prefs[KEY_PHONE] ?: ""),
            "name" to (prefs[KEY_NAME] ?: ""),
            "avatar" to (prefs[KEY_AVATAR] ?: "")
        )
    }

    suspend fun clear() = dataStore.edit { it.clear() }
}
