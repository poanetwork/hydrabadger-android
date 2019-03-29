package net.korul.hbbft.P2P

import android.content.Context
import android.os.Handler
import android.util.Log
import io.nats.client.Connection
import io.nats.client.ConnectionFactory
import io.nats.client.Message
import io.nats.client.Subscription
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.korul.hbbft.CommonData.utils.AppUtils
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.SessionDescription
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantLock

class P2PMesh(private val applicationContext: Context, private val callback: IGetData) {
    private var TAG = "HYDRA:P2PMesh"

    var consNats: HashMap<String, Connection?> = hashMapOf()
    var mConnections: HashMap<Pair<String, String>, Connections> = hashMapOf()

    var userName: MutableList<String?> = arrayListOf()
    var roomIdList: MutableList<String?> = arrayListOf()

    var usersCon: MutableList<String> = arrayListOf()

    var listOfSub: MutableList<Subscription> = arrayListOf()

    val DISPLAY_UI_TOAST = 0

    private val lock = ReentrantLock()

    var handlerToast = Handler(Handler.Callback { msg ->
        when (msg.what) {
            DISPLAY_UI_TOAST -> run {
                try {
                    AppUtils.showToast(
                        applicationContext,
                        msg.obj as String, false
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            else -> {
            }
        }
        false
    })


    fun initOneMesh(roomId: String, UID: String) {
        try {
            Log.d(TAG, "P2PMesh initOneMesh $roomId - roomId, $UID - UID")

            roomIdList.add(roomId)
            userName.add(UID)
            consNats[UID] = initNatsSignalling(UID)
            initNatsMeshInitiator(consNats[UID], UID, "users:Room:$roomId")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun publishAboutMe(roomId: String, UID: String) {
        val json = JSONObject()
        json.put("type", "addUser")
        json.put("dialog", UID)

        val message = json.toString()

        consNats[UID]?.publish("users:Room:$roomId", message.toByteArray(StandardCharsets.UTF_8))
    }

    fun FreeConnect() {
        Log.d(TAG, "P2PMesh FreeConnect")

        try {
            lock.lock()
            for (con in mConnections.values) {
                con.FreeConnect()
            }
            lock.unlock()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        for (sub in listOfSub) {
            try {
                sub.unsubscribe()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        for (roomId in roomIdList) {
            if (roomId.isNullOrEmpty())
                continue

            for (user in userName) {
                val json = JSONObject()
                val message: String

                json.put("type", "deleteUser")
                json.put("dialog", user)

                message = json.toString()

                consNats[user]?.publish("users:Room:$roomId", message.toByteArray(StandardCharsets.UTF_8))
            }
        }
    }

    fun initNatsMeshInitiator(nats: Connection?, UID: String, roomId: String) {
        val sub = nats!!.subscribe(roomId) { msg: Message? ->
            if (msg == null)
                return@subscribe

            val message = String(msg.data, StandardCharsets.UTF_8)
            val json2 = JSONObject(message)

            if (json2.getString("type") == "addUser") {
                val user = json2.getString("dialog")
                val myUid = UID
                val pair: Pair<String, String> = Pair(user, myUid)
                val pair2: Pair<String, String> = Pair(myUid, user)
                if (user == UID)
                    return@subscribe
                if (mConnections.keys.contains(pair) || mConnections.keys.contains(pair2))
                    return@subscribe

                if (!mConnections.keys.contains(pair) && !mConnections.keys.contains(pair2)) {
                    if (!usersCon.contains(user))
                        usersCon.add(user)
                    mConnections[pair] = Connections(applicationContext, user, UID, consNats[UID]!!, true, callback)
                }
            } else if (json2.getString("type") == "deleteUser") {
                try {
                    val user = json2.getString("dialog")
                    val myUid = UID

                    val pair: Pair<String, String> = Pair(user, myUid)
                    val pair2: Pair<String, String> = Pair(myUid, user)
                    lock.lock()
                    if (mConnections.keys.contains(pair)) {
                        mConnections[pair]?.FreeConnect()
                        mConnections.remove(pair)
                    }
                    if (mConnections.keys.contains(pair2)) {
                        mConnections[pair2]?.FreeConnect()
                        mConnections.remove(pair2)
                    }
                    lock.unlock()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        listOfSub.add(sub)
    }

    private fun initNatsSignalling(listenFrom: String): Connection {
        Log.d(TAG, "P2PMesh initNatsSignalling $listenFrom - listenFrom")

        val async = GlobalScope.async {
            try {
                val strUrls: MutableList<String> = arrayListOf()
                strUrls.add("nats://62.176.10.54:4222")
                strUrls.add("nats://108.61.190.95:4222")
                val conNats = ConnectionFactory(strUrls.toTypedArray()).createConnection()
                conNats
            } catch (e: Exception) {
                val msg = handlerToast.obtainMessage(DISPLAY_UI_TOAST)
                msg.obj = "Nats error - ${e.printStackTrace()}!"
                handlerToast.sendMessage(msg)

                null
            }
        }
        val conNats = runBlocking { async.await() }

        val sub = conNats!!.subscribe(listenFrom) { msg: Message? ->
            if (msg == null)
                return@subscribe

            val message = String(msg.data, StandardCharsets.UTF_8)

            val json2 = JSONObject(message)
            val uid = json2.getString("UID")
            val toUser = json2.getString("toUser")
            val pair: Pair<String, String> = Pair(uid, toUser)
            val pair2: Pair<String, String> = Pair(toUser, uid)

            if (json2.getString("type") == "candidate") {
                val candidate =
                    IceCandidate(json2.getString("sdpMid"), json2.getInt("sdpMLineIndex"), json2.getString("candidate"))
                if (mConnections.keys.contains(pair)) {
                    mConnections[pair]?.peerConnection?.addIceCandidate(candidate)
                } else if (mConnections.keys.contains(pair2)) {
                    mConnections[pair2]?.peerConnection?.addIceCandidate(candidate)
                }
            } else {
                val msg = handlerToast.obtainMessage(DISPLAY_UI_TOAST)
                msg.obj = "Message - set SDP to $uid"
                handlerToast.sendMessage(msg)

                val type = json2.getString("type")
                val sdp = json2.getString("sdp")

                val sdp2 = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp)
                if (type == "offer") {
                    if (!mConnections.keys.contains(pair) && !mConnections.keys.contains(pair2)) {
                        if (!usersCon.contains(uid))
                            usersCon.add(uid)
                        mConnections[pair] =
                            Connections(applicationContext, uid, listenFrom, consNats[listenFrom]!!, false, callback)
                    }

                    if (mConnections.keys.contains(pair)) {
                        mConnections[pair]?.peerConnection?.setRemoteDescription(
                            mConnections[pair]?.SessionObserver,
                            sdp2
                        )
                        val constraints = MediaConstraints()
                        mConnections[pair]?.peerConnection?.createAnswer(
                            mConnections[pair]?.SessionObserver,
                            constraints
                        )
                    } else if (mConnections.keys.contains(pair2)) {
                        mConnections[pair2]?.peerConnection?.setRemoteDescription(
                            mConnections[pair2]?.SessionObserver,
                            sdp2
                        )
                        val constraints = MediaConstraints()
                        mConnections[pair2]?.peerConnection?.createAnswer(
                            mConnections[pair2]?.SessionObserver,
                            constraints
                        )
                    }
                } else if (type == "answer") {
                    if (mConnections.keys.contains(pair)) {
                        mConnections[pair]?.peerConnection?.setRemoteDescription(
                            mConnections[pair]?.SessionObserver,
                            sdp2
                        )
                    } else if (mConnections.keys.contains(pair2)) {
                        mConnections[pair2]?.peerConnection?.setRemoteDescription(
                            mConnections[pair2]?.SessionObserver,
                            sdp2
                        )
                    }
                }
            }
        }

        listOfSub.add(sub)

        return conNats
    }
}