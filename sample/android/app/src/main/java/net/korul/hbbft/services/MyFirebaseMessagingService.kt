package net.korul.hbbft.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import net.korul.hbbft.CommonFragments.tabSettings.NotificationFragment
import net.korul.hbbft.FireAlarm.FireAlarmActivity
import net.korul.hbbft.MainActivity
import net.korul.hbbft.R
import java.lang.Math.abs
import java.text.SimpleDateFormat
import java.util.*

class MyFirebaseMessagingService : FirebaseMessagingService() {
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Log.d(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        Log.d(TAG, "onMessageReceived: ${remoteMessage?.from}")

        try {
            val roomid = remoteMessage?.data!!["body"]!!
            val uidsFrom = remoteMessage.data!!["title"]!!

            Log.d(TAG, "onMessageReceived from uid: $uidsFrom and roomid $roomid")

            sendNotification(roomid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // [END receive_message]

    // [START on_new_token]
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String?) {
        Log.d(TAG, "Refreshed token: $token")

        sendRegistrationToServer(token)
    }
    // [END on_new_token]

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private fun handleNow() {
        Log.d(TAG, "Short lived task is done.")
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the dialog's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        Log.d(TAG, "sendRegistrationToServer new token - $token")
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(roomid: String) {

        val activityPreferences = this.applicationContext.getSharedPreferences(
            "lasFireActivity",
            AppCompatActivity.MODE_PRIVATE
        )
        val dateLastActivityString = activityPreferences.getString("lasFireActivity", "")
        if (dateLastActivityString != "") {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = formatter.parse(dateLastActivityString)

            if (abs(date.time - Calendar.getInstance().timeInMillis) < 1000 * 5) {
                return
            }
        }

        val calendar = Calendar.getInstance()
        val date = calendar.time
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val dateStr = formatter.format(date)

        val editor = this.applicationContext.getSharedPreferences(
            "lasFireActivity",
            AppCompatActivity.MODE_PRIVATE
        ).edit()
        editor.putString("lasFireActivity", dateStr)
        editor.apply()

        val needAlarm = NotificationFragment.loadNeedActivity(this)
        if (needAlarm) {
            val i = Intent(this, FireAlarmActivity::class.java)
            i.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.putExtra("Start_App", true)
            i.putExtra("RoomId", roomid)
            i.putExtra("ringtone", NotificationFragment.loadUserCurRingtone(this))
            this.startActivity(i)
        } else {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            intent.putExtra("Start_App", true)
            intent.putExtra("RoomId", roomid)
            val pendingIntent = PendingIntent.getActivity(
                this, 0 /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            val channelId = getString(R.string.default_notification_channel_id)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher))
                .setContentTitle("Start connect?")
                .setContentText("User want start messaging in Room - $roomid")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            Log.d(TAG, " sendNotification  ")

            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create channel to show notifications.
                val channelId = getString(R.string.default_notification_channel_id)
                val channelName = getString(R.string.default_notification_channel_name)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        channelName, NotificationManager.IMPORTANCE_HIGH
                    )
                )
            }

            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
        }
    }

    companion object {

        private const val TAG = "HYDRABADGERTAG:PushServ"

        /**
         * Schedule a job using FirebaseJobDispatcher.
         */
        fun scheduleJob(context: Context) {
            // [START dispatch_job]
            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
            val myJob = dispatcher.newJobBuilder()
                .setService(MyJobService::class.java)
                .setTag("my-job-tag")
                .build()
            dispatcher.schedule(myJob)
            // [END dispatch_job]
        }
    }
}
