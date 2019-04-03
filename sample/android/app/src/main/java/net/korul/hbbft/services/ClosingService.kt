package net.korul.hbbft.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.annotation.Nullable
import android.util.Log
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

//    /**
//     * Schedule a job using FirebaseJobDispatcher.
//     */
//    fun scheduleJob(context: Context) {
//        // [START dispatch_job]
//        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
//        val myJob = dispatcher.newJobBuilder()
//            .setService(MyJobService::class.java)
//            .setTag("MyJobService")
//            .build()
//        dispatcher.schedule(myJob)
//        // [END dispatch_job]
//    }


    @SuppressLint("CheckResult")
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)

        Log.i(TAG, "Service: start close app")

        DatabaseApplication.mCoreHBBFT2X.sendMessageIDIE()
        Log.i(TAG, "sendMessageIDIE()")

//        scheduleJob(this)

        Thread.sleep(7*1000)
        DatabaseApplication.mCoreHBBFT2X.freeCoreHBBFT()
        Log.i(TAG, "freeCoreHBBFT()")


        // Destroy the service
        stopSelf()
    }
}