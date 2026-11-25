package com.okul.dersprogrami

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_ID_PREFIX = "okul_ders_zili_v2_"
    private var mediaPlayer: MediaPlayer? = null
    private var activeRingtone: Ringtone? = null

    fun getSoundUri(context: Context, soundType: String): Uri {
        return when (soundType) {
            "system_alarm" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            "system_ringtone" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            "system_notif" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            else -> Uri.parse("android.resource://${context.packageName}/${R.raw.okul_zili}")
        }
    }

    fun playSelectedMelody(context: Context, soundType: String, loop: Boolean = false) {
        stopMelody()
        try {
            if (soundType == "school_bell") {
                val mp = MediaPlayer.create(context.applicationContext, R.raw.okul_zili)
                mp?.isLooping = loop
                mp?.setOnCompletionListener {
                    if (!loop) stopMelody()
                }
                mp?.start()
                mediaPlayer = mp
            } else {
                val uri = getSoundUri(context, soundType)
                val rt = RingtoneManager.getRingtone(context.applicationContext, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    rt?.isLooping = loop
                }
                rt?.play()
                activeRingtone = rt
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopMelody() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null

        try {
            activeRingtone?.stop()
        } catch (_: Exception) {
        }
        activeRingtone = null
    }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val types = listOf("school_bell", "system_alarm", "system_ringtone", "system_notif")
            for (t in types) {
                val channelId = CHANNEL_ID_PREFIX + t
                val soundUri = getSoundUri(context, t)
                val audioAttrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()

                val channel = NotificationChannel(
                    channelId,
                    "Ders Zili Uyarıları ($t)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Ders saati geldiğinde tam ekran uyarı ve zil sesi verir"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 450, 200, 450)
                    setSound(soundUri, audioAttrs)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showLessonAlert(
        context: Context,
        notifId: Int,
        className: String,
        subjectName: String,
        timeInfo: String,
        badgeText: String,
        soundType: String,
        useFullScreen: Boolean
    ) {
        createChannels(context)

        val fullScreenIntent = Intent(context, FullScreenAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notif_id", notifId)
            putExtra("class_name", className)
            putExtra("subject_name", subjectName)
            putExtra("time_info", timeInfo)
            putExtra("badge_text", badgeText)
            putExtra("sound_type", soundType)
        }

        val fullScreenPi = PendingIntent.getActivity(
            context,
            notifId + 5000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            notifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = CHANNEL_ID_PREFIX + soundType
        val soundUri = getSoundUri(context, soundType)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("👉 $className — $subjectName ($badgeText)")
            .setContentText(timeInfo)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$className • $subjectName
$timeInfo"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 450, 200, 450))
            .setContentIntent(if (useFullScreen) fullScreenPi else contentPi)

        if (useFullScreen) {
            builder.setFullScreenIntent(fullScreenPi, true)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, builder.build())

        // Ekran kilitliyse doğrudan Tam Ekran Uyarıyı da aç
        if (useFullScreen) {
            try {
                context.startActivity(fullScreenIntent)
            } catch (_: Exception) {
            }
        }
    }
}
