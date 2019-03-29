package net.korul.hbbft.CommonFragments.tabSettings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.crashlytics.android.Crashlytics
import kotlinx.android.synthetic.main.fragment_settings_notifications.*
import net.korul.hbbft.CommonFragments.tabSettings.RingtoneUtil.RingtonePickerDialog
import net.korul.hbbft.CommonFragments.tabSettings.RingtoneUtil.RingtonePickerDialogController
import net.korul.hbbft.R
import java.io.IOException


class NotificationFragment : Fragment() {

    private val TAG = "HYDRA:NotificationFragment"
    private var mRingtonePickerController: RingtonePickerDialogController? = null
    private var mRingtone: Uri? = null
    private var mMediaPlayer: MediaPlayer? = null

    private var curAllarmringtone: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()

        curAllarmringtone = loadUserCurRingtone(context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_notifications, container, false)
    }

    override fun onStop() {
        super.onStop()

        curAllarmringtone = if (mRingtone != null)
            mRingtone.toString()
        else
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL).toString()

        saveUserCurRingtone(context!!, curAllarmringtone!!)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_back.setOnClickListener {
            (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }

        try {
            mRingtonePickerController = RingtonePickerDialogController(fragmentManager,
                object : RingtonePickerDialog.OnRingtoneSelectedListener {
                    override fun onRingtoneSelected(ringtoneUri: Uri?) {
                        Log.d(TAG, "Selected ringtone: " + ringtoneUri.toString())
                        mRingtone = ringtoneUri

                        val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
                        val title = ringtone.getTitle(context)
                        ringtone_name.text = title
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            Crashlytics.logException(e)
        }

        try {
            if (curAllarmringtone == null) {
                mRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL)

                val ringtone = RingtoneManager.getRingtone(context, mRingtone)
                val title = ringtone.getTitle(context)
                ringtone_name.text = title
            } else {
                mRingtone = Uri.parse(curAllarmringtone)

                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(curAllarmringtone))
                val title = ringtone.getTitle(context)
                ringtone_name.text = title
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Crashlytics.logException(e)
        }

        try {
            ringtone.setOnClickListener {
                if (curAllarmringtone == null)
                    mRingtonePickerController!!.show(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL), TAG)
                else
                    mRingtonePickerController!!.show(Uri.parse(curAllarmringtone), TAG)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Crashlytics.logException(e)
        }


        open_activity_or_push.isChecked = loadNeedActivity(context!!)
        open_activity_or_push.setOnClickListener {
            saveNeedActivity(context!!, open_activity_or_push.isChecked)
        }

        hasNeededPermission()
    }

    fun hasNeededPermission() {
        var permisionAsk = false
        val permissions = ArrayList<String>()
        if (!activity!!.hasWRITE_SETTINGSPermission()) {
            permissions.add(Manifest.permission.WRITE_SETTINGS)
            permisionAsk = true
        }
        if (!activity!!.hasWAKE_LOCKPermission()) {
            permissions.add(Manifest.permission.WAKE_LOCK)
            permisionAsk = true
        }

        if (permisionAsk) {
            if (Build.VERSION.SDK_INT >= 23)
                requestPermissions(permissions.toTypedArray(), 123)
            else
                ActivityCompat.requestPermissions(activity!!, permissions.toTypedArray(), 123)
        }
    }

    private fun Context.hasWRITE_SETTINGSPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    private fun Context.hasWAKE_LOCKPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED


    private fun showRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select ringtone for notifications:")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        startActivityForResult(intent, 999)
    }

    private fun playSound(context: Context, alert: Uri) {
        mMediaPlayer = MediaPlayer()
        try {
            mMediaPlayer!!.setDataSource(context, alert)
            val audioManager = context
                .getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_ALARM)
                mMediaPlayer!!.prepare()
                mMediaPlayer!!.start()
            }
            mMediaPlayer!!.setOnCompletionListener { mMediaPlayer = null }
        } catch (e: IOException) {
            println("OOPS")
        }
    }

    companion object {
        fun loadNeedActivity(context: Context): Boolean {
            val activityPreferences = context.applicationContext.getSharedPreferences(
                "curUsersAlarm",
                AppCompatActivity.MODE_PRIVATE
            )
            val dateLastBackupString = activityPreferences.getBoolean("needActivity", true)
            return dateLastBackupString
        }

        fun saveNeedActivity(context: Context, needActivity: Boolean) {
            val editor = context.applicationContext.getSharedPreferences(
                "curUsersAlarm",
                AppCompatActivity.MODE_PRIVATE
            ).edit()
            editor.putBoolean("needActivity", needActivity)
            editor.apply()
        }

        fun loadUserCurRingtone(context: Context): String? {
            try {
                val activityPreferences = context.applicationContext.getSharedPreferences(
                    "curUsersAlarm",
                    AppCompatActivity.MODE_PRIVATE
                )
                val dateLastBackupString = activityPreferences.getString("curUsersAlarm", "")

                return if (dateLastBackupString != "") {
                    dateLastBackupString
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Crashlytics.logException(e)
                return null
            }
        }

        fun saveUserCurRingtone(context: Context, newAlarmRingtone: String) {
            try {
                val editor = context.applicationContext.getSharedPreferences(
                    "curUsersAlarm",
                    AppCompatActivity.MODE_PRIVATE
                ).edit()
                editor.putString("curUsersAlarm", newAlarmRingtone)
                editor.apply()
            } catch (e: Exception) {
                e.printStackTrace()
                Crashlytics.logException(e)
            }
        }

        fun newInstance() = NotificationFragment()
    }
}
