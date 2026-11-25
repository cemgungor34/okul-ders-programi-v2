package com.okul.dersprogrami

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannels(this)
        requestRequiredPermissions()

        webView = WebView(this)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.addJavascriptInterface(WebAppBridge(this), "AndroidBridge")
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                }
            }
        }

        // Android 14+ Tam Ekran Intent İzni Kontrolü
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.canUseFullScreenIntent()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                }
            }
        }
    }

    class WebAppBridge(private val context: Context) {
        @JavascriptInterface
        fun syncAlarms(jsonPayload: String) {
            val prefs = context.getSharedPreferences("ders_programi_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("alarm_config_json", jsonPayload).apply()
            AlarmScheduler.scheduleAllFromConfig(context, jsonPayload)
            ScheduleWidgetProvider.updateAllWidgets(context)
        }

        @JavascriptInterface
        fun previewSound(soundType: String) {
            NotificationHelper.playSelectedMelody(context, soundType, loop = false)
        }

        @JavascriptInterface
        fun testFullScreenAlert(className: String, subjectName: String, timeInfo: String, soundType: String) {
            val intent = Intent(context, FullScreenAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("class_name", className)
                putExtra("subject_name", subjectName)
                putExtra("time_info", timeInfo)
                putExtra("sound_type", soundType)
                putExtra("badge_text", "TEST UYARISI")
            }
            context.startActivity(intent)
        }
    }
}
