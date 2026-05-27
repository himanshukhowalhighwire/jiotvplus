package com.himanshutv.apk.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

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
        
        val REPLAY_LAST_CHANNEL = booleanPreferencesKey("replay_last_channel")
        val LAST_PLAYED_CHANNEL_ID = stringPreferencesKey("last_played_channel_id")
    }

    val mobileNumber: Flow<String?> = context.dataStore.data.map { it[MOBILE_NUMBER] }
    val ssoToken: Flow<String?> = context.dataStore.data.map { it[SSO_TOKEN] }
    val jToken: Flow<String?> = context.dataStore.data.map { it[J_TOKEN] }
    val lbCookie: Flow<String?> = context.dataStore.data.map { it[LB_COOKIE] }
    val subscriberId: Flow<String?> = context.dataStore.data.map { it[SUBSCRIBER_ID] }
    val uniqueId: Flow<String?> = context.dataStore.data.map { it[UNIQUE_ID] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }

    val replayLastChannel: Flow<Boolean> = context.dataStore.data.map { it[REPLAY_LAST_CHANNEL] ?: false }
    val lastPlayedChannelId: Flow<String?> = context.dataStore.data.map { it[LAST_PLAYED_CHANNEL_ID] }

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
        // Write external backup
        backupCredentials(
            mobile = mobile,
            sso = sso,
            jTokenVal = jTokenVal,
            lbCookieVal = lbCookieVal,
            subId = subId,
            unique = unique,
            access = access,
            refresh = refresh
        )
    }

    suspend fun setReplayLastChannel(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[REPLAY_LAST_CHANNEL] = enabled
        }
    }

    suspend fun saveLastPlayedChannel(channelId: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_PLAYED_CHANNEL_ID] = channelId
        }
    }

    suspend fun saveChannelLanguage(channelId: String, langCode: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("lang_$channelId")] = langCode
        }
    }

    suspend fun getChannelLanguage(channelId: String): String? {
        return context.dataStore.data.map { it[stringPreferencesKey("lang_$channelId")] }.firstOrNull()
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
        try {
            val file = getBackupFile()
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getBackupFile(): java.io.File {
        val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return java.io.File(downloadDir, "himanshutv_credentials.json")
    }

    private fun backupCredentials(
        mobile: String,
        sso: String?,
        jTokenVal: String?,
        lbCookieVal: String?,
        subId: String?,
        unique: String?,
        access: String?,
        refresh: String?
    ) {
        try {
            val file = getBackupFile()
            val json = org.json.JSONObject().apply {
                put("mobile", mobile)
                put("sso", sso ?: "")
                put("jToken", jTokenVal ?: "")
                put("lbCookie", lbCookieVal ?: "")
                put("subscriberId", subId ?: "")
                put("uniqueId", unique ?: "")
                put("accessToken", access ?: "")
                put("refreshToken", refresh ?: "")
            }
            file.writeText(json.toString(), Charsets.UTF_8)
            android.util.Log.d("BACKUP", "Credentials backed up to ${file.absolutePath}")
        } catch (t: Throwable) {
            android.util.Log.e("BACKUP", "Failed to back up credentials", t)
        }
    }

    fun tryRestoreCredentials(): Boolean {
        try {
            val file = getBackupFile()
            if (file.exists()) {
                val text = file.readText(Charsets.UTF_8)
                val json = org.json.JSONObject(text)
                val mobile = json.optString("mobile")
                val sso = json.optString("sso").takeIf { it.isNotEmpty() }
                val jTokenVal = json.optString("jToken").takeIf { it.isNotEmpty() }
                val lbCookieVal = json.optString("lbCookie").takeIf { it.isNotEmpty() }
                val subId = json.optString("subscriberId").takeIf { it.isNotEmpty() }
                val unique = json.optString("uniqueId").takeIf { it.isNotEmpty() }
                val access = json.optString("accessToken").takeIf { it.isNotEmpty() }
                val refresh = json.optString("refreshToken").takeIf { it.isNotEmpty() }

                if (mobile.isNotEmpty()) {
                    runBlocking {
                        // Temporarily bypass backup write during restore to avoid loop
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
                    android.util.Log.d("BACKUP", "Credentials successfully restored from ${file.absolutePath}")
                    return true
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("BACKUP", "Failed to restore credentials", t)
        }
        return false
    }
}
