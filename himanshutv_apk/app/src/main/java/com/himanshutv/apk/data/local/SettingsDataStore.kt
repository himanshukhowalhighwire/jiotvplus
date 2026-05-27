package com.himanshutv.apk.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val MOBILE_NUMBER = stringPreferencesKey("mobile_number")
        val SSO_TOKEN = stringPreferencesKey("sso_token")
        val J_TOKEN = stringPreferencesKey("j_token")
        val LB_COOKIE = stringPreferencesKey("lb_cookie")
        val SUBSCRIBER_ID = stringPreferencesKey("subscriber_id")
        val UNIQUE_ID = stringPreferencesKey("unique_id")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val mobileNumber: Flow<String?> = context.dataStore.data.map { it[MOBILE_NUMBER] }
    val ssoToken: Flow<String?> = context.dataStore.data.map { it[SSO_TOKEN] }
    val jToken: Flow<String?> = context.dataStore.data.map { it[J_TOKEN] }
    val lbCookie: Flow<String?> = context.dataStore.data.map { it[LB_COOKIE] }
    val subscriberId: Flow<String?> = context.dataStore.data.map { it[SUBSCRIBER_ID] }
    val uniqueId: Flow<String?> = context.dataStore.data.map { it[UNIQUE_ID] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }

    suspend fun saveAuthData(
        mobile: String,
        sso: String?,
        jTokenVal: String?,
        lbCookieVal: String?,
        subId: String?,
        unique: String?,
        access: String?,
        refresh: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[MOBILE_NUMBER] = mobile
            sso?.let { prefs[SSO_TOKEN] = it }
            jTokenVal?.let { prefs[J_TOKEN] = it }
            lbCookieVal?.let { prefs[LB_COOKIE] = it }
            subId?.let { prefs[SUBSCRIBER_ID] = it }
            unique?.let { prefs[UNIQUE_ID] = it }
            access?.let { prefs[ACCESS_TOKEN] = it }
            refresh?.let { prefs[REFRESH_TOKEN] = it }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
