package com.himanshutv.apk

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HimanshuTVApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
