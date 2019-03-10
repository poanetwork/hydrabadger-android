package net.korul.hbbft.FireAlarm

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.ViewGroup
import android.view.WindowManager
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.fragment_fire_alarm.*
import net.korul.hbbft.MainActivity
import net.korul.hbbft.R


class FireAlarmActivity : AlarmActivity() {

    private var LOGTAG = "FireAlarmActivity"
    private var alert: Uri? = null
    private var ringtone: Ringtone? = null

    private var v: Vibrator? = null

    private var vibro: Boolean = false

    private var myLock: KeyguardManager.KeyguardLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    var RoomId: String = ""

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.setBackgroundDrawable(ColorDrawable(0))

        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, LOGTAG
        )
        wakeLock?.acquire(10 * 1000)

        val myKeyGuard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        myLock = myKeyGuard.newKeyguardLock(LOGTAG)
        myLock?.disableKeyguard()

        setContentView(R.layout.fragment_fire_alarm)

        // Popup alert over black screen
        val lp = window.attributes
        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        // XXX DO NOT COPY THIS!!!  THIS IS BOGUS!  Making an activity have
        // a system alert type is completely broken, because the activity
        // manager will still hide/show it as if it is part of the normal
        // activity stack.  If this is really what you want and you want it
        // to work correctly, you should create and show your own custom window.TYPE_APPLICATION_OVERLAY
//        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT  TYPE_APPLICATION_OVERLAY
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT //TYPE_PHONE //TYPE_SYSTEM_ALERT
        lp.token = null
        window.attributes = lp
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND
        )

        initAlarmSound()
        playDefaultAlarmSound()

        vibro = intent.getBooleanExtra("vibro", true)

        if (vibro) {
            vibrate()
        }

        RoomId = intent.getStringExtra("RoomId")

        setPauseListener(v, ringtone)

        Log.i(LOGTAG, "FireAlarmActivity onCreate finish")
    }

    override fun onResume() {
        super.onResume()

        closeAlarmTextView.setOnClickListener {
            Log.i(LOGTAG, "closeAlarmTextView is clicked")

            finishAlarm()
            finish()
        }

        openAppTextView.setOnClickListener {
            Log.i(LOGTAG, "openAppTextView is clicked")

            openApp()
            finishAlarm()
            finish()
        }

        val timer = object : CountDownTimer(120 * 1000, 1000) //120 second Timer
        {
            override fun onTick(l: Long) {
            }

            override fun onFinish() {
                finishAlarm()
                finish()
            }
        }.start()
    }

    private fun openApp() {
        val activityManager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val procInfos = activityManager.runningAppProcesses
        for (i in procInfos.indices) {
            if (procInfos[i].processName == "ru.hintsolutions.diabets") {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                intent.putExtra("Start_App", true)
                intent.putExtra("RoomId", RoomId)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                return
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        intent.putExtra("Start_App", true)
        intent.putExtra("RoomId", RoomId)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            finishAlarm()
        } catch (e: Exception) {
            e.printStackTrace()
            Crashlytics.logException(e)
        }
    }

    private fun vibrate() {
        v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Start without a delay
        // Vibrate for 100 milliseconds
        // Sleep for 1000 milliseconds
        val pattern = longArrayOf(0, 250, 1000)

        // The '0' here means to repeat indefinitely
        // '-1' would play the vibration once
        v?.vibrate(pattern, 0)
    }

    private fun finishAlarm() {
        releaseLocks()

        try {
            dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
            Crashlytics.logException(e)
        }

        try {
            ringtone?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
            Crashlytics.logException(e)
        }

        try {
            v?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
            Crashlytics.logException(e)
        }
    }

    private fun playDefaultAlarmSound() {
        if (alert != null) {
            ringtone = RingtoneManager.getRingtone(applicationContext, alert)
            //TODO fix
            ringtone?.play()
        }
    }

    private fun initAlarmSound() {
        alert = try {
            Uri.parse(intent.getStringExtra("ringtone"))
        } catch (e: Exception) {
            null
        }
        if (alert == null) {
            // alert is null, using backup
            alert = try {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL)
            } catch (e: Exception) {
                null
            }
            if (alert == null) {  // I can't see this ever being null (as always have a default notification) but just incase
                // alert backup is null, using 2nd backup
                alert = try {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    private fun releaseLocks() {
        try {
            myLock?.reenableKeyguard()
            wakeLock?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
