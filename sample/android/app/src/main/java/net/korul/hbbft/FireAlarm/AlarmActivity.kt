package net.korul.hbbft.FireAlarm


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.os.Bundle
import android.os.Vibrator
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import com.crashlytics.android.Crashlytics

/**
 * Alarm activity that pops up a visible indicator when the alarm goes off.
 */
open class AlarmActivity : AppCompatActivity() {
    private var LOGTAG = "AlarmActivity"

    // Controller for GlowPadView.
    private var mv: Vibrator? = null
    private var mringtone: Ringtone? = null


    private var mVolumeBehavior: Int = 0
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.v(LOGTAG, "AlarmActivity - Broadcast Receiver - " + action!!)
            when (action) {
                ALARM_SNOOZE_ACTION -> snooze()
                ALARM_DISMISS_ACTION -> dismiss()
                else -> {
                    Log.i(LOGTAG, "Unknown broadcast in AlarmActivity: $action")
                    finish()
                }
            }
        }
    }

    private fun snooze() {
        //        AlarmStateManager.setSnoozeState(this, mInstance);
    }

    protected fun dismiss() {
        try {
            mringtone?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
            Crashlytics.logException(e)
        }

        try {
            mv?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
            Crashlytics.logException(e)
        }
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Get the volume/camera button behavior setting
        val vol = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(
                SettingsActivity.KEY_VOLUME_BEHAVIOR,
                SettingsActivity.DEFAULT_VOLUME_BEHAVIOR
            )
        mVolumeBehavior = Integer.parseInt(vol!!)

        val win = window
        win.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

    }

    public override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(mReceiver)
        } catch (e: Exception) {
            Log.d(LOGTAG, "Receiver not registered")
        }

    }

    fun setPauseListener(v: Vibrator?, ringtone: Ringtone?) {
        mv = v
        mringtone = ringtone
    }

    override fun onBackPressed() {
        // Don't allow back to dismiss.
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Do this on key down to handle a few of the system keys.
        Log.v(LOGTAG, "AlarmActivity - dispatchKeyEvent - " + event.keyCode)
        when (event.keyCode) {
            // Volume keys and camera keys dismiss the alarm
            KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_VOLUME_UP -> {
                dismiss()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                dismiss()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_CAMERA, KeyEvent.KEYCODE_FOCUS -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    when (mVolumeBehavior) {
                        1 -> dismiss()

                        2 -> dismiss()

                        else -> {
                        }
                    }
                }
                return true
            }
            else -> {
            }
        }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        // AlarmActivity listens for this broadcast intent, so that other applications
        // can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
        const val ALARM_SNOOZE_ACTION = "show_and_dismiss_alarm"

        // AlarmActivity listens for this broadcast intent, so that other applications
        // can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
        const val ALARM_DISMISS_ACTION = "show_and_dismiss_alarm"
    }
}
