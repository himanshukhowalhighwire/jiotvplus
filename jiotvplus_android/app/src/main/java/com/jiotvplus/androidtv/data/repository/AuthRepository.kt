package com.jiotvplus.androidtv.data.repository

import com.jiotvplus.androidtv.data.local.SettingsDataStore
import com.jiotvplus.androidtv.data.model.ExchangeTokenRequest
import com.jiotvplus.androidtv.data.model.VerifyOtpRequest
import com.jiotvplus.androidtv.data.remote.AuthApi
import com.jiotvplus.androidtv.data.remote.ExchangeTokenApi
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Base64

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val exchangeTokenApi: ExchangeTokenApi,
    private val dataStore: SettingsDataStore
) {
    suspend fun sendOtp(mobileNumber: String): Boolean {
        val cleaned = mobileNumber.replace(Regex("[^0-9]"), "")
        val number = if (cleaned.length > 10 && cleaned.startsWith("91")) {
            cleaned.substring(2)
        } else if (cleaned.length > 10 && cleaned.startsWith("0")) {
            cleaned.substring(1)
        } else {
            cleaned
        }

        return try {
            val response = authApi.sendOtp(number)
            if (response.isSuccessful) {
                response.body()?.identifier?.let {
                    // Temporarily store identifier in a local variable or just return it if we needed to
                    // For simplicity, we could pass identifier back, but we'll return true.
                    // Wait, we need the identifier to verify OTP.
                    // The easiest way is to just keep it in memory for the next step, or return it.
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun sendOtpAndGetId(mobileNumber: String): String? {
        val cleaned = mobileNumber.replace(Regex("[^0-9]"), "")
        val number = if (cleaned.length > 10 && cleaned.startsWith("91")) {
            cleaned.substring(2)
        } else if (cleaned.length > 10 && cleaned.startsWith("0")) {
            cleaned.substring(1)
        } else {
            cleaned
        }
        return try {
            val response = authApi.sendOtp(number)
            if (response.isSuccessful) {
                response.body()?.identifier
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun verifyOtp(mobileNumber: String, otp: String, identifier: String): Boolean {
        return try {
            val request = VerifyOtpRequest(identifier = identifier, otp = otp)
            val response = authApi.verifyOtp(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val ssoToken = body.ssoToken ?: body.sessionAttributes?.user?.ssoToken
                    val jToken = body.jToken ?: body.sessionAttributes?.user?.jToken
                    val lbCookie = body.lbCookie ?: body.sessionAttributes?.lbCookie ?: body.sessionAttributes?.user?.lbCookie
                    val subscriberId = body.subscriberId ?: body.sessionAttributes?.user?.subscriberId
                    val uniqueId = body.sessionAttributes?.user?.unique

                    dataStore.saveCredentials(
                        ssoToken, jToken, lbCookie, subscriberId, uniqueId, null, mobileNumber
                    )
                    
                    if (ssoToken != null && subscriberId != null && uniqueId != null) {
                        exchangeToken(ssoToken, subscriberId, uniqueId, mobileNumber)
                    }
                    true
                } else false
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun exchangeToken(ssoToken: String, subscriberId: String, uniqueId: String, mobileNumber: String) {
        try {
            val cleaned = mobileNumber.replace(Regex("[^0-9]"), "")
            val number = if (cleaned.length > 10 && cleaned.startsWith("91")) {
                cleaned.substring(2)
            } else cleaned
            val encodedNumber = Base64.encodeToString(number.toByteArray(), Base64.NO_WRAP)
            
            val request = ExchangeTokenRequest(encodedNumber)
            val response = exchangeTokenApi.exchangeToken(
                ssoToken = ssoToken,
                deviceId = uniqueId,
                subscriberId = subscriberId,
                request = request
            )
            if (response.isSuccessful) {
                val body = response.body()
                val token = body?.authToken ?: body?.data?.authToken
                if (token != null) {
                    dataStore.saveAccessToken(token)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
