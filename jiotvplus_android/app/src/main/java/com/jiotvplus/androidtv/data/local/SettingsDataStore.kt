package com.jiotvplus.androidtv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jiotv_prefs")

class SettingsDataStore @Inject constructor(private val context: Context) {
    companion object {
        val SSO_TOKEN = stringPreferencesKey("sso_token")
        val J_TOKEN = stringPreferencesKey("j_token")
        val LB_COOKIE = stringPreferencesKey("lb_cookie")
        val SUBSCRIBER_ID = stringPreferencesKey("subscriber_id")
        val UNIQUE_ID = stringPreferencesKey("unique_id")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val MOBILE_NUMBER = stringPreferencesKey("mobile_number")
        
        val AUTO_PLAY = booleanPreferencesKey("auto_play")
        val AUTO_START = booleanPreferencesKey("auto_start")
        val LAST_CHANNEL_ID = stringPreferencesKey("last_channel_id")
    }

    val ssoToken: Flow<String?> = context.dataStore.data.map { it[SSO_TOKEN] }
    val jToken: Flow<String?> = context.dataStore.data.map { it[J_TOKEN] }
    val lbCookie: Flow<String?> = context.dataStore.data.map { it[LB_COOKIE] }
    val subscriberId: Flow<String?> = context.dataStore.data.map { it[SUBSCRIBER_ID] }
    val uniqueId: Flow<String?> = context.dataStore.data.map { it[UNIQUE_ID] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val mobileNumber: Flow<String?> = context.dataStore.data.map { it[MOBILE_NUMBER] }

    val autoPlay: Flow<Boolean> = context.dataStore.data.map { it[AUTO_PLAY] ?: false }
    val autoStart: Flow<Boolean> = context.dataStore.data.map { it[AUTO_START] ?: false }
    val lastChannelId: Flow<String?> = context.dataStore.data.map { it[LAST_CHANNEL_ID] }

    suspend fun saveCredentials(
        ssoToken: String?, jToken: String?, lbCookie: String?,
        subscriberId: String?, uniqueId: String?, accessToken: String?, mobileNumber: String?
    ) {
        context.dataStore.edit { prefs ->
            ssoToken?.let { prefs[SSO_TOKEN] = it }
            jToken?.let { prefs[J_TOKEN] = it }
            lbCookie?.let { prefs[LB_COOKIE] = it }
            subscriberId?.let { prefs[SUBSCRIBER_ID] = it }
            uniqueId?.let { prefs[UNIQUE_ID] = it }
            accessToken?.let { prefs[ACCESS_TOKEN] = it }
            mobileNumber?.let { prefs[MOBILE_NUMBER] = it }
        }
    }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = token
        }
    }

    suspend fun saveAutoPlay(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[AUTO_PLAY] = enabled }
    }

    suspend fun saveAutoStart(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[AUTO_START] = enabled }
    }

    suspend fun saveLastChannelId(id: String) {
        context.dataStore.edit { prefs -> prefs[LAST_CHANNEL_ID] = id }
    }
}
