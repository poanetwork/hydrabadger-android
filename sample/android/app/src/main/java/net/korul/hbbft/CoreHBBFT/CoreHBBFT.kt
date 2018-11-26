package net.korul.hbbft.CoreHBBFT

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.SpannableStringBuilder
import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.Session
import net.korul.hbbft.server.util.ModelStun
import net.korul.hbbft.server.util.ServerUtil
import net.korul.hbbft.server.util.ServerUtil.Companion.connectToStun
import net.korul.hbbft.server.util.ServerUtil.Companion.requestGetClient
import net.korul.hbbft.server.util.ServerUtil.Companion.requestInsertClient
import net.korul.hbbft.services.ClosingService
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

data class Optional<M>(val value: M?)

interface CoreHBBFTListener {
    fun updateStateToOnline()

    fun reciveMessage(you: Boolean, uid: String, mes: String)
}


object CoreHBBFT {
    // это будет именем файла настроек
    val APP_PREFERENCES = "mysettings"
    val APP_PREFERENCES_NAME1 = "UUID1" // UUID
    val APP_PREFERENCES_NAME2 = "UUID2" // UUID
    val APP_PREFERENCES_NAME3 = "UUID3" // UUID

    lateinit var uniqueID1: String
    lateinit var uniqueID2: String
    lateinit var uniqueID3: String

    private val TAG = "Hydrabadger"

    var session: Session? = null

    lateinit var thread1: Thread
    lateinit var thread2: Thread
    lateinit var thread3: Thread

    private val lock = ReentrantLock()

    private var modelStun1 = ModelStun()
    private var modelStun2 = ModelStun()
    private var modelStun3 = ModelStun()

    private var ipserver = "62.176.10.54"

    private var mClose: Boolean = false
    var mShowError: Boolean = true

    private val listeners = ArrayList<CoreHBBFTListener?>()

    var mUpdateStateToOnline = false
    var mRoomName: String = ""

    // Used to load the 'native-lib' library on application startup.
    init {
        System.loadLibrary("hydra_android")

        session = net.korul.hbbft.Session()

        generateOrGetUID()
    }

    fun addListener(toAdd: CoreHBBFTListener) {
        listeners.add(toAdd)
    }
    fun afterSubscribeSession() {
        session?.after_subscribe()
    }

    fun subscribeSession() {
        session?.subscribe {you: Boolean, uid: String, mes: String ->
            if (uid == "test" && mes == "test") {
                Log.d(TAG, "subscribeSession - init")
                mShowError = false
                return@subscribe
            }

            if(!mUpdateStateToOnline) {
                for (hl in listeners)
                    hl?.updateStateToOnline()
                mUpdateStateToOnline = true
            }

            if (!uid.isEmpty() && !mes.isEmpty() && mes != "[None]") {
                val str = SpannableStringBuilder()
                var mess = mes.removeRange(0, 19)
                mess = mess.removeRange(mess.count() - 5, mess.count())
                str.append(mess)

                // Notify everybody that may be interested.
                for (hl in listeners)
                    hl?.reciveMessage(you, uid, str.toString())
            }
        }
    }

    fun sendMessage(uid: String, str: String) {
        if(str.isNotEmpty()) {
            var num = 0
            when (uid) {
                uniqueID1 -> num = 0
                uniqueID2 -> num = 2
                uniqueID3 -> num = 3
            }

            session?.send_message(num, str)
        }
    }

    fun generateOrGetUID() {
        val mSettings = DatabaseApplication.instance.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        var uiid = mSettings.getString(APP_PREFERENCES_NAME1, "")

        if (uiid == null || uiid == "") {
            uniqueID1 = UUID.randomUUID().toString()

            val editor = mSettings.edit()
            editor.putString(APP_PREFERENCES_NAME1, uniqueID1)
            editor.apply()
        } else
            uniqueID1 = uiid

        uiid = mSettings.getString(APP_PREFERENCES_NAME2, "")

        if (uiid == null || uiid == "") {
            uniqueID2 = UUID.randomUUID().toString()

            val editor = mSettings.edit()
            editor.putString(APP_PREFERENCES_NAME2, uniqueID2)
            editor.apply()
        } else
            uniqueID2 = uiid

        uiid = mSettings.getString(APP_PREFERENCES_NAME3, "")

        if (uiid == null || uiid == "") {
            uniqueID3 = UUID.randomUUID().toString()

            val editor = mSettings.edit()
            editor.putString(APP_PREFERENCES_NAME3, uniqueID3)
            editor.apply()
        } else
            uniqueID3 = uiid
    }

    @SuppressLint("CheckResult")
    fun initConnectWithReset(context: Context,
                             use1: Boolean,
                             use2: Boolean,
                             use3: Boolean,
                             uid1: String,
                             uid2: String,
                             uid3: String,
                             roomName: String) {
        mRoomName = roomName

        Observable.just(Optional(null))
            .subscribeOn(Schedulers.newThread())
            .doOnNext {
                if (use1) {
                    val arr: Array<String> = arrayOf(uid1)
                    ServerUtil.requestDeleteClient(arr)
                    ServerUtil.resetConnectOnServer(uid1, ipserver)
                }
            }
            .doOnNext {
                if (use1) {
                    Log.d(TAG, "connectToStun uniqueID1")
                    modelStun1 = connectToStun(uniqueID1, ipserver)
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use1) {
                    Log.d(TAG, "requestInsertClient uniqueID1")
                    val arr: Array<String> =
                        arrayOf(uniqueID1, roomName, "${ipserver}:${modelStun1.myPortForOther}")
                    requestInsertClient(arr)
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use1) {
                    Log.d(TAG, "requestGetClient uniqueID1")
                    val clients = requestGetClient(roomName, uniqueID1)

                    Log.d(TAG, "retranslateAllToMainSocket 1")
                    retranslateAllToMainSocket(
                        "${ipserver}",
                        modelStun1.myRecievePort,
                        "127.0.0.1",
                        modelStun1.myRecievePort
                    )

                    var strTosend = ""
                    if (clients.isNotEmpty()) {
                        strTosend = ""

                        for (ipport in clients) {
                            strTosend += ipport
                            strTosend += ";"
                        }

                        if (strTosend.endsWith(";"))
                            strTosend = strTosend.substring(0, strTosend.length - 1)
                    }

                    thread1 = Thread {
                        session?.start_node(
                            "127.0.0.1:${modelStun1.myRecievePort}",
                            "${ipserver}:${modelStun1.myPortForOther}",
                            strTosend
                        )
                    }
                    thread1.start()
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use2) {
                    val arr: Array<String> = arrayOf(uid2)
                    ServerUtil.requestDeleteClient(arr)
                    ServerUtil.resetConnectOnServer(uid2, ipserver)
                }
            }
            .doOnNext {
                if (use2) {
                    Log.d(TAG, "connectToStun uniqueID2")
                    modelStun2 = connectToStun(uniqueID2, ipserver.toString())
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use2) {
                    Log.d(TAG, "requestInsertClient uniqueID2")
                    val arr: Array<String> =
                        arrayOf(uniqueID2, roomName, "${ipserver}:${modelStun2.myPortForOther}")
                    requestInsertClient(arr)
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use2) {
                    Log.d(TAG, "requestGetClient uniqueID2")
                    val clients = requestGetClient(roomName, uniqueID2)

                    Log.d(TAG, "retranslateAllToMainSocket 2")
                    retranslateAllToMainSocket(
                        "${ipserver}",
                        modelStun2.myRecievePort,
                        "127.0.0.1",
                        modelStun2.myRecievePort
                    )

                    var strTosend = ""
                    if (clients.isNotEmpty()) {
                        strTosend = ""

                        for (ipport in clients) {
                            strTosend += ipport
                            strTosend += ";"
                        }

                        if (strTosend.endsWith(";"))
                            strTosend = strTosend.substring(0, strTosend.length - 1)
                    }

                    thread2 = Thread {
                        session?.start_node(
                            "127.0.0.1:${modelStun2.myRecievePort}",
                            "${ipserver}:${modelStun2.myPortForOther}",
                            strTosend
                        )
                    }
                    thread2.start()
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use3) {
                    val arr: Array<String> = arrayOf(uid3)
                    ServerUtil.requestDeleteClient(arr)
                    ServerUtil.resetConnectOnServer(uid3, ipserver)
                }
            }
            .doOnNext {
                if (use3) {
                    Log.d(TAG, "connectToStun uniqueID3")
                    modelStun3 = connectToStun(uniqueID3, ipserver.toString())
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use3) {
                    Log.d(TAG, "requestInsertClient uniqueID3")
                    val arr: Array<String> =
                        arrayOf(uniqueID3, roomName, "${ipserver}:${modelStun3.myPortForOther}")
                    requestInsertClient(arr)
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use3) {
                    Log.d(TAG, "requestGetClient uniqueID3")
                    val clients = requestGetClient(roomName, uniqueID3)

                    Log.d(TAG, "retranslateAllToMainSocket 3")
                    retranslateAllToMainSocket(
                        "${ipserver}",
                        modelStun3.myRecievePort,
                        "127.0.0.1",
                        modelStun3.myRecievePort
                    )

                    var strTosend = ""
                    if (clients.isNotEmpty()) {
                        strTosend = ""

                        for (ipport in clients) {
                            strTosend += ipport
                            strTosend += ";"
                        }

                        if (strTosend.endsWith(";"))
                            strTosend = strTosend.substring(0, strTosend.length - 1)
                    }

                    thread3 = Thread {
                        session?.start_node(
                            "127.0.0.1:${modelStun3.myRecievePort}",
                            "${ipserver}:${modelStun3.myPortForOther}",
                            strTosend
                        )
                    }
                    thread3.start()
                }
            }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({

                val serviceIntent = Intent(context, ClosingService::class.java)
                serviceIntent.putExtra("uniqueID1", uniqueID1)
                serviceIntent.putExtra("uniqueID2", uniqueID2)
                serviceIntent.putExtra("uniqueID3", uniqueID3)
                serviceIntent.putExtra("server", ipserver.toString())
                context.startService(serviceIntent)

            }, { it ->
                val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
                } else {
                    AlertDialog.Builder(context)
                }
                builder.setTitle("Error ")
                    .setMessage(it.message)
                    .setPositiveButton(android.R.string.yes) { dialog, which ->
                        dialog.cancel()
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            })
    }

    @SuppressLint("CheckResult")
    fun initConnect(context: Context, use1: Boolean, use2: Boolean, use3: Boolean, roomName: String) {
        Observable.just(Optional(null))
            .subscribeOn(Schedulers.newThread())
            .doOnNext {
                if (use1) {
                    Log.d(TAG, "connectToStun uniqueID1")
                    modelStun1 = connectToStun(uniqueID1, ipserver)
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use1) {
                    Log.d(TAG, "requestInsertClient uniqueID1")
                    val arr: Array<String> =
                        arrayOf(uniqueID1, roomName, "${ipserver}:${modelStun1.myPortForOther}")
                    requestInsertClient(arr)
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use1) {
                    Log.d(TAG, "requestGetClient uniqueID1")
                    val clients = requestGetClient(roomName, uniqueID1)

                    Log.d(TAG, "retranslateAllToMainSocket 1")
                    retranslateAllToMainSocket(
                        "${ipserver}",
                        modelStun1.myRecievePort,
                        "127.0.0.1",
                        modelStun1.myRecievePort
                    )

                    var strTosend = ""
                    if (clients.isNotEmpty()) {
                        strTosend = ""

                        for (ipport in clients) {
                            strTosend += ipport
                            strTosend += ";"
                        }

                        if (strTosend.endsWith(";"))
                            strTosend = strTosend.substring(0, strTosend.length - 1)
                    }

                    thread1 = Thread {
                        session?.start_node(
                            "127.0.0.1:${modelStun1.myRecievePort}",
                            "${ipserver}:${modelStun1.myPortForOther}",
                            strTosend
                        )
                    }
                    thread1.start()
                }
            }
            .delay(1000, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use2) {
                    Log.d(TAG, "connectToStun uniqueID2")
                    modelStun2 = connectToStun(uniqueID2, ipserver.toString())
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use2) {
                    Log.d(TAG, "requestInsertClient uniqueID2")
                    val arr: Array<String> =
                        arrayOf(uniqueID2, roomName, "${ipserver}:${modelStun2.myPortForOther}")
                    requestInsertClient(arr)
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use2) {
                    Log.d(TAG, "requestGetClient uniqueID2")
                    val clients = requestGetClient(roomName, uniqueID2)

                    Log.d(TAG, "retranslateAllToMainSocket 2")
                    retranslateAllToMainSocket(
                        "${ipserver}",
                        modelStun2.myRecievePort,
                        "127.0.0.1",
                        modelStun2.myRecievePort
                    )

                    var strTosend = ""
                    if (clients.isNotEmpty()) {
                        strTosend = ""

                        for (ipport in clients) {
                            strTosend += ipport
                            strTosend += ";"
                        }

                        if (strTosend.endsWith(";"))
                            strTosend = strTosend.substring(0, strTosend.length - 1)
                    }

                    thread2 = Thread {
                        session?.start_node(
                            "127.0.0.1:${modelStun2.myRecievePort}",
                            "${ipserver}:${modelStun2.myPortForOther}",
                            strTosend
                        )
                    }
                    thread2.start()
                }
            }
            .delay(1000, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use3) {
                    Log.d(TAG, "connectToStun uniqueID3")
                    modelStun3 = connectToStun(uniqueID3, ipserver.toString())
                }
            }
            .delay(10, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use3) {
                    Log.d(TAG, "requestInsertClient uniqueID3")
                    val arr: Array<String> =
                        arrayOf(uniqueID3, roomName, "${ipserver}:${modelStun3.myPortForOther}")
                    requestInsertClient(arr)
                }
            }
            .delay(100, TimeUnit.MILLISECONDS)
            .doOnNext {
                if (use3) {
                    Log.d(TAG, "requestGetClient uniqueID3")
                    val clients = requestGetClient(roomName, uniqueID3)

                    Log.d(TAG, "retranslateAllToMainSocket 3")
                    retranslateAllToMainSocket(
                        "${ipserver}",
                        modelStun3.myRecievePort,
                        "127.0.0.1",
                        modelStun3.myRecievePort
                    )

                    var strTosend = ""
                    if (clients.isNotEmpty()) {
                        strTosend = ""

                        for (ipport in clients) {
                            strTosend += ipport
                            strTosend += ";"
                        }

                        if (strTosend.endsWith(";"))
                            strTosend = strTosend.substring(0, strTosend.length - 1)
                    }

                    thread3 = Thread {
                        session?.start_node(
                            "127.0.0.1:${modelStun3.myRecievePort}",
                            "${ipserver}:${modelStun3.myPortForOther}",
                            strTosend
                        )
                    }
                    thread3.start()
                }
            }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({

                val serviceIntent = Intent(context, ClosingService::class.java)
                serviceIntent.putExtra("uniqueID1", uniqueID1)
                serviceIntent.putExtra("uniqueID2", uniqueID2)
                serviceIntent.putExtra("uniqueID3", uniqueID3)
                serviceIntent.putExtra("server", ipserver.toString())
                context.startService(serviceIntent)

            }, { it ->
                val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
                } else {
                    AlertDialog.Builder(context)
                }
                builder.setTitle("Error ")
                    .setMessage(it.message)
                    .setPositiveButton(android.R.string.yes) { dialog, which ->
                        dialog.cancel()
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show()
            })
    }

    fun retranslateAllToMainSocket(
        addrFrom: String,
        portFrom: Int,
        addrTo: String,
        portTo: Int
    ) {
        val thread = Thread {
            try {
                val soc = Socket(addrFrom, portFrom)
                val din = DataInputStream(soc.getInputStream())

                val HashMapSocket: HashMap<Int, Socket?> = hashMapOf()
                val HashMapThread: HashMap<Int, Thread> = hashMapOf()

                while (true) {
                    if (mClose)
                        break

                    val bytesAvailable = din.available()
                    if (bytesAvailable > 0) {
                        var bytesnum = din.available()
                        while (bytesnum < 8) {
                            Thread.sleep(10)
                            bytesnum = din.available()
                        }

                        val size = din.readInt()
                        val socketDescription = din.readInt()

                        bytesnum = din.available()
                        while (bytesnum < size && !mClose) {
                            Thread.sleep(10)
                            bytesnum = din.available()
                        }

                        val bytes = ByteArray(size)
                        val reads = din.read(bytes, 0, size)

                        writeAndConfigLocal(
                            bytes,
                            reads,
                            socketDescription,
                            HashMapSocket,
                            HashMapThread,
                            addrTo,
                            portTo,
                            soc
                        )
                        continue
                    } else {
                        Thread.sleep(10)
                    }
                }
                din.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        thread.start()
    }

    fun writeAndConfigLocal(
        bytes: ByteArray,
        size: Int,
        socketDescription: Int,
        HashMapSocket: HashMap<Int, Socket?>,
        HashMapThread: HashMap<Int, Thread>,
        addrTo: String,
        portTo: Int,
        socOut: Socket
    ) {
        if (!HashMapSocket.contains(socketDescription)) {
            try {
                HashMapSocket[socketDescription] = Socket(addrTo, portTo)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            HashMapThread[socketDescription] = Thread {
                while (true) {
                    if (mClose)
                        break

                    val dout = DataOutputStream(socOut.getOutputStream())
                    val din = DataInputStream(HashMapSocket[socketDescription]?.getInputStream())

                    val size = din.available()
                    if (size > 0) {
                        lock.lock()
                        val bytes = ByteArray(size)
                        val readsize = din.read(bytes, 0, size)

                        dout.writeInt(readsize)
                        dout.writeInt(socketDescription)
                        dout.write(bytes, 0, readsize)
                        dout.flush()
                        lock.unlock()
                    }
                }
            }
            HashMapThread[socketDescription]?.start()
        }

        val dout = DataOutputStream(HashMapSocket[socketDescription]?.getOutputStream())
        dout.write(bytes, 0, size)
        dout.flush()
    }


}