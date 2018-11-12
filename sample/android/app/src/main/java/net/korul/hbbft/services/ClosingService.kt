package net.korul.hbbft.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.annotation.Nullable
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import kotlin.concurrent.thread


class ClosingService : Service() {

    private var TAG = "ClosingService"

    var uniqueID1: String? = null
    var uniqueID2: String? = null
    var uniqueID3: String? = null

    @Nullable
    override fun onBind(intent: Intent): IBinder? {

        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand")

        uniqueID1 = intent?.getStringExtra("uniqueID1")
        uniqueID2 = intent?.getStringExtra("uniqueID2")
        uniqueID3 = intent?.getStringExtra("uniqueID3")

        Log.i(TAG, "uniqueID1: $uniqueID1")
        Log.i(TAG, "uniqueID2: $uniqueID2")
        Log.i(TAG, "uniqueID3: $uniqueID3")

        return super.onStartCommand(intent, flags, startId)
    }


    fun resetConnectOnServer(uniqueID: String) {
        try {
//            62.176.10.54
            Log.i(TAG, "Service: try connect")
            val soc = Socket("62.176.10.54", 49999)
            val dout = DataOutputStream(soc.getOutputStream())
            val din = DataInputStream(soc.getInputStream())
            Log.i(TAG, "Service: connected")

            val magick = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xCA.toByte(), 0xFE.toByte())
            val bytesLengthString = ByteBuffer.allocate(4).putInt(uniqueID.count()).array()
            val original = uniqueID
            val utf8Bytes = original.toByteArray(charset("UTF8"))

            dout.write(magick)
            dout.write(bytesLengthString, 0, 4)
            dout.write(utf8Bytes, 0, utf8Bytes.count())
            dout.flush()

            Log.i(TAG, "Service: write to socket")

            dout.close()
            din.close()
            soc.close()
        }
        catch (e:Exception){
            Log.i(TAG, "Service: not connected")
            e.printStackTrace()
        }
    }

    @SuppressLint("CheckResult")
    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)

        Log.i(TAG, "Service: start Init")

        if(uniqueID1 != null)
            thread {
                resetConnectOnServer(uniqueID1!!)
            }


        if(uniqueID2 != null)
            thread {
                resetConnectOnServer(uniqueID2!!)
            }


        if(uniqueID3 != null)
            thread {
                resetConnectOnServer(uniqueID3!!)
            }


        Log.i(TAG, "Service: finish Init")
        // Destroy the service
        stopSelf()
    }
}