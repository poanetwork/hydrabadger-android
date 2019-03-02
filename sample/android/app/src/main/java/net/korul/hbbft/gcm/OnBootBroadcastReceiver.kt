package net.korul.hbbft.gcm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log
import net.korul.hbbft.MainActivity
import net.korul.hbbft.R
import net.korul.hbbft.services.MyFirebaseMessagingService


class OnBootBroadcastReceiver : BroadcastReceiver() {
    private val TAG = "GCM"
    private var mcontext: Context? = null


    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        mcontext = context

        try {
//            val roomname = remoteMessage?.data!!["body"]!!
//            val uidsFrom = remoteMessage.data!!["title"]!!

//            Log.d(TAG, "onMessageReceived from uid: $uidsFrom and roomname $roomname")

//            sendNotification(roomname)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun sendNotification(roomname: String) {
        val intent = Intent(mcontext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        intent.putExtra("Start_App", true)
        intent.putExtra("RoomName", roomname)
        val pendingIntent = PendingIntent.getActivity(
            mcontext, 0 /* Request code */, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = mcontext?.getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(mcontext!!, channelId!!)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(mcontext?.resources, R.mipmap.ic_launcher))
            .setContentTitle("Start connect?")
            .setContentText("User want start messaging in Room - $roomname")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = mcontext?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        Log.d(TAG, " sendNotification  ")

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = mcontext?.getString(R.string.default_notification_channel_id)
            val channelName = mcontext?.getString(R.string.default_notification_channel_name)
            val notificationManager = mcontext?.getSystemService(NotificationManager::class.java)
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