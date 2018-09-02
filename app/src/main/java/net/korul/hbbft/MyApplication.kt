package net.korul.hbbft

import android.app.Application
import android.util.Log

/**
 * Created by korul on 16.03.17.
 */

class hbbft : Application() {
    var session: Session? = null
        private set

    init {
        sSelf = this
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate")
        super.onCreate()
        try {
            System.loadLibrary("mobcore")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Load libary ERROR: $e")
            return
        }

        session = Session()
    }

    companion object {
        private lateinit var sSelf: hbbft
        private val TAG = "Hydrabadger"

        fun get(): hbbft {
            return sSelf
        }
    }
}
