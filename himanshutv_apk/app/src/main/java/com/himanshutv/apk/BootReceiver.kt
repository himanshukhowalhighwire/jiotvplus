package com.himanshutv.apk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.himanshutv.apk.data.local.SettingsDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val dataStore = SettingsDataStore(context)
            val shouldStart = runBlocking { dataStore.autoStartOnBoot.firstOrNull() ?: false }
            
            if (shouldStart) {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
