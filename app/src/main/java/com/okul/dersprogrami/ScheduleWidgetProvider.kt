package com.okul.dersprogrami

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import org.json.JSONObject
import java.util.Calendar

class ScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.okul.dersprogrami.ACTION_REFRESH_WIDGET") {
            updateAllWidgets(context)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ScheduleWidgetProvider::class.java))
            for (id in ids) {
                updateSingleWidget(context, manager, id)
            }
        }

        private fun updateSingleWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_schedule)

            // Widget'a tıklanınca uygulamayı aç
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openPi = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPi)

            // Yenile ikonuna tıklanınca widget'ı anında güncelle
            val refreshIntent = Intent(context, ScheduleWidgetProvider::class.java).apply {
                action = "com.okul.dersprogrami.ACTION_REFRESH_WIDGET"
            }
            val refreshPi = PendingIntent.getBroadcast(
                context, 1, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPi)

            val state = computeCurrentLessonState(context)
            views.setTextViewText(R.id.widget_badge, state.badge)
            views.setTextViewText(R.id.widget_main_class, state.mainText)
            views.setTextViewText(R.id.widget_sub_info, state.subText)
            views.setInt(R.id.widget_badge, "setTextColor", state.badgeColor)

            manager.updateAppWidget(widgetId, views)
        }

        private data class WidgetState(
            val badge: String,
            val mainText: String,
            val subText: String,
            val badgeColor: Int
        )

        private fun computeCurrentLessonState(context: Context): WidgetState {
            val prefs = context.getSharedPreferences("ders_programi_prefs", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("alarm_config_json", "") ?: ""
            if (jsonStr.isBlank()) {
                return WidgetState(
                    "DERS PROGRAMI",
                    "Program Henüz Boş",
                    "Eklemek için dokunun",
                    Color.parseColor("#93C5FD")
                )
            }

            try {
                val root = JSONObject(jsonStr)
                val bellTimes = root.getJSONArray("bellTimes")
                val scheduleData = root.getJSONArray("scheduleData")

                val cal = Calendar.getInstance()
                val jsDay = when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    else -> 0
                }
                val nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

                var todayObj: JSONObject? = null
                for (i in 0 until scheduleData.length()) {
                    val d = scheduleData.getJSONObject(i)
                    if (d.getInt("dayIndex") == jsDay) {
                        todayObj = d
                        break
                    }
                }

                if (todayObj == null) {
                    return WidgetState("HAFTA SONU", "Bugün Ders Yok 🎉", "İyi tatiller!", Color.parseColor("#A7F3D0"))
                }

                val slots = todayObj.getJSONArray("slots")
                var hasAny = false
                for (i in 0 until slots.length()) {
                    if (!slots.isNull(i)) hasAny = true
                }
                if (!hasAny) {
                    return WidgetState(
                        "${todayObj.getString("day").uppercase()} • BOŞ GÜN",
                        "Bugün Dersiniz Yok ☕",
                        "Programınız bugün boş",
                        Color.parseColor("#A7F3D0")
                    )
                }

                // 1. Şu an derste mi?
                for (i in 0 until 7) {
                    val b = bellTimes.getJSONObject(i)
                    val sM = toMins(b.getString("start"))
                    val eM = toMins(b.getString("end"))
                    if (nowMins in sM until eM) {
                        val rem = eM - nowMins
                        return if (!slots.isNull(i)) {
                            val slot = slots.getJSONObject(i)
                            WidgetState(
                                "🟢 ŞU AN ${i + 1}. DERS (${b.getString("start")}–${b.getString("end")})",
                                "${slot.getString("cls")} — ${slot.getString("subject")}",
                                "Zile $rem dakika kaldı",
                                Color.parseColor("#4ADE80")
                            )
                        } else {
                            WidgetState(
                                "☕ ${i + 1}. DERS SAATİ BOŞ",
                                "Bu Saat Boş",
                                "Bitime $rem dakika var",
                                Color.parseColor("#FDE047")
                            )
                        }
                    }
                }

                // 2. Teneffüs veya gün başı - sıradaki dolu dersi bul
                for (i in 0 until 7) {
                    val b = bellTimes.getJSONObject(i)
                    val sM = toMins(b.getString("start"))
                    if (nowMins < sM && !slots.isNull(i)) {
                        val wait = sM - nowMins
                        val slot = slots.getJSONObject(i)
                        return WidgetState(
                            "⏳ SIRADAKİ DERS (${i + 1}. DERS)",
                            "${slot.getString("cls")} — ${slot.getString("subject")}",
                            "Saat ${b.getString("start")}'da ($wait dk sonra)",
                            Color.parseColor("#FDBA74")
                        )
                    }
                }

                return WidgetState("GÜN TAMAMLANDI", "Bugünkü Dersler Bitti 🎉", "İyi dinlenmeler!", Color.parseColor("#93C5FD"))
            } catch (_: Exception) {
                return WidgetState("DERS PROGRAMI", "Programı Görüntüle", "Dokunun", Color.WHITE)
            }
        }

        private fun toMins(hhmm: String): Int {
            val p = hhmm.split(":")
            return p[0].toInt() * 60 + p[1].toInt()
        }
    }
}
