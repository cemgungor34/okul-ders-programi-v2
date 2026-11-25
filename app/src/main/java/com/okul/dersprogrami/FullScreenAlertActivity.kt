package com.okul.dersprogrami

import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FullScreenAlertActivity : AppCompatActivity() {

    private var vibrator: Vibrator? = null
    private val autoStopHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        // Kilit açma deseni istemeden doğrudan kilit ekranının üzerinde açıl
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        super.onCreate(savedInstanceState)

        val className = intent.getStringExtra("class_name") ?: "DERS SAATİ"
        val subjectName = intent.getStringExtra("subject_name") ?: ""
        val timeInfo = intent.getStringExtra("time_info") ?: ""
        val badgeText = intent.getStringExtra("badge_text") ?: "DERS BAŞLIYOR"
        val soundType = intent.getStringExtra("sound_type") ?: "school_bell"
        val notifId = intent.getIntExtra("notif_id", -1)

        NotificationHelper.playSelectedMelody(this, soundType, loop = (soundType == "system_alarm"))
        startVibration()

        // 45 saniye sonra dokunulmazsa otomatik sustur
        autoStopHandler.postDelayed({ dismissAlert(notifId) }, 45_000L)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(56, 72, 56, 72)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#1E1B4B"), Color.parseColor("#312E81"), Color.parseColor("#0F172A"))
            )
        }

        val badgeView = TextView(this).apply {
            text = "🔔 $badgeText"
            setTextColor(Color.parseColor("#A7F3D0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(36, 16, 36, 16)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3310B981"))
                cornerRadius = 999f
            }
        }

        val hintLabel = TextView(this).apply {
            text = "SIRADAKİ SINIFINIZ"
            setTextColor(Color.parseColor("#CBD5E1"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 8)
        }

        val classCard = TextView(this).apply {
            text = className
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 76f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(48, 36, 48, 36)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 48f
            }
        }

        val subjectView = TextView(this).apply {
            text = subjectName
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 44, 0, 12)
        }

        val timeView = TextView(this).apply {
            text = timeInfo
            setTextColor(Color.parseColor("#E2E8F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 64)
        }

        val dismissBtn = Button(this).apply {
            text = "✓  TAMAM / DERSE GEÇ"
            setTextColor(Color.parseColor("#0F172A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(64, 42, 64, 42)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FACC15"))
                cornerRadius = 36f
            }
            setOnClickListener {
                dismissAlert(notifId)
            }
        }

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 12, 0, 12)
        }

        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        root.addView(badgeView)
        root.addView(hintLabel)
        root.addView(classCard, cardParams)
        root.addView(subjectView)
        root.addView(timeView)
        root.addView(dismissBtn, btnParams)

        setContentView(root)
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 250, 500, 250, 700)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, -1)
        }
    }

    private fun dismissAlert(notifId: Int) {
        autoStopHandler.removeCallbacksAndMessages(null)
        NotificationHelper.stopMelody()
        vibrator?.cancel()
        if (notifId != -1) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(notifId)
        }
        finish()
    }

    override fun onDestroy() {
        autoStopHandler.removeCallbacksAndMessages(null)
        NotificationHelper.stopMelody()
        vibrator?.cancel()
        super.onDestroy()
    }
}
