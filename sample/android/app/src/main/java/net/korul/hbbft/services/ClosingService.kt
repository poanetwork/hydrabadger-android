package net.korul.hbbft.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.annotation.Nullable
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import net.korul.hbbft.DatabaseApplication


class ClosingService : Service() {

    private var TAG = "HYDRA:ClosingService"

    @Nullable
    override fun onBind(intent: Intent): IBinder? {

        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hydrabadger",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()

            startForeground(1, notification)
        }
    }

//    /**
//     * Schedule a job using FirebaseJobDispatcher.
//     */
fun scheduleJob(context: Context) {
    // [START dispatch_job]
    val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
    val myJob = dispatcher.newJobBuilder()
        .setService(MyJobService::class.java)
        .setTag("MyJobService")
        .build()
    dispatcher.schedule(myJob)
    // [END dispatch_job]
}


    @SuppressLint("CheckResult")
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)

        Log.i(TAG, "Service: start close app")

        DatabaseApplication.mCoreHBBFT2X.sendMessageIDIE()
        Log.i(TAG, "sendMessageIDIE()")


        Thread.sleep(7*1000)
        DatabaseApplication.mCoreHBBFT2X.freeCoreHBBFT()
        Log.i(TAG, "freeCoreHBBFT()")

        scheduleJob(this)

        // Destroy the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
            stopSelf()
        } else {
            stopSelf()
        }
    }
}