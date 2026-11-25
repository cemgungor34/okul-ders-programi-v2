package com.okul.dersprogrami

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = context.getSharedPreferences("ders_programi_prefs", Context.MODE_PRIVATE)
            val savedJson = prefs.getString("alarm_config_json", "") ?: ""
            if (savedJson.isNotBlank()) {
                AlarmScheduler.scheduleAllFromConfig(context, savedJson)
            }
            ScheduleWidgetProvider.updateAllWidgets(context)
        }
    }
}
