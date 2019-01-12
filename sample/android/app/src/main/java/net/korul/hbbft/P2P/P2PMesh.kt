package ru.hintsolutions.myapplication2

import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import io.deepstream.DeepstreamClient
import io.deepstream.ListChangedListener
import io.nats.client.Connection
import io.nats.client.ConnectionFactory
import io.nats.client.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.korul.hbbft.P2P.INewUserInCon
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.SessionDescription
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantLock

class P2PMesh(private val applicationContext: Context, private val callback: IGetData) {
    private var TAG = "HYDRABADGERTAG:P2PMesh"

    var mDeepstreamClient: DeepstreamClient? = null

    var consNats: HashMap<String, Connection?> = hashMapOf()
    var mConnections: HashMap<String, Connections> = hashMapOf()

    var userName: MutableList<String?> = arrayListOf()
    var roomNameList: MutableList<String?> = arrayListOf()

    var isInited = false
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

    init {
        isInited = initDeepStream()
    }

    fun setNewUserCallback(callback: INewUserInCon) {
        callbackNewUser = callback
    }

    private fun initDeepStream(): Boolean {
        try {
            Log.d(TAG, "P2PMesh initDeepStream")

            mDeepstreamClient = DeepstreamClient("62.176.10.54:6020/deepstream")
            val loginResult = mDeepstreamClient?.login()
            if(loginResult?.loggedIn() == true)
                return true
        }
        catch (e: Exception) {
            val msg = handlerToast.obtainMessage(DISPLAY_UI_TOAST)
            msg.obj = "DeepStream server not worked!"
            handlerToast.sendMessage(msg)

            e.printStackTrace()
        }
        return false
    }

    fun clearAllUsersFromDataBase(roomName: String) {
        val users = mDeepstreamClient?.record?.getList( "users:Room:$roomName")
        try {
            for (us in users!!.entries)
                users.removeEntry( us )
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initOneMesh(roomName: String, UID: String) {
        try {
            Log.d(TAG, "P2PMesh initOneMesh ${roomName} - roomName, ${UID} - UID")

            roomNameList.add( roomName )
            userName.add( UID )
            consNats[UID] = initNatsSignalling(UID)

            val users = mDeepstreamClient?.record?.getList( "users:Room:$roomName")
            users?.removeEntry(UID)
            users?.addEntry( UID )
            users?.subscribe { _: String?, users_: Array<out String>? ->
                if(users_ != null) {
                    for(user in users_) {
                        if( mConnections.contains(user))
                            continue
                        if( user == UID )
                            continue

                        if (!mConnections.contains(user)) {
                            callbackNewUser?.NewUser(user)
                            mConnections[ user ] = Connections(applicationContext, user, UID, consNats[UID]!!,true, callback)
                        }
                    }

                    try {
                        lock.lock()
                        for(name_ in mConnections.keys) {
                            if(!users_.contains( name_ )) {
                                mConnections[ name_ ]?.Free()
                                mConnections.remove(name_)
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
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun Free() {
        Log.d(TAG, "P2PMesh Free")

        try {
            lock.lock()
            for (con in mConnections.values) {
                con?.Free()
            }
            lock.unlock()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

        for (roomName in roomNameList) {
            if(roomName.isNullOrEmpty())
                continue

            val users = mDeepstreamClient?.record?.getList( "users:Room:$roomName")
            for (user in userName)
                users?.removeEntry( user )
        }
    }

    private fun initNatsSignalling(listenFrom: String): Connection {
        Log.d(TAG, "P2PMesh initNatsSignalling ${listenFrom} - listenFrom")

        val async = GlobalScope.async {
            try {
                val conNats = ConnectionFactory("nats://62.176.10.54:4222").createConnection()
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