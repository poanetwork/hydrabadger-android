package net.korul.hbbft.CoreHBBFT

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.iid.FirebaseInstanceId
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.R
import org.json.JSONObject

object PushWork {

    fun registerForPush(applicationContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = applicationContext.getString(R.string.default_notification_channel_id)
            val channelName = applicationContext.getString(R.string.default_notification_channel_name)
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(CoreHBBFT.TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                // Log
                val msg = applicationContext.getString(R.string.msg_token_fmt, token)
                Log.d(CoreHBBFT.TAG, msg)

                DatabaseApplication.mToken = token.toString()
            })
    }

    fun preSendPushToStart(listOfUIDs: List<String>, text: String, title: String) {
        val currentUser = CoreHBBFT.mAuth.currentUser

        if (currentUser == null) {
            val latch = CoreHBBFT.authAnonymouslyInFirebase()
            latch.await()
        }

        for (uid in listOfUIDs)
            sendPushToTopic(uid, title, text)
    }

    fun sendPushToTopic(UIDtopic: String, title: String, text: String): Task<String> {
        val json = JSONObject()
        json.put("text", text)
        json.put("title", title)
        json.put("topic", UIDtopic)

        return CoreHBBFT.mFunctions
            .getHttpsCallable("sendToTopic")
            .call(json)
            .continueWith(object : Continuation<HttpsCallableResult, String> {
                override fun then(task: Task<HttpsCallableResult>): String {
                    return task.result?.data.toString()
                }
            })
    }
}