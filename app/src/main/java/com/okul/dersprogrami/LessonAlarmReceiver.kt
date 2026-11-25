package com.okul.dersprogrami

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LessonAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra("notif_id", 101)
        val className = intent.getStringExtra("class_name") ?: "Ders Saati"
        val subjectName = intent.getStringExtra("subject_name") ?: ""
        val timeInfo = intent.getStringExtra("time_info") ?: ""
        val badgeText = intent.getStringExtra("badge_text") ?: "DERS BAŞLADI"
        val soundType = intent.getStringExtra("sound_type") ?: "school_bell"
        val fullScreen = intent.getBooleanExtra("full_screen", true)

        NotificationHelper.showLessonAlert(
            context = context,
            notifId = notifId,
            className = className,
            subjectName = subjectName,
            timeInfo = timeInfo,
            badgeText = badgeText,
            soundType = soundType,
            useFullScreen = fullScreen
        )

        ScheduleWidgetProvider.updateAllWidgets(context)

        val prefs = context.getSharedPreferences("ders_programi_prefs", Context.MODE_PRIVATE)
        val savedJson = prefs.getString("alarm_config_json", "") ?: ""
        if (savedJson.isNotBlank()) {
            AlarmScheduler.scheduleAllFromConfig(context, savedJson)
        }
    }
}
