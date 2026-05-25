package com.jiotvplus.androidtv.data.remote

import com.jiotvplus.androidtv.data.model.ExchangeTokenRequest
import com.jiotvplus.androidtv.data.model.ExchangeTokenResponse
import com.jiotvplus.androidtv.data.model.OtpRequestResponse
import com.jiotvplus.androidtv.data.model.VerifyOtpRequest
import com.jiotvplus.androidtv.data.model.VerifyOtpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {
    @Headers(
        "Content-Length: 0"
    )
    @POST("sendotp")
    suspend fun sendOtp(
        @Header("number") number: String
    ): Response<OtpRequestResponse>

    @POST("verifyotp")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<VerifyOtpResponse>
}

interface ExchangeTokenApi {
    @POST("exchangetoken")
    suspend fun exchangeToken(
        @Header("ssotoken") ssoToken: String,
        @Header("deviceid") deviceId: String,
        @Header("subscriberid") subscriberId: String,
        @Header("persistentRefreshToken") persistentRefreshToken: String = "true",
        @Header("x-platform") xPlatform: String = "jiotvplus-androidtv",
        @Body request: ExchangeTokenRequest
    ): Response<ExchangeTokenResponse>
}
