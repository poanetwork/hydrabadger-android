package net.korul.hbbft.firebaseStorage

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.R
import java.io.File


class MyDownloadService : MyBaseTaskService() {

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

        if (ACTION_DOWNLOAD == intent.action && numOftask() == 0) {
            // Get the path to download from the intent
            val userid = intent.getStringExtra(EXTRA_DOWNLOAD_USERID)
            downloadFromPath(userid)
        }

        return Service.START_REDELIVER_INTENT
    }

    private fun downloadFromPath(userId: String) {
        Log.d(TAG, "downloadFromPath:$userId")

        // Mark task started
        taskStarted()
//        showProgressNotification(getString(R.string.syncing), 0, 0)

        val outputDir = this.cacheDir
        val localFile = File.createTempFile("avatar", "jpg", outputDir)
        storageRef.child("usersAvatars").child(userId).child("avatar.jpg").getFile(localFile).addOnSuccessListener {
            Log.d(TAG, "download:SUCCESS")

            // Create file metadata with property to delete
            val metadata = StorageMetadata.Builder()
                .setContentType(null)
                .build()
            // Delete the metadata property
            storageRef.child("usersAvatars").child(userId).child("avatar.jpg").updateMetadata(metadata)
                .addOnSuccessListener {
                }.addOnFailureListener {
                }

            // Send success broadcast with number of bytes downloaded
            broadcastDownloadFinished(localFile, localFile.length())
//            showDownloadFinishedNotification(localFile.length().toInt())

            // Mark task completed
            taskCompleted()
        }.addOnFailureListener { exception ->
            Log.w(TAG, "download:FAILURE", exception)

            // Send failure broadcast
            broadcastDownloadFinished(-1)
//            showDownloadFinishedNotification(-1)

            // Mark task completed
            taskCompleted()
        }
    }

    /**
     * Broadcast finished download (success or failure).
     * @return true if a running receiver received the broadcast.
     */
    private fun broadcastDownloadFinished(file: File, bytesDownloaded: Long): Boolean {
        val success = bytesDownloaded != -1L
        val action = if (success) DOWNLOAD_COMPLETED else DOWNLOAD_ERROR

        val broadcast = Intent(action)
            .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded)
            .putExtra(EXTRA_FILE_DOWNLOADED, file.path)
        return LocalBroadcastManager.getInstance(applicationContext)
            .sendBroadcast(broadcast)
    }

    private fun broadcastDownloadFinished(bytesDownloaded: Long): Boolean {
        val success = bytesDownloaded != -1L
        val action = if (success) DOWNLOAD_COMPLETED else DOWNLOAD_ERROR

        val broadcast = Intent(action)
            .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded)
        return LocalBroadcastManager.getInstance(applicationContext)
            .sendBroadcast(broadcast)
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

    companion object {

        private const val TAG = "Storage#DownloadService"

        /** Actions  */
        const val ACTION_DOWNLOAD = "action_download"
        const val DOWNLOAD_COMPLETED = "download_completed"
        const val DOWNLOAD_ERROR = "download_error"

        /** Extras  */
        const val EXTRA_DOWNLOAD_USERID = "extra_download_usserid"
        const val EXTRA_BYTES_DOWNLOADED = "extra_bytes_downloaded"
        const val EXTRA_FILE_DOWNLOADED = "extra_file_downloaded"

        val intentFilter: IntentFilter
            get() {
                val filter = IntentFilter()
                filter.addAction(DOWNLOAD_COMPLETED)
                filter.addAction(DOWNLOAD_ERROR)

                return filter
            }
    }
}
