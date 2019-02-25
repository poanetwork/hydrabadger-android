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

class SocketWrapper {
    private var TAG = "HYDRABADGERTAG:SocketWrapper"

    private var mP2PMesh: P2PMesh? = null

    private val mPseudoNotLocalThread: HashMap<String, Thread> = hashMapOf()
    private val mLocalALoopThread: HashMap<String, Thread> = hashMapOf()
    private val mLocalALoopThread2: HashMap<String, Thread> = hashMapOf()

    private val mPseudoNotLocalSocketServer: HashMap<String, ServerSocket?> = hashMapOf()
    private val mPseudoNotLocalSocket: HashMap<Pair<String, String>, Socket?> = hashMapOf()
    private val mLocalALoopSocket: HashMap<String, Socket?> = hashMapOf()
    private val mLocalALoopSocket2: HashMap<String, Socket?> = hashMapOf()


    val clientsBusyPorts: HashMap<String, Int> = hashMapOf()
    var myLocalPort1 = 0
    var myLocalPort2 = 0

    var mAllStop = false
    var mStarted = false

    lateinit var mRoomName: String
    lateinit var myUID1: String
    lateinit var myUID2: String

    constructor(p_: P2PMesh) {
        mP2PMesh = p_
    }

    fun initSocketWrapper(roomName: String, myUID_: String, users: List<String>) {
        Log.d(TAG, "SocketWrapper initSocketWrapper2X $roomName - roomName, $myUID_ - myUID")

        myUID1 = myUID_
        mRoomName = roomName

        myLocalPort1 = getPortForUID(myUID1)

        for (uid in users) {
            if (uid == myUID1)
                continue
            addUser(uid)
        }
        mStarted = true
    }

    fun initSocketWrapper2X(roomName: String, myUID_: String, myUID_2: String, users: List<String>) {
        Log.d(TAG, "SocketWrapper initSocketWrapper2X $roomName - roomName, $myUID_ - myUID")

        myUID1 = myUID_
        myUID2 = myUID_2
        mRoomName = roomName

        myLocalPort1 = getPortForUID(myUID1)
        myLocalPort2 = getPortForUID(myUID2)

        for (uid in users) {
            if (uid == myUID1 || uid == myUID2)
                continue
            addUser(uid)
        }
        mStarted = true
    }

    private fun getPortForUID(myUID: String): Int {
        var port: BigInteger
        var port1: Int
        try {
            val digest = MessageDigest.getInstance("SHA1")
            digest.update(myUID.toByteArray())
            val output = digest.digest()
            val hex = output.joinToString("") { String.format("%02X", (it.toInt() and 0xFF)) }

            port = hex.toBigInteger(16)
        } catch (e: Exception) {
            port = 2000.toBigInteger()
            println("SHA1 not implemented in this system")
        }

        port1 = (2000 + CongruentPseudoGen(port))
        while (clientsBusyPorts.values.contains(port1)) {
            port1++
        }
        clientsBusyPorts[myUID] = port1
        return port1
    }

    private fun CongruentPseudoGen(x: BigInteger): Int {
        val a: BigInteger = 1664525.toBigInteger()
        val c: BigInteger = 1013904223.toBigInteger()
        val m: BigInteger = (Math.pow(2.0, 15.0) - 1).toInt().toBigInteger()

        return (((a * x) + c) % m).toInt()
    }

    private fun addUser(uid: String) {
        startPseudoNotLocalSocketALoop(uid, getPortForUID(uid))
    }

    fun sendReceivedDataToHydra(messageBytes: ByteArray) {
        val message = String(messageBytes, StandardCharsets.UTF_8)
        val json = JSONObject(message)

        val uid = json.getString("myUID")
        val touid = json.getString("toUID")

        val pair: Pair<String, String> = Pair(touid, uid)


        if (touid == myUID1) {
            if (!mPseudoNotLocalSocket.containsKey(pair) && !mLocalALoopSocket.contains(uid))
                startLocalSocketALoop(myLocalPort1, uid, myUID1, mLocalALoopThread, mLocalALoopSocket)

            flushToLocalHydra(pair, uid, mPseudoNotLocalSocket, mLocalALoopSocket, json)

        } else if (touid == myUID2) {
            if (!mPseudoNotLocalSocket.containsKey(pair) && !mLocalALoopSocket2.contains(uid))
                startLocalSocketALoop(myLocalPort2, uid, myUID2, mLocalALoopThread2, mLocalALoopSocket2)

            flushToLocalHydra(pair, uid, mPseudoNotLocalSocket, mLocalALoopSocket2, json)
        }
    }

    fun flushToLocalHydra(
        pair: Pair<String, String>, uid: String,
        PseudoNotLocalSocket: HashMap<Pair<String, String>, Socket?>,
        LocalALoopSocket: HashMap<String, Socket?>,
        json: JSONObject
    ) {
        while (!mAllStop && LocalALoopSocket[uid] == null && PseudoNotLocalSocket[pair] == null)
            Thread.sleep(10)

        if (PseudoNotLocalSocket[pair] != null) {
            val socket = PseudoNotLocalSocket[pair]
            val dout = socket?.getOutputStream()

            val bytesString = json.getString("bytes")
            val bytes = Base64.decode(bytesString, Base64.DEFAULT)

            Log.d(TAG, "SocketWrapper write to PseudoNotLocalSocket ${bytes.size} - bytes; pair - $pair")

            dout?.write(bytes)
            dout?.flush()
        } else {
            val socket = LocalALoopSocket[uid]
            val dout = socket?.getOutputStream()

            val bytesString = json.getString("bytes")
            val bytes = Base64.decode(bytesString, Base64.DEFAULT)

            Log.d(TAG, "SocketWrapper write to LocalALoopSocket ${bytes.size} - bytes; pair - $pair")

            dout?.write(bytes)
            dout?.flush()
        }
    }

    fun startPseudoNotLocalSocketALoop(uid: String, port: Int) {
        Log.d(TAG, "SocketWrapper startPseudoNotLocalSocketALoop new user - $uid ; port - $port")

        mPseudoNotLocalThread[uid] = thread {
            mPseudoNotLocalSocketServer[uid] = ServerSocket(port)

            while (!mAllStop) {
                val clientSocket = mPseudoNotLocalSocketServer[uid]!!.accept()
                Log.d(TAG, "SocketWrapper startPseudoNotLocalSocketALoop new connect from $clientSocket")

                thread {
                    while (!mAllStop) {
                        var din = clientSocket.getInputStream()
                        var bytesnum = din.available()
                        if (bytesnum > 0) {
                            var bytes = ByteArray(bytesnum)
                            var reads = din.read(bytes, 0, bytesnum)
                            Log.d(TAG, "SocketWrapper reed from startPseudoNotLocalSocketALoop Socket $reads - bytes")

                            val uid_ = getUIDFromHelloRequest(bytes)
                            val pair2: Pair<String, String> = Pair(uid_, uid)
                            mPseudoNotLocalSocket[pair2] = clientSocket

                            sendBytesToDataChannel(uid, uid_, bytes)

                            while (!mAllStop) {
                                din = mPseudoNotLocalSocket[pair2]!!.getInputStream()
                                bytesnum = din.available()

                                if (bytesnum > 0) {
                                    bytes = ByteArray(bytesnum)
                                    reads = din.read(bytes, 0, bytesnum)
                                    Log.d(
                                        TAG,
                                        "SocketWrapper reed from startPseudoNotLocalSocketALoop Socket $reads - bytes"
                                    )

                                    sendBytesToDataChannel(uid, uid_, bytes)
                                } else
                                    Thread.sleep(10)
                            }
                            break
                        } else
                            Thread.sleep(10)
                    }
                }
            }
        }
    }

    fun getUIDFromHelloRequest(bytes: ByteArray): String {
        val portInBytes: ByteArray = byteArrayOf(bytes[49], bytes[48])
        val bufferBytes = ByteBuffer.wrap(portInBytes)
        val portUids = bufferBytes.getShort(0)
        val uid_ = when {
            myLocalPort1 == portUids.toInt() -> myUID1
            myLocalPort2 == portUids.toInt() -> myUID2
            else -> ""
        }
        return uid_
    }

    fun startLocalSocketALoop(
        myLocalPort: Int, uid: String, myUID: String,
        LocalALoopThread: HashMap<String, Thread>,
        LocalALoopSocket: HashMap<String, Socket?>
    ) {
        LocalALoopThread[uid] = thread {
            LocalALoopSocket[uid] = Socket("127.0.0.1", myLocalPort)
            while (!mAllStop) {
                val din = LocalALoopSocket[uid]?.getInputStream()
                val bytesnum = din?.available()
                if (bytesnum!! > 0) {
                    val bytes = ByteArray(bytesnum)
                    din.read(bytes, 0, bytesnum)

                    sendBytesToDataChannel(uid, myUID, bytes)
                } else
                    Thread.sleep(10)
            }
        }
    }

    fun sendBytesToDataChannel(uid: String, touid: String, bytes: ByteArray) {
        var pair: Pair<String, String> = Pair(uid, touid)
        val pair2: Pair<String, String> = Pair(touid, uid)

        if (mP2PMesh!!.mConnections.contains(pair2)) {
            pair = pair2
        }

        while (!mAllStop && mP2PMesh!!.mConnections[pair]?.dataChannel?.state() != DataChannel.State.OPEN)
            Thread.sleep(10)

        Log.d(TAG, "SocketWrapper send to dataChannel ${bytes.size} - bytes; myUID - $touid  uid -  $uid")
        val json = JSONObject()
        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
        json.put("bytes", base64String)
        json.put("myUID", touid)
        json.put("toUID", uid)
        val buffer = ByteBuffer.wrap(json.toString().toByteArray(StandardCharsets.UTF_8))

        mP2PMesh!!.mConnections[pair]?.dataChannel?.send(DataChannel.Buffer(buffer, true))
    }
}
