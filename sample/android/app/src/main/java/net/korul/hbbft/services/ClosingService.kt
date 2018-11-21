package net.korul.hbbft.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.annotation.Nullable
import android.util.Log
import net.korul.hbbft.server.util.ServerUtil.Companion.resetConnectOnServer
import kotlin.concurrent.thread


class ClosingService : Service() {

    private var TAG = "HYDRABADGERTAG:ClosingService"

    var uniqueID1: String? = null
    var uniqueID2: String? = null
    var uniqueID3: String? = null

    var server: String? = null

    @Nullable
    override fun onBind(intent: Intent): IBinder? {

        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")

        uniqueID1 = intent?.getStringExtra("uniqueID1")
        uniqueID2 = intent?.getStringExtra("uniqueID2")
        uniqueID3 = intent?.getStringExtra("uniqueID3")

        server = intent?.getStringExtra("server")

        Log.i(TAG, "uniqueID1: $uniqueID1")
        Log.i(TAG, "uniqueID2: $uniqueID2")
        Log.i(TAG, "uniqueID3: $uniqueID3")
        Log.i(TAG, "server:    $server")

        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("CheckResult")
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)

        Log.i(TAG, "Service: start Init")

        if(!server.isNullOrEmpty()) {
            if(uniqueID1 != null)
                thread {
                    resetConnectOnServer(uniqueID1!!, server!!)
                }


            if(uniqueID2 != null)
                thread {
                    resetConnectOnServer(uniqueID2!!, server!!)
                }


            if(uniqueID3 != null)
                thread {
                    resetConnectOnServer(uniqueID3!!, server!!)
                }
        }

        Log.i(TAG, "Service: finish Init")
        // Destroy the service
        stopSelf()
    }
}