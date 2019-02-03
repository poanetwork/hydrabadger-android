package net.korul.hbbft.P2P

import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import io.nats.client.Connection
import io.nats.client.ConnectionFactory
import io.nats.client.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.SessionDescription
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantLock

class P2PMesh(private val applicationContext: Context, private val callback: IGetData) {
    private var TAG = "HYDRABADGERTAG:P2PMesh"

    var consNats: HashMap<String, Connection?> = hashMapOf()
    var mConnections: HashMap<String, Connections> = hashMapOf()

    var userName: MutableList<String?> = arrayListOf()
    var roomNameList: MutableList<String?> = arrayListOf()

    val DISPLAY_UI_TOAST = 0

    private val lock = ReentrantLock()

    var callbackNewUser: INewUserInCon? = null

    var handlerToast = Handler(Handler.Callback { msg ->
        when (msg.what) {
            DISPLAY_UI_TOAST -> run {
                try {
                    Toast.makeText(applicationContext, msg.obj as String, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            else -> {
            }
        }
        false
    })


    fun setNewUserCallback(callback: INewUserInCon) {
        callbackNewUser = callback
    }


    fun initOneMesh(roomName: String, UID: String) {
        try {
            Log.d(TAG, "P2PMesh initOneMesh $roomName - roomName, $UID - UID")

            roomNameList.add( roomName )
            userName.add( UID )
            consNats[UID] = initNatsSignalling(UID)

            val nats = consNats[UID]
            initNatsMeshInitiator(nats, UID, "users:Room:$roomName")

        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun publishAboutMe(roomName: String, UID: String) {
        val json = JSONObject()
        val message: String

        json.put("type", "addUser")
        json.put("user", UID)

        message = json.toString()

        consNats[UID]?.publish("users:Room:$roomName", message.toByteArray(StandardCharsets.UTF_8))
    }

    fun FreeConnect() {
        Log.d(TAG, "P2PMesh FreeConnect")

        try {
            lock.lock()
            for (con in mConnections.values) {
                con.FreeConnect()
            }
            lock.unlock()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

        for (roomName in roomNameList) {
            if(roomName.isNullOrEmpty())
                continue

            for (user in userName) {
                val json = JSONObject()
                val message: String

                json.put("type", "deleteUser")
                json.put("user", user)

                message = json.toString()

                consNats[user]?.publish("users:Room:$roomName", message.toByteArray(StandardCharsets.UTF_8))
            }
        }
    }

    fun initNatsMeshInitiator(nats: Connection?, UID: String, roomName: String) {
        nats!!.subscribe(roomName) { msg: Message? ->
            if (msg == null)
                return@subscribe

            val message = String(msg.data, StandardCharsets.UTF_8)
            val json2 = JSONObject(message)

            if (json2.getString("type") == "addUser") {
                val user = json2.getString("user")
                if( mConnections.contains(user))
                    return@subscribe
                if( user == UID )
                    return@subscribe

                if (!mConnections.contains(user)) {
                    callbackNewUser?.NewUser(user)
                    mConnections[ user ] = Connections(applicationContext, user, UID, consNats[UID]!!,true, callback)
                }
            }
            else if (json2.getString("type") == "deleteUser") {
                try {
                    val user = json2.getString("user")

                    lock.lock()
                    for(name_ in mConnections.keys) {
                        if(user == name_) {
                            mConnections[ user ]?.FreeConnect()
                            mConnections.remove(user)
                        }
                    }
                    lock.unlock()
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun initNatsSignalling(listenFrom: String): Connection {
        Log.d(TAG, "P2PMesh initNatsSignalling ${listenFrom} - listenFrom")

        val async = GlobalScope.async {
            try {
                val strUrls: MutableList<String> = arrayListOf()
                strUrls.add("nats://62.176.10.54:4222")
                strUrls.add("nats://108.61.190.95:4222")
                val conNats = ConnectionFactory(strUrls.toTypedArray()).createConnection()
                conNats
            }
            catch (e: Exception) {
                val msg = handlerToast.obtainMessage(DISPLAY_UI_TOAST)
                msg.obj = "Nats error - ${e.printStackTrace()}!"
                handlerToast.sendMessage(msg)

                null
            }
        }
        val conNats = runBlocking { async.await() }

        conNats!!.subscribe(listenFrom) { msg: Message? ->
            if(msg == null)
                return@subscribe

            val message = String(msg.data, StandardCharsets.UTF_8)

            val json2 = JSONObject(message)
            if(json2.getString("type") == "candidate") {
                val uid = json2.getString("UID")
                val candidate = IceCandidate(json2.getString("sdpMid"), json2.getInt("sdpMLineIndex"), json2.getString("candidate"))
                mConnections[uid]?.peerConnection?.addIceCandidate(candidate)
            }
            else {
                val uid = json2.getString("UID")

                val msg = handlerToast.obtainMessage(DISPLAY_UI_TOAST)
                msg.obj = "Message - set SDP to $uid"
                handlerToast.sendMessage(msg)

                val type = json2.getString("type")
                val sdp = json2.getString("sdp")

                val sdp2 = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp)
                if(type == "offer") {
                    if(!mConnections.contains(uid)) {
                        mConnections[ uid ] =  Connections(applicationContext, uid, listenFrom, consNats[listenFrom]!!, false, callback)
                    }

                    mConnections[uid]?.peerConnection?.setRemoteDescription(mConnections[uid]?.SessionObserver, sdp2)
                    val constraints = MediaConstraints()
                    mConnections[uid]?.peerConnection?.createAnswer(mConnections[uid]?.SessionObserver, constraints)
                }
                else if(type == "answer") {
                    mConnections[uid]?.peerConnection?.setRemoteDescription(mConnections[uid]?.SessionObserver, sdp2)
                }
            }
        }

        mConnections[listenFrom]?.conNats = conNats
        return conNats
    }
}