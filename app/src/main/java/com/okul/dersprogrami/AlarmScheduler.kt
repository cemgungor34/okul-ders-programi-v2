package com.okul.dersprogrami

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONObject
import java.util.Calendar

object AlarmScheduler {

    private const val MAX_ALARM_SLOTS = 70

    fun scheduleAllFromConfig(context: Context, jsonStr: String) {
        cancelAllAlarms(context)
        if (jsonStr.isBlank()) return

        try {
            val root = JSONObject(jsonStr)
            val enabled = root.optBoolean("enabled", true)
            if (!enabled) return

            val offsetMinutes = root.optInt("offset", 0)
            val soundType = root.optString("soundType", "school_bell")
            val fullScreen = root.optBoolean("fullScreen", true)
            val bellTimes = root.getJSONArray("bellTimes")
            val scheduleData = root.getJSONArray("scheduleData")

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            var requestCodeCounter = 100

            for (d in 0 until scheduleData.length()) {
                val dayObj = scheduleData.getJSONObject(d)
                val jsDayIndex = dayObj.getInt("dayIndex")
                val calendarDay = jsDayToCalendarDay(jsDayIndex)
                val slots = dayObj.getJSONArray("slots")

                for (s in 0 until slots.length()) {
                    if (slots.isNull(s)) continue
                    val slotObj = slots.getJSONObject(s)
                    val cls = slotObj.optString("cls", "")
                    val subject = slotObj.optString("subject", "Ders")
                    if (cls.isBlank()) continue

                    val bellObj = bellTimes.getJSONObject(s)
                    val startStr = bellObj.getString("start")
                    val endStr = bellObj.getString("end")
                    val parts = startStr.split(":")
                    val hour = parts[0].toInt()
                    val minute = parts[1].toInt()

                    val cal = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, calendarDay)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.MINUTE, -offsetMinutes)
                    }

                    if (cal.timeInMillis <= System.currentTimeMillis()) {
                        cal.add(Calendar.WEEK_OF_YEAR, 1)
                    }

                    val badgeText = if (offsetMinutes == 0) "DERS BAŞLADI!" else "DERSE $offsetMinutes DK KALDI!"
                    val timeInfo = "${s + 1}. Ders • $startStr - $endStr"

                    val intent = Intent(context, LessonAlarmReceiver::class.java).apply {
                        putExtra("notif_id", requestCodeCounter)
                        putExtra("class_name", cls)
                        putExtra("subject_name", subject)
                        putExtra("time_info", timeInfo)
                        putExtra("badge_text", badgeText)
                        putExtra("sound_type", soundType)
                        putExtra("full_screen", fullScreen)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCodeCounter,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    setExactAlarmCompat(alarmManager, cal.timeInMillis, pendingIntent)
                    requestCodeCounter++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setExactAlarmCompat(alarmManager: AlarmManager, triggerAtMillis: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (code in 100 until (100 + MAX_ALARM_SLOTS)) {
            val intent = Intent(context, LessonAlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
    }

    private fun jsDayToCalendarDay(jsDay: Int): Int {
        return when (jsDay) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            else -> Calendar.SUNDAY
        }
    }
}
