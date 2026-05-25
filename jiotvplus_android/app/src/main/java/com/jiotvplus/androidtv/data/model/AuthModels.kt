package com.jiotvplus.androidtv.data.model

import com.google.gson.annotations.SerializedName

data class OtpRequestResponse(
    @SerializedName("identifier") val identifier: String? = null,
    @SerializedName("message") val message: String? = null
)

data class VerifyOtpRequest(
    @SerializedName("identifier") val identifier: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("upgradeAuth") val upgradeAuth: String = "Y",
    @SerializedName("rememberUser") val rememberUser: String = "T",
    @SerializedName("deviceInfo") val deviceInfo: DeviceInfo = DeviceInfo()
)

data class DeviceInfo(
    @SerializedName("consumptionDeviceName") val consumptionDeviceName: String = "JioTVPlusAndroidTV",
    @SerializedName("info") val info: Info = Info()
)

data class Info(
    @SerializedName("type") val type: String = "android",
    @SerializedName("androidId") val androidId: String = "30303030303030303030303030303030",
    @SerializedName("platform") val platform: Platform = Platform()
)

data class Platform(
    @SerializedName("name") val name: String = "JioTVPlusAndroidTV"
)

data class VerifyOtpResponse(
    @SerializedName("ssoToken") val ssoToken: String?,
    @SerializedName("jToken") val jToken: String?,
    @SerializedName("lbCookie") val lbCookie: String?,
    @SerializedName("subscriberId") val subscriberId: String?,
    @SerializedName("sessionAttributes") val sessionAttributes: SessionAttributes?
)

data class SessionAttributes(
    @SerializedName("lbCookie") val lbCookie: String?,
    @SerializedName("user") val user: User?
)

data class User(
    @SerializedName("ssoToken") val ssoToken: String?,
    @SerializedName("jToken") val jToken: String?,
    @SerializedName("lbCookie") val lbCookie: String?,
    @SerializedName("subscriberId") val subscriberId: String?,
    @SerializedName("unique") val unique: String?
)

data class ExchangeTokenRequest(
    @SerializedName("number") val number: String
)

data class ExchangeTokenResponse(
    @SerializedName("authToken") val authToken: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("data") val data: ExchangeTokenData?
)

data class ExchangeTokenData(
    @SerializedName("authToken") val authToken: String?
)
