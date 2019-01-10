package ru.hintsolutions.myapplication2

import android.util.Base64
import android.util.Log
import net.korul.hbbft.P2P.INewUserInCon
import org.json.JSONObject
import org.webrtc.DataChannel
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

class SocketWrapper: INewUserInCon {

    private var TAG = "HYDRABADGERTAG:SocketWrapper"

    private var mP2PMesh: P2PMesh? = null

    val HashMapThread: HashMap<String, Thread> = hashMapOf()
    val HashMapThread1: HashMap<String, Thread> = hashMapOf()
    val HashMapSocketServer: HashMap<String, ServerSocket?> = hashMapOf()
    val HashMapSocket: HashMap<String, Socket?> = hashMapOf()
    val HashMapSocket1: HashMap<String, Socket?> = hashMapOf()

    val clients: HashMap<String, Int> = hashMapOf()

    var mPortLoc = 0

    val mAllStop = false
    var mRoomName: String = ""

    lateinit var myUID: String

    constructor(P2PMesh_: P2PMesh) {
        mP2PMesh = P2PMesh_

        mP2PMesh?.setNewUserCallback(this)
    }

    override fun NewUser(port: Int, uid: String) {
        addUser(port, uid)
    }

    fun initSocketWrapper(roomName: String, myUID_: String) {
        myUID = myUID_
        Log.d(TAG, "SocketWrapper initSocketWrapper ${roomName} - roomName, ${myUID} - myUID")
        mRoomName = roomName
        val users = mP2PMesh?.mDeepstreamClient?.record?.getList( "users:Room:$mRoomName")
        mPortLoc = 2000 + users!!.entries.indexOf(myUID)

        clients[myUID] = mPortLoc

        val clientsUIDs =  users.entries
        for (uid in clientsUIDs) {
            if(uid == myUID)
                break

            val port = 2000 + users.entries.indexOf(uid)
            addUser(port, uid)
        }
    }

    private fun addUser(port: Int, uid: String) {
        if(!clients.values.contains(port)) {
            Log.d(TAG, "SocketWrapper addUser ${port} - port, ${uid} - uid")
            clients[uid] = port

            HashMapThread[uid] = thread {
                HashMapSocketServer[uid] = ServerSocket(port)

                val clientSocket = HashMapSocketServer[uid]!!.accept()
                HashMapSocket[uid] = clientSocket
                while (!mAllStop) {
                    val din = clientSocket.getInputStream()
                    val bytesnum = din.available()
                    if(bytesnum > 0) {
//                        lock.lock()
                        val bytes = ByteArray(bytesnum)
                        val reads = din.read(bytes, 0, bytesnum)

                        Log.d(TAG, "SocketWrapper clientSocket read ${reads} - bytes")

                        while (mP2PMesh!!.mConnections[uid]?.dataChannel?.state() != DataChannel.State.OPEN)
                            Thread.sleep(1)

                        if(mP2PMesh!!.mConnections[uid]?.dataChannel?.state() == DataChannel.State.OPEN) {
                            Log.d(TAG, "SocketWrapper mConnections send ${reads} - bytes")

                            val json = JSONObject()
                            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                            json.put("bytes", base64String)
                            json.put("myUID", myUID)
                            val buffer = ByteBuffer.wrap(json.toString().toByteArray(StandardCharsets.UTF_8))

                            mP2PMesh!!.mConnections[uid]?.dataChannel?.send(DataChannel.Buffer(buffer, true))
                        }
//                        lock.unlock()
                    }
                    Thread.sleep(10)
                }
            }
        }
    }

    fun sendToLocal(receiveFrom: String, bytes: ByteArray) {
        Log.d(TAG, "SocketWrapper sendToLocal ${receiveFrom} - receiveFrom")

        val message = String(bytes, StandardCharsets.UTF_8)
        val json = JSONObject(message)

        val uid = json.getString("myUID")
        if(!HashMapSocketServer.contains(uid)) {
            val users = mP2PMesh?.mDeepstreamClient?.record?.getList( "users:Room:$mRoomName")

            val port = 2000 + users!!.entries.indexOf(uid)
            addUser(port, uid)
        }

        if(!HashMapSocket.containsKey(uid) && !HashMapSocket1.contains(uid)) {
            HashMapThread1[uid] = thread {
                HashMapSocket1[uid] = Socket("127.0.0.1", mPortLoc)
                while (!mAllStop) {
                    val din = HashMapSocket1[uid]?.getInputStream()
                    val bytesnum = din?.available()
                    if (bytesnum!! > 0) {
//                        lock.lock()
                        val bytes = ByteArray(bytesnum)
                        val reads = din.read(bytes, 0, bytesnum)

                        Log.d(TAG, "SocketWrapper clientSocket read ${reads} - bytes")

                        while (mP2PMesh!!.mConnections[uid]?.dataChannel?.state() != DataChannel.State.OPEN)
                            Thread.sleep(1)

                        if (mP2PMesh!!.mConnections[uid]?.dataChannel?.state() == DataChannel.State.OPEN) {
                            Log.d(TAG, "SocketWrapper mConnections send ${reads} - bytes")

                            val json = JSONObject()
                            val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                            json.put("bytes", base64String)
                            json.put("myUID", myUID)
                            val buffer = ByteBuffer.wrap(json.toString().toByteArray(StandardCharsets.UTF_8))

                            mP2PMesh!!.mConnections[uid]?.dataChannel?.send(DataChannel.Buffer(buffer, true))
                        }
//                        lock.unlock()
                    }
                    Thread.sleep(10)
                }
            }
        }

        while (HashMapSocket1[uid] == null && HashMapSocket[uid] == null)
            Thread.sleep(10)

//        lock.lock()
        if (HashMapSocket[uid] != null) {
            val socket = HashMapSocket[uid]
            val dout = socket?.getOutputStream()

            val bytesString = json.getString("bytes")
            val bytes = Base64.decode(bytesString, Base64.DEFAULT)

            dout?.write(bytes)
            dout?.flush()
        }
        else {
            val socket = HashMapSocket1[uid]
            val dout = socket?.getOutputStream()

            val bytesString = json.getString("bytes")
            val bytes = Base64.decode(bytesString, Base64.DEFAULT)

            dout?.write(bytes)
            dout?.flush()
        }
//        lock.unlock()
    }
}
