package net.korul.hbbft.FirebaseStorageDU

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.*

class MyGetLastModificationRoomService : MyBaseTaskService() {
    // [START declare_ref]
    private lateinit var storageRef: StorageReference
    // [END declare_ref]

    override fun onCreate() {
        super.onCreate()

        // [START get_storage_ref]
        storageRef = FirebaseStorage.getInstance().reference
        // [END get_storage_ref]
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand:$intent:$startId")

        if (ACTION_COMPARE == intent.action) {
            // Get the path to download from the intent
            val roomuid = intent.getStringExtra(EXTRA_COMPARE_UID)
            compareFromPath(roomuid)
        }

        return Service.START_REDELIVER_INTENT
    }

    private fun compareFromPath(roomuid: String) {
        // Mark task started
        taskStarted()

        storageRef.child("roomsAvatars").child(roomuid).child("avatar.png").metadata.addOnSuccessListener {
            val lastUpadte = it.updatedTimeMillis
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = lastUpadte
            val date = calendar.time

            // Send failure broadcast
            broadcastCompareFinished(roomuid, date)

            // Mark task completed
            taskCompleted()
        }.addOnFailureListener {
            Log.w(TAG, "download:FAILURE", it)

            if (it.message == "Object does not exist at location.") {
                val lastUpadte = 0L
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = lastUpadte
                val date = calendar.time

                // Send failure broadcast
                broadcastCompareFinished(roomuid, date)
                // Mark task completed
                taskCompleted()
            } else {
                // Send failure broadcast
                broadcastCompareFinished(roomuid, null)

                // Mark task completed
                taskCompleted()
            }
        }
    }

    /**
     * Broadcast finished compare (success or failure).
     * @return true if a running receiver received the broadcast.
     */
    @SuppressLint("SimpleDateFormat")
    private fun broadcastCompareFinished(roomuid: String, dateModication: Date?): Boolean {
        val success = dateModication != null

        val action = if (success) COMPARE_COMPLETED else COMPARE_ERROR

        return if (!success) {
            val broadcast = Intent(action)
            LocalBroadcastManager.getInstance(applicationContext)
                .sendBroadcast(broadcast)
        } else {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = formatter.format(dateModication)

            val broadcast = Intent(action)
                .putExtra(EXTRA_COMPARE_DATE, date)
                .putExtra(EXTRA_COMPARE_UID, roomuid)
            LocalBroadcastManager.getInstance(applicationContext)
                .sendBroadcast(broadcast)
        }
    }

    companion object {
        private const val TAG = "HYDRA:COMPAREService"

        /** Actions  */
        const val ACTION_COMPARE = "action_room_compare"
        const val COMPARE_COMPLETED = "compare_room_completed"
        const val COMPARE_ERROR = "compare_room_error"

        /** Extras  */
        const val EXTRA_COMPARE_UID = "extra_room_compare_uid"

        const val EXTRA_COMPARE_DATE = "extra_room_compare_date"

        val intentFilter: IntentFilter
            get() {
                val filter = IntentFilter()
                filter.addAction(COMPARE_COMPLETED)
                filter.addAction(COMPARE_ERROR)

                return filter
            }
    }
}