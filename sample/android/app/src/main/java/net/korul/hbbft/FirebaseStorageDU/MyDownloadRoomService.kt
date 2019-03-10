package net.korul.hbbft.FirebaseStorageDU

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.R
import java.io.File


class MyDownloadRoomService : MyBaseTaskService() {

    private lateinit var storageRef: StorageReference

    override fun onCreate() {
        super.onCreate()

        // Initialize Storage
        storageRef = FirebaseStorage.getInstance().reference
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand:$intent:$startId")

        if (ACTION_DOWNLOAD == intent.action) {
            // Get the path to download from the intent
            val dialogid = intent.getStringExtra(EXTRA_DOWNLOAD_DIALOGID)
            downloadFromPath(dialogid)
        }

        return Service.START_REDELIVER_INTENT
    }

    private fun downloadFromPath(dialogId: String) {
        Log.d(TAG, "downloadFromPath:$dialogId")

        // Mark task started
        taskStarted()
        // showProgressNotification(getString(R.string.syncing), 0, 0)

        val outputDir = this.filesDir
        val localFile = File.createTempFile(dialogId, "png", outputDir)
        val avatarFile = File(outputDir.path + File.separator + dialogId + ".png")

        storageRef.child("roomsAvatars").child(dialogId).child("avatar.png").getFile(localFile).addOnSuccessListener {
            Log.d(TAG, "download:SUCCESS")

            localFile.copyTo(avatarFile, true)
            localFile.delete()
            // Send success broadcast with number of bytes downloaded
            broadcastDownloadFinished(avatarFile, avatarFile.length(), dialogId)
            // showDownloadFinishedNotification(localFile.length().toInt())

            // Mark task completed
            taskCompleted()
        }.addOnFailureListener { exception ->
            if (exception.toString().contains("com.google.firebase.storage.StorageException: Object does not exist at location.")) {
                Log.w(TAG, "download: " + getString(R.string.avatar_file_not_found) + exception.toString())
                broadcastDownloadFinished(-2L)
            } else {
                Log.w(TAG, "download:FAILURE", exception)
                // Send failure broadcast
                broadcastDownloadFinished(-1L)
                // showDownloadFinishedNotification(-1)
            }

            // Mark task completed
            taskCompleted()
        }
    }

    /**
     * Broadcast finished download (success or failure).
     * @return true if a running receiver received the broadcast.
     */
    private fun broadcastDownloadFinished(file: File, bytesDownloaded: Long, dialogId: String): Boolean {
        val success = bytesDownloaded != -1L
        val action = if (success) DOWNLOAD_COMPLETED else DOWNLOAD_ERROR

        val broadcast = Intent(action)
            .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded)
            .putExtra(EXTRA_FILE_DOWNLOADED, file.path)
            .putExtra(EXTRA_UID_DOWNLOADED, dialogId)
        return LocalBroadcastManager.getInstance(applicationContext)
            .sendBroadcast(broadcast)
    }

    private fun broadcastDownloadFinished(bytesDownloaded: Long): Boolean {
        if (bytesDownloaded == -2L) {
            val action = DOWNLOAD_FILE_NOT_FOUND

            val broadcast = Intent(action)
            return LocalBroadcastManager.getInstance(this)
                .sendBroadcast(broadcast)
        } else {
            val success = bytesDownloaded != -1L
            val action = if (success) DOWNLOAD_COMPLETED else DOWNLOAD_ERROR

            val broadcast = Intent(action)
                .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded)
            return LocalBroadcastManager.getInstance(this)
                .sendBroadcast(broadcast)
        }
    }

    companion object {

        private const val TAG = "Storage#DownloadService"

        /** Actions  */
        const val ACTION_DOWNLOAD = "action_room_download"
        const val DOWNLOAD_COMPLETED = "download_room_completed"
        const val DOWNLOAD_ERROR = "download_room_error"
        const val DOWNLOAD_FILE_NOT_FOUND = "download_room_file_not_found"

        /** Extras  */
        const val EXTRA_DOWNLOAD_DIALOGID = "extra_room_download_dialogid"
        const val EXTRA_BYTES_DOWNLOADED = "extra_room_bytes_downloaded"
        const val EXTRA_FILE_DOWNLOADED = "extra_room_file_downloaded"
        const val EXTRA_UID_DOWNLOADED = "extra_room_uid_downloaded"

        val intentFilter: IntentFilter
            get() {
                val filter = IntentFilter()
                filter.addAction(DOWNLOAD_COMPLETED)
                filter.addAction(DOWNLOAD_ERROR)

                return filter
            }
    }

    /**
     * Show a notification for a finished download.
     */
    private fun showDownloadFinishedNotification(bytesDownloaded: Int) {
        // Hide the progress notification
        dismissProgressNotification()

        // Make Intent to MainActivity
        val intent = Intent(this, DatabaseApplication::class.java)
            .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val success = bytesDownloaded != -1
        val caption = if (success) {
            getString(R.string.syncing_completed_well)
        } else {
            getString(R.string.syncing_error)
        }

        showFinishedNotification(caption, intent, true)
    }
}
