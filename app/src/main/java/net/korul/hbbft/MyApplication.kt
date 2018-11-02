package net.korul.hbbft

import android.app.Application
import android.util.Log
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle

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
            System.loadLibrary("hydra_android")
        } catch (e: UnsatisfiedLinkError) {

            val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            } else {
                AlertDialog.Builder(this)
            }
            builder.setTitle("Load libary ERROR")
                    .setMessage(e.message)
                    .setPositiveButton(android.R.string.yes) { dialog, which ->
                        dialog.cancel()
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()

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
