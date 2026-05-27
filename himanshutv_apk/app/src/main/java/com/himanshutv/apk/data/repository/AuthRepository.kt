package com.himanshutv.apk.data.repository

import android.util.Base64
import com.himanshutv.apk.data.local.SettingsDataStore
import com.himanshutv.apk.data.model.ExchangeTokenRequest
import com.himanshutv.apk.data.model.VerifyOtpRequest
import com.himanshutv.apk.data.remote.AuthApi
import com.himanshutv.apk.data.remote.ExchangeTokenApi
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val exchangeTokenApi: ExchangeTokenApi,
    private val dataStore: SettingsDataStore
) {
    private var currentOtpIdentifier: String? = null

    suspend fun sendOtp(mobile: String): Result<Unit> {
        return try {
            val cleaned = cleanMobileNumber(mobile)
            val response = authApi.sendOtp(cleaned)
            if (response.isSuccessful && response.body()?.identifier != null) {
                currentOtpIdentifier = response.body()?.identifier
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send OTP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(mobile: String, otp: String): Result<Unit> {
        return try {
            val identifier = currentOtpIdentifier ?: return Result.failure(Exception("OTP identifier missing"))
            val response = authApi.verifyOtp(VerifyOtpRequest(identifier = identifier, otp = otp))
            
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                var ssoToken = body.ssoToken
                var jToken = body.jToken
                var lbCookie = body.lbCookie
                var subscriberId = body.subscriberId
                var uniqueId: String? = null

                body.sessionAttributes?.let { attrs ->
                    if (lbCookie == null && attrs.lbCookie != null) lbCookie = attrs.lbCookie
                    attrs.user?.let { user ->
                        if (ssoToken == null) ssoToken = user.ssoToken
                        if (jToken == null) jToken = user.jToken
                        if (lbCookie == null) lbCookie = user.lbCookie
                        if (subscriberId == null) subscriberId = user.subscriberId
                        uniqueId = user.unique
                    }
                }

                if (subscriberId == null && body.subscriberId != null) {
                    subscriberId = body.subscriberId
                }

                // Exchange token
                if (ssoToken != null) {
                    val cleaned = cleanMobileNumber(mobile)
                    val base64Number = Base64.encodeToString(cleaned.toByteArray(), Base64.NO_WRAP)
                    val deviceId = uniqueId ?: java.util.UUID.randomUUID().toString()
                    val subId = subscriberId ?: ""
                    
                    val exchangeResp = exchangeTokenApi.exchangeToken(
                        ssoToken = ssoToken!!,
                        deviceId = deviceId,
                        subscriberId = subId,
                        request = ExchangeTokenRequest(base64Number)
                    )

                    var accessToken: String? = null
                    var refreshToken: String? = null

                    if (exchangeResp.isSuccessful && exchangeResp.body() != null) {
                        val exBody = exchangeResp.body()!!
                        if (exBody.authToken != null) {
                            accessToken = exBody.authToken
                            refreshToken = exBody.refreshToken
                        } else if (exBody.data?.authToken != null) {
                            accessToken = exBody.data.authToken
                        }
                    }

                    dataStore.saveAuthData(
                        mobile = mobile,
                        sso = ssoToken,
                        jTokenVal = jToken,
                        lbCookieVal = lbCookie,
                        subId = subId,
                        unique = deviceId,
                        access = accessToken,
                        refresh = refreshToken
                    )
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("SSO Token missing from response"))
                }
            } else {
                Result.failure(Exception("Failed to verify OTP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return dataStore.accessToken.firstOrNull() != null || dataStore.ssoToken.firstOrNull() != null
    }
    
    suspend fun logout() {
        dataStore.clear()
    }

    private fun cleanMobileNumber(mobile: String): String {
        var cleaned = mobile.replace(Regex("[^0-9]"), "")
        if (cleaned.length > 10 && cleaned.startsWith("91")) {
            cleaned = cleaned.substring(2)
        } else if (cleaned.length > 10 && cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1)
        }
        return cleaned
    }
}
