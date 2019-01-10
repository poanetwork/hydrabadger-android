package net.korul.hbbft.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.annotation.Nullable
import android.util.Log
import net.korul.hbbft.DatabaseApplication


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

        Log.i(TAG, "Service: start Init")

        DatabaseApplication.mCoreHBBFT.Free()

        Log.i(TAG, "Service: finish Init")
        // Destroy the service
        stopSelf()
    }
}