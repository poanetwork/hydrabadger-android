package net.korul.hbbft.P2P

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import org.webrtc.DataChannel
import java.math.BigInteger
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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

    lateinit var mRoomName: String
    lateinit var myUID: String

    constructor(P2PMesh_: P2PMesh) {
        mP2PMesh = P2PMesh_
        mP2PMesh?.setNewUserCallback(this)
    }

    override fun NewUser(uid: String) {
        addUser(uid)
    }

    fun initSocketWrapper(roomName: String, myUID_: String, users: List<String>) {
        Log.d(TAG, "SocketWrapper initSocketWrapper $roomName - roomName, $myUID_ - myUID")

        myUID = myUID_
        mRoomName = roomName
        var port: BigInteger
        try {
            val digest = MessageDigest.getInstance("SHA1")
            digest.update(myUID_.toByteArray())
            val output = digest.digest()
            val hex = output.joinToString("") { String.format("%02X", (it.toInt() and 0xFF)) }

            port = hex.toBigInteger(16)
        } catch (e: Exception) {
            port = 2000.toBigInteger()
            println("SHA1 not implemented in this system")
        }

        var port1 = (2000 + CongruentPseudoGen(port))
        while (clientsBusyPorts.values.contains(port1)) {
            port1++
        }
        myLocalPort = port1
        clientsBusyPorts[myUID] = myLocalPort

        for (uid in users) {
            if(uid == myUID)
                break
            addUser(uid)
        }
    }

    private fun CongruentPseudoGen(x: BigInteger): Int {
        val a: BigInteger = 1664525.toBigInteger()
        val c: BigInteger  = 1013904223.toBigInteger()
        val m: BigInteger  = (Math.pow(2.0, 15.0)-1).toInt().toBigInteger()

        return (((a * x) + c)%m).toInt()
    }

    private fun addUser(uid: String) {
        var port: BigInteger
        try {
            val digest = MessageDigest.getInstance("SHA1")
            digest.update(uid.toByteArray())
            val output = digest.digest()
            val hex = output.joinToString("") { String.format("%02X", (it.toInt() and 0xFF)) }

            port = hex.toBigInteger(16)
        } catch (e: Exception) {
            port = 2000.toBigInteger()
            println("SHA1 not implemented in this system")
        }

        var port1 = 2000 + CongruentPseudoGen(port)
        while (clientsBusyPorts.values.contains(port1)) {
            port1++
        }

        clientsBusyPorts[uid] = port1
        Log.d(TAG, "SocketWrapper addUser $port1 - port, $uid - uid")
        startPseudoNotLocalSocketALoop(uid, port1)
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
                    val bytes = ByteArray(bytesnum)
                    val reads = din.read(bytes, 0, bytesnum)

                    Log.d(TAG, "SocketWrapper reed from ALoop Socket $reads - bytes")
                    while (mP2PMesh!!.mConnections[uid]?.dataChannel?.state() != DataChannel.State.OPEN)
                        Thread.sleep(10)

                    Log.d(TAG, "SocketWrapper send to user from mConnections $reads - bytes")
                    val json = JSONObject()
                    val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                    json.put("bytes", base64String)
                    json.put("myUID", myUID)
                    val buffer = ByteBuffer.wrap(json.toString().toByteArray(StandardCharsets.UTF_8))

                    mP2PMesh!!.mConnections[uid]?.dataChannel?.send(DataChannel.Buffer(buffer, true))
                }
                else
                    Thread.sleep(10)
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
                    val bytes = ByteArray(bytesnum)
                    val reads = din.read(bytes, 0, bytesnum)

                    Log.d(TAG, "SocketWrapper reed from ALoop Socket $reads - bytes")
                    while (mP2PMesh!!.mConnections[uid]?.dataChannel?.state() != DataChannel.State.OPEN)
                        Thread.sleep(1)

                    Log.d(TAG, "SocketWrapper send to user from mConnections $reads - bytes")
                    val json = JSONObject()
                    val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                    json.put("bytes", base64String)
                    json.put("myUID", myUID)
                    val buffer = ByteBuffer.wrap(json.toString().toByteArray(StandardCharsets.UTF_8))

                    mP2PMesh!!.mConnections[uid]?.dataChannel?.send(DataChannel.Buffer(buffer, true))
                }
                else
                    Thread.sleep(10)
            }
        }
    }
}
