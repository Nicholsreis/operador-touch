package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SessionManager(private val context: Context) {
    companion object {
        val SERVER_IP = stringPreferencesKey("server_ip")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val OPERADOR_ID = intPreferencesKey("operador_id")
        val OPERADOR_NOME = stringPreferencesKey("operador_nome")
        val GUICHE_ATIVO = stringPreferencesKey("guiche_ativo")
        
        // Premium Settings keys
        val APP_THEME = stringPreferencesKey("app_theme")
        val AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val PAUSA_ENABLED = booleanPreferencesKey("pausa_enabled")
    }

    val serverIp: Flow<String> = context.dataStore.data.map { it[SERVER_IP] ?: "http://192.168.1.100:3000" }
    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    val operadorId: Flow<Int?> = context.dataStore.data.map { it[OPERADOR_ID] }
    val operadorNome: Flow<String?> = context.dataStore.data.map { it[OPERADOR_NOME] }
    val guicheAtivo: Flow<String> = context.dataStore.data.map { it[GUICHE_ATIVO] ?: "Guichê 1" }

    // Read premium preferences, falling back to logical defaults
    val appTheme: Flow<String> = context.dataStore.data.map { it[APP_THEME] ?: "system" }
    val audioEnabled: Flow<Boolean> = context.dataStore.data.map { it[AUDIO_ENABLED] ?: true }
    val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { it[HAPTIC_ENABLED] ?: true }
    val pausaEnabled: Flow<Boolean> = context.dataStore.data.map { it[PAUSA_ENABLED] ?: false }

    suspend fun saveServerIp(ip: String) {
        context.dataStore.edit { it[SERVER_IP] = ip }
    }

    suspend fun saveSession(token: String, id: Int, nome: String) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
            prefs[OPERADOR_ID] = id
            prefs[OPERADOR_NOME] = nome
        }
    }

    suspend fun saveGuiche(guiche: String) {
        context.dataStore.edit { it[GUICHE_ATIVO] = guiche }
    }

    suspend fun saveAppTheme(theme: String) {
        context.dataStore.edit { it[APP_THEME] = theme }
    }

    suspend fun saveAudioEnabled(enabled: Boolean) {
        context.dataStore.edit { it[AUDIO_ENABLED] = enabled }
    }

    suspend fun saveHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[HAPTIC_ENABLED] = enabled }
    }

    suspend fun savePausaEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PAUSA_ENABLED] = enabled }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(AUTH_TOKEN)
            prefs.remove(OPERADOR_ID)
            prefs.remove(OPERADOR_NOME)
            prefs[PAUSA_ENABLED] = false // auto-reset pause when clearing session
        }
    }
}
