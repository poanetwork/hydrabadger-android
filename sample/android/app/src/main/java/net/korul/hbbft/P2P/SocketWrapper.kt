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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

class SocketWrapper: INewUserInCon {
    private var TAG = "HYDRABADGERTAG:SocketWrapper"

    private var mP2PMesh: P2PMesh? = null

    private val mPseudoNotLocalThread: HashMap<String, Thread>  = hashMapOf()
    private val mLocalALoopThread:     HashMap<String, Thread>  = hashMapOf()
    private val mPseudoNotLocalSocket: HashMap<String, Socket?> = hashMapOf()
    private val mLocalALoopSocket:     HashMap<String, Socket?> = hashMapOf()

    private val mPseudoNotLocalSocketServer: HashMap<String, ServerSocket?> = hashMapOf()

    val clientsBusyPorts: HashMap<String, Int> = hashMapOf()
    var myLocalPort = 0

    var mAllStop = false

    private val lock = ReentrantLock()

    lateinit var mRoomName: String
    lateinit var myUID: String

    constructor(P2PMesh_: P2PMesh) {
        mP2PMesh = P2PMesh_
        mP2PMesh?.setNewUserCallback(this)
    }

    override fun NewUser(uid: String) {
        addUser(uid)
    }

    fun initSocketWrapper(roomName: String, myUID_: String) {
        Log.d(TAG, "SocketWrapper initSocketWrapper ${roomName} - roomName, ${myUID_} - myUID")

        myUID = myUID_
        mRoomName = roomName
        val users = mP2PMesh?.mDeepstreamClient?.record?.getList( "users:Room:$mRoomName")
        myLocalPort = 2000 + users!!.entries.indexOf(myUID)
        clientsBusyPorts[myUID] = myLocalPort

        val clientsUIDs =  users.entries
        for (uid in clientsUIDs) {
            if(uid == myUID)
                break

            addUser(uid)
        }
    }

    private fun addUser(uid: String) {
        val users = mP2PMesh?.mDeepstreamClient?.record?.getList( "users:Room:$mRoomName")
        val port = 2000 + users!!.entries.indexOf(uid)

        if(!clientsBusyPorts.values.contains(port)) {
            Log.d(TAG, "SocketWrapper addUser ${port} - port, ${uid} - uid")
            clientsBusyPorts[uid] = port
            startPseudoNotLocalSocketALoop(uid, port)
        }
    }

    fun sendReceivedDataToHydra(messageBytes: ByteArray) {
        val message = String(messageBytes, StandardCharsets.UTF_8)
        val json = JSONObject(message)
        val uid = json.getString("myUID")

        if(!mPseudoNotLocalSocketServer.contains(uid))
            addUser(uid)

        if(!mPseudoNotLocalSocket.containsKey(uid) && !mLocalALoopSocket.contains(uid))
            startLocalSocketALoop(uid)

        while (mLocalALoopSocket[uid] == null && mPseudoNotLocalSocket[uid] == null)
            Thread.sleep(10)

        Thread.sleep(50)

        lock.lock()
        if (mPseudoNotLocalSocket[uid] != null) {
            val socket = mPseudoNotLocalSocket[uid]
            val dout = socket?.getOutputStream()

            val bytesString = json.getString("bytes")
            val bytes = Base64.decode(bytesString, Base64.DEFAULT)

            dout?.write(bytes)
            dout?.flush()
        } else {
            val socket = mLocalALoopSocket[uid]
            val dout = socket?.getOutputStream()

            val bytesString = json.getString("bytes")
            val bytes = Base64.decode(bytesString, Base64.DEFAULT)

            dout?.write(bytes)
            dout?.flush()
        }
        lock.unlock()
    }

    fun startPseudoNotLocalSocketALoop(uid: String, port: Int) {
        mPseudoNotLocalThread[uid] = thread {
            mPseudoNotLocalSocketServer[uid] = ServerSocket(port)

            val clientSocket = mPseudoNotLocalSocketServer[uid]!!.accept()
            mPseudoNotLocalSocket[uid] = clientSocket
            while (!mAllStop) {
                val din = clientSocket.getInputStream()
                val bytesnum = din.available()
                if(bytesnum > 0) {
//                    lock.lock()
                    val bytes = ByteArray(bytesnum)
                    val reads = din.read(bytes, 0, bytesnum)

                    Log.d(TAG, "SocketWrapper reed from ALoop Socket ${reads} - bytes")
                    while (mP2PMesh!!.mConnections[uid]?.dataChannel?.state() != DataChannel.State.OPEN)
                        Thread.sleep(1)

                    if(mP2PMesh!!.mConnections[uid]?.dataChannel?.state() == DataChannel.State.OPEN) {
                        Log.d(TAG, "SocketWrapper send to user from mConnections ${reads} - bytes")

                        val json = JSONObject()
                        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                        json.put("bytes", base64String)
                        json.put("myUID", myUID)
                        val buffer = ByteBuffer.wrap(json.toString().toByteArray(StandardCharsets.UTF_8))

                        mP2PMesh!!.mConnections[uid]?.dataChannel?.send(DataChannel.Buffer(buffer, true))
                    }
//                    lock.unlock()
                }
                Thread.sleep(50)
            }
        }
    }

    fun startLocalSocketALoop(uid: String) {
        mLocalALoopThread[uid] = thread {
            mLocalALoopSocket[uid] = Socket("127.0.0.1", myLocalPort)
            while (!mAllStop) {
                val din = mLocalALoopSocket[uid]?.getInputStream()
                val bytesnum = din?.available()
                if (bytesnum!! > 0) {
//                    lock.lock()
                    val bytes = ByteArray(bytesnum)
                    val reads = din.read(bytes, 0, bytesnum)

                    Log.d(TAG, "SocketWrapper reed from ALoop Socket ${reads} - bytes")
                    while (mP2PMesh!!.mConnections[uid]?.dataChannel?.state() != DataChannel.State.OPEN)
                        Thread.sleep(1)

                    if (mP2PMesh!!.mConnections[uid]?.dataChannel?.state() == DataChannel.State.OPEN) {
                        Log.d(TAG, "SocketWrapper send to user from mConnections ${reads} - bytes")

                        val json = JSONObject()
                        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                        json.put("bytes", base64String)
                        json.put("myUID", myUID)
                        val buffer = ByteBuffer.wrap(json.toString().toByteArray(StandardCharsets.UTF_8))

                        mP2PMesh!!.mConnections[uid]?.dataChannel?.send(DataChannel.Buffer(buffer, true))
                    }
//                    lock.unlock()
                }
                Thread.sleep(50)
            }
        }
    }
}
