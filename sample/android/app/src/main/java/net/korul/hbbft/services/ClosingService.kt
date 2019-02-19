package net.korul.hbbft.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.annotation.Nullable
import android.util.Log
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.services.MyFirebaseMessagingService.Companion.scheduleJob
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread


class ClosingService : Service() {

    private var TAG = "HYDRABADGERTAG:ClosingService"

    @Nullable
    override fun onBind(intent: Intent): IBinder? {

        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")

        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("CheckResult")
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)

        Log.i(TAG, "Service: start close app")

        DatabaseApplication.mCoreHBBFT2X.Free()

        Log.i(TAG, "Service: finish close app")
        // Destroy the service
        stopSelf()
    }
}