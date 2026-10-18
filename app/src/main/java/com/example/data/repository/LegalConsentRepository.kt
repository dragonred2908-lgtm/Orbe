package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "legal_consent_prefs")

data class LegalConsentState(
    val isAccepted: Boolean = false,
    val timestamp: Long = 0L,
    val termsVersion: String = LegalConsentRepository.CURRENT_TERMS_VERSION
)

class LegalConsentRepository(private val context: Context) {

    private object PreferencesKeys {
        val KEY_ACCEPTED_TERMS_V1 = booleanPreferencesKey("accepted_terms_v1")
        val KEY_CONSENT_TIMESTAMP = longPreferencesKey("consent_timestamp")
        val KEY_TERMS_VERSION = stringPreferencesKey("terms_version")
    }

    val consentStateFlow: Flow<LegalConsentState> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isAccepted = preferences[PreferencesKeys.KEY_ACCEPTED_TERMS_V1] ?: false
            val timestamp = preferences[PreferencesKeys.KEY_CONSENT_TIMESTAMP] ?: 0L
            val version = preferences[PreferencesKeys.KEY_TERMS_VERSION] ?: CURRENT_TERMS_VERSION
            LegalConsentState(
                isAccepted = isAccepted,
                timestamp = timestamp,
                termsVersion = version
            )
        }

    val isTermsAcceptedFlow: Flow<Boolean> = consentStateFlow.map { it.isAccepted }

    suspend fun isTermsAccepted(): Boolean {
        return consentStateFlow.first().isAccepted
    }

    suspend fun recordConsent(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_ACCEPTED_TERMS_V1] = accepted
            preferences[PreferencesKeys.KEY_CONSENT_TIMESTAMP] = if (accepted) System.currentTimeMillis() else 0L
            preferences[PreferencesKeys.KEY_TERMS_VERSION] = CURRENT_TERMS_VERSION
        }
    }

    suspend fun revokeConsent() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_ACCEPTED_TERMS_V1] = false
            preferences[PreferencesKeys.KEY_CONSENT_TIMESTAMP] = 0L
        }
    }

    suspend fun clearAllUserData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        const val CURRENT_TERMS_VERSION = "1.0.0"
        const val TERMS_TITLE = "Términos y Condiciones de Uso v1.0"
        const val PRIVACY_TITLE = "Política de Privacidad y Metadatos"
        const val DMCA_TITLE = "Procedimiento DMCA de Retirada"
    }
}
