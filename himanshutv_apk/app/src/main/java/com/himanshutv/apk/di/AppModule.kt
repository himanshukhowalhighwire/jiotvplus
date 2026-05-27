package com.himanshutv.apk.di

import android.content.Context
import com.himanshutv.apk.data.AppConfig
import com.himanshutv.apk.data.local.SettingsDataStore
import com.himanshutv.apk.data.remote.AuthApi
import com.himanshutv.apk.data.remote.ExchangeTokenApi
import com.himanshutv.apk.data.remote.MetadataApi
import com.himanshutv.apk.data.remote.PlaybackApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun createRetrofitOkHttpClient(): OkHttpClient {
        val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("x-apisignatures", AppConfig.X_APISIGNATURE)
                .header("x-feature-code", AppConfig.X_FEATURE_CODE)
                .header("x-api-key", AppConfig.X_API_KEY)
                .header("x-platform", AppConfig.X_PLATFORM)
                .header("x-appname", "JioTVPlus")
                .header("app-name", "RJIL_JioTVPlus")
                .header("User-Agent", AppConfig.USER_AGENT)
                .header("devicetype", "tv")
                .header("os", "android")
                .header("lbcookie", "1")
                .build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("AuthRetrofit")
    fun provideAuthRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppConfig.AUTH_BASE_URL)
            .client(createRetrofitOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(@Named("AuthRetrofit") retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideExchangeTokenApi(): ExchangeTokenApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.EXCHANGE_TOKEN_URL.substringBeforeLast("exchangetoken"))
            .client(createRetrofitOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ExchangeTokenApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMetadataApi(): MetadataApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.METADATA_BASE_URL)
            .client(createRetrofitOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(MetadataApi::class.java)
    }

    @Provides
    @Singleton
    fun providePlaybackApi(): PlaybackApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.PLAYBACK_BASE_URL)
            .client(createRetrofitOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(PlaybackApi::class.java)
    }
}
