package net.korul.hbbft.CoreHBBFT

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.korul.hbbft.CommonData.data.model.core.Getters.getAllDialogsUids
import net.korul.hbbft.CommonData.utils.AppUtils
import net.korul.hbbft.CoreHBBFT.FileUtil.ReadObjectFromFile
import net.korul.hbbft.CoreHBBFT.FileUtil.WriteObjectToFile
import net.korul.hbbft.CoreHBBFT.PushWork.preSendPushToStart
import net.korul.hbbft.CoreHBBFT.PushWork.registerForPush
import net.korul.hbbft.CoreHBBFT.RoomDescrWork.updateAllRoomsFromFirebase
import net.korul.hbbft.CoreHBBFT.RoomWork.getUIDsInRoomFromFirebase
import net.korul.hbbft.CoreHBBFT.RoomWork.isSomeBodyOnlineInList
import net.korul.hbbft.CoreHBBFT.RoomWork.reregisterInFirebase
import net.korul.hbbft.CoreHBBFT.RoomWork.setOfflineModeInRoomInFirebase
import net.korul.hbbft.CoreHBBFT.RoomWork.setOnlineModeInRoomInFirebase
import net.korul.hbbft.CoreHBBFT.UserWork.initCurUser
import net.korul.hbbft.CoreHBBFT.UserWork.saveCurUserSync
import net.korul.hbbft.CoreHBBFT.UserWork.setUnOnlineUserWithUID
import net.korul.hbbft.CoreHBBFT.UserWork.updateAllUsersFromFirebase
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.P2P.IGetData
import net.korul.hbbft.P2P.P2PMesh
import net.korul.hbbft.P2P.SocketWrapper
import net.korul.hbbft.R
import net.korul.hbbft.Session
import net.korul.hbbft.services.ClosingService
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.math.abs


object CoreHBBFT : IGetData {
    val TAG = "HYDRA"

    // p2p
    var mP2PMesh: P2PMesh? = null

    var mSocketWrapper: SocketWrapper? = null
    val APP_PREFERENCES = "mysettings"

    val APP_PREFERENCES_NAME1 = "UUID1" // UUID
    val APP_PREFERENCES_NAME2 = "UUID2" // UUID

    lateinit var uniqueID1: String
    lateinit var uniqueID2: String

    var session: Session? = null
    var mShowError: Boolean = true

    private val listeners = ArrayList<CoreHBBFTListener?>()

    var mUpdateStateToOnline = false
    var mRoomId: String = ""

    var lastMes: String = ""
    var lastMestime = Calendar.getInstance().timeInMillis

    val mFunctions: FirebaseFunctions
    var mAuth: FirebaseAuth

    lateinit var mApplicationContext: Context
    var mDatabase: DatabaseReference

    val formatDate= SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    val handler = Handler()
    var runnable: Runnable? = null

    init {
        System.loadLibrary("hydra_android")

        session = net.korul.hbbft.Session()

        mFunctions = FirebaseFunctions.getInstance()
        mAuth = FirebaseAuth.getInstance()

        mDatabase = FirebaseDatabase.getInstance().reference

//        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }

    fun Init(applicationContext: Context) {
        mApplicationContext = applicationContext

        generateOrGetUID()

        val serviceIntent = Intent(applicationContext, ClosingService::class.java)
        applicationContext.startService(serviceIntent)

        mP2PMesh = P2PMesh(applicationContext, this)
        mSocketWrapper = SocketWrapper(mP2PMesh!!)

        registerForPush(applicationContext)

        FirebaseMessaging.getInstance().subscribeToTopic(uniqueID1)
            .addOnCompleteListener { task ->
                var msg = mApplicationContext.getString(R.string.msg_subscribed)
                if (!task.isSuccessful) {
                    msg = mApplicationContext.getString(R.string.msg_subscribe_failed)
                }
                Log.d(TAG, msg)
            }

        initCurUser()
        thread {
            val latch = authAnonymouslyInFirebase()
            latch.await()
            saveCurUserSync(DatabaseApplication.mCurUser)
            reregisterInFirebase(getAllDialogsUids(), uniqueID1)
        }
    }

    fun ALLUpdate() {
        thread {
            updateAllUsersFromFirebase()
            updateAllRoomsFromFirebase()
        }
    }

    fun setRoomId(roomid: String) {
        mRoomId = roomid
    }

    fun authAnonymouslyInFirebase(): CountDownLatch {
        val latch = CountDownLatch(1)
        mAuth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in dialog's information
                    Log.d(TAG, "signInAnonymously:success")
                    val user = mAuth.currentUser
                } else {
                    // If sign in fails, display a message to the dialog.
                    Log.w(TAG, "signInAnonymously:failure", task.exception)

                    AppUtils.showToast(
                        mApplicationContext,
                        "Authentication failed", true
                    )
                }

                latch.countDown()
            }
            .addOnCanceledListener {
                AppUtils.showToast(
                    mApplicationContext,
                    "Authentication canceled", true
                )
            }
            .addOnFailureListener {
                AppUtils.showToast(
                    mApplicationContext,
                    "Authentication Failure", true
                )
            }

        return latch
    }

    fun startAllNode(RoomId: String) {
        thread {
            val listObjectsOfUIds = getUIDsInRoomFromFirebase(RoomId)
            val isSomebodyOnline = isSomeBodyOnlineInList(listObjectsOfUIds)
            val cntUsers = listObjectsOfUIds.count()

            // if 1 dialog
            if (cntUsers < 2) {
                AppUtils.showToast(
                    mApplicationContext,
                    "Room is empty", true
                )
                Log.d(TAG, "Room is empty $RoomId")

            }
            // if 2 users and i am first
            else if (cntUsers == 2 && !isSomebodyOnline) {
                start_node_2x(RoomId)

                var uid = ""
                for (ui in listObjectsOfUIds) {
                    if (ui.UID != uniqueID1) {
                        uid = ui.UID!!
                        break
                    }
                }
                if (uid.isNotEmpty()) {
                    Log.d(TAG, "2 users and i am first in room $RoomId")
                    preSendPushToStart(listOf(uid), RoomId, uniqueID1)
                } else {
                    Log.d(TAG, "Room uid is empty $RoomId")
                    AppUtils.showToast(
                        mApplicationContext,
                        "Room is empty", true
                    )
                }
            }
            // if 2 users and second start
            else if (cntUsers == 2 && isSomebodyOnline) {
                Log.d(TAG, "2 users and second start in room $RoomId")
                start_node(RoomId)
            }
            // if many users and i am first
            else if (cntUsers > 2 && !isSomebodyOnline) {
                start_node(RoomId)

                val uids = mutableListOf<String>()
                for (ui in listObjectsOfUIds) {
                    if (ui.UID != uniqueID1) {
                        uids.add(ui.UID!!)
                    }
                }
                if (uids.isNotEmpty()) {
                    Log.d(TAG, "many users and i am first in room $RoomId")
                    preSendPushToStart(uids, RoomId, uniqueID1)
                } else {
                    Log.d(TAG, "Room uid is empty $RoomId")
                    AppUtils.showToast(
                        mApplicationContext,
                        "Room uids is empty", true
                    )
                }
            }
            // if many users and i am not first
            else {
                Log.d(TAG, "many users and i am not first in room $RoomId")
                start_node(RoomId)
            }
        }
    }


    fun start_node(RoomId: String) {
        setOnlineModeInRoomInFirebase(RoomId)

        mRoomId = RoomId

        mP2PMesh?.initOneMesh(RoomId, uniqueID1)
        mP2PMesh?.publishAboutMe(RoomId, uniqueID1)

        waitForConnect()

        mSocketWrapper!!.initSocketWrapper(RoomId, uniqueID1, mP2PMesh!!.usersCon.toList())

        thread {
            var strTosend = ""
            for (clients in mSocketWrapper!!.clientsBusyPorts) {
                if (clients.key != uniqueID1)
                    strTosend += "127.0.0.1:${clients.value};"
            }
            if (strTosend.endsWith(";"))
                strTosend = strTosend.substring(0, strTosend.length - 1)

            session?.start_node(
                "127.0.0.1:${mSocketWrapper!!.myLocalPort1}",
                strTosend,
                uniqueID1
            )
        }
    }

    fun start_node_2x(RoomId: String) {
        setOnlineModeInRoomInFirebase(RoomId)

        mRoomId = RoomId

        mP2PMesh?.initOneMesh(RoomId, uniqueID1)
        mP2PMesh?.initOneMesh(RoomId, uniqueID2)
        mP2PMesh?.publishAboutMe(RoomId, uniqueID1)
        Thread.sleep(100)
        mP2PMesh?.publishAboutMe(RoomId, uniqueID2)

        waitForConnectWithoutSelf()

        mSocketWrapper!!.initSocketWrapper2X(RoomId, uniqueID1, uniqueID2, mP2PMesh!!.usersCon.toList())

        thread {
            var strTosend = ""
            for (clients in mSocketWrapper!!.clientsBusyPorts) {
                if (clients.key != uniqueID1 && clients.key != uniqueID2)
                    strTosend += "127.0.0.1:${clients.value};"
            }
            if (strTosend.endsWith(";"))
                strTosend = strTosend.substring(0, strTosend.length - 1)

            session?.start_node(
                "127.0.0.1:${mSocketWrapper!!.myLocalPort1}",
                strTosend,
                uniqueID1
            )

            strTosend = ""
            for (clients in mSocketWrapper!!.clientsBusyPorts) {
                if (clients.key != uniqueID2) {
                    strTosend += "127.0.0.1:${clients.value};"
                }
            }
            if (strTosend.endsWith(";"))
                strTosend = strTosend.substring(0, strTosend.length - 1)

            session?.start_node(
                "127.0.0.1:${mSocketWrapper!!.myLocalPort2}",
                strTosend,
                uniqueID2
            )
        }
    }

    fun waitForConnectWithoutSelf() {
        val async = GlobalScope.async {
            var ready = false
            while (!ready) {
                Thread.sleep(1000)
                ready = true
                for (con in mP2PMesh?.mConnections!!.values) {
                    if (con.myName == uniqueID1 || con.myName == uniqueID2)
                        continue

                    if (con.mIamReadyToDataTranfer) {
                        ready = true
                    } else {
                        ready = false
                        break
                    }
                }
            }
        }
        runBlocking { async.await() }
    }

    fun waitForConnect() {
        val async = GlobalScope.async {
            var ready = false
            while (!ready) {
                Thread.sleep(1000)
                ready = true
                for (con in mP2PMesh?.mConnections!!.values) {
                    if (con.mIamReadyToDataTranfer) {
                        ready = true
                    } else {
                        ready = false
                        break
                    }
                }
            }
        }
        runBlocking { async.await() }
    }

    fun Free() {
        mP2PMesh?.FreeConnect()
        mSocketWrapper?.mAllStop = true

        setOfflineModeInRoomInFirebase(mRoomId)
    }

    override fun dataReceived(bytes: ByteArray) {
        if (mSocketWrapper?.mStarted != null && mSocketWrapper?.mStarted!!)
            mSocketWrapper?.sendReceivedDataToHydra(bytes)
    }


    fun addListener(toAdd: CoreHBBFTListener?) {
        listeners.add(toAdd)
    }

    fun delListener(toDel: CoreHBBFTListener?) {
        listeners.remove(toDel)
    }

    fun afterSubscribeSession() {
        session?.after_subscribe()
    }


    fun subscribeSession() {
        session?.subscribe {uid: String, mes: String ->
            if (uid == "test" && mes == "test") {
                Log.d(TAG, "subscribeSession - init")
                mShowError = false
                return@subscribe
            }

            if (!mUpdateStateToOnline) {
                for (hl in listeners) {
                    hl?.updateStateToOnline()
                    mUpdateStateToOnline = true
                }
            }

            if (!uid.isEmpty() && !mes.isEmpty() && uid != uniqueID2 && uid != uniqueID1) {
                if (lastMes == mes) {
                    val mestime = Calendar.getInstance().timeInMillis
                    if (abs(mestime - lastMestime) < 500) {
                        lastMestime = Calendar.getInstance().timeInMillis
                        return@subscribe
                    }
                }

                var message = mes.replace("[Transaction ", "")
                message = message.removeRange(message.count() - 1, message.count())
                val jsonObj = JSONObject(message)
                val messJson = jsonObj.getJSONArray("trVec")
                for (i in 0 until messJson!!.length()) {
                    var mess = messJson.getString(i)
                    if(mess.endsWith('!'))
                        mess = mess.removeRange(mess.count() - 1, mess.count())

                    parseMessage(mess, uid)
                }

                lastMes = mes
                lastMestime = Calendar.getInstance().timeInMillis
            }
        }
        session?.subscribe { uid: String, mes: String ->

        }
    }

    fun parseMessage(mess: String, uid: String) {
        val commMes = mess.split("᳀†\u058D:")
        if(commMes.size >= 2) when(commMes[0]) {
            "ILIVE" -> {
                if(runnable == null) {
                    runnable = Runnable {
                        setUnOnlineUserWithUID(uid, true)
                        for (hl in listeners)
                            hl?.setOnlineUser(uid, true)
                        runnable = null
                    }
                    handler.post(runnable)
                }
            }
            "IDIE" -> {
                if(runnable == null)
                    handler.removeCallbacks(runnable)
                runnable = Runnable {
                    setUnOnlineUserWithUID(uid, false)
                    for (hl in listeners)
                        hl?.setOnlineUser(uid, false)
                    runnable = null
                }
                handler.post(runnable)
            }
            "MESSFOR" -> {
//                try {
//                    val mesforUId = commMes[1]
//                    if(mesforUId == uniqueID1) {
//                        val you = uid == uniqueID1 || uid == uniqueID2
//                        val date = formatDate.parse(commMes[2])
//                        if(abs(date.time - Calendar.getInstance().timeInMillis) > 20*1000) {
//                            for (hl in listeners)
//                                hl?.reciveMessageHistory(you, uid, commMes[3], date)
//                        }
//                        else {
//                            for (hl in listeners)
//                                hl?.reciveMessage(you, uid, commMes[3], date)
//                        }
//                    }
//                }
//                catch (e: Exception) {
//                    Log.d(TAG, "ERROR parse incoming message")
//                    e.printStackTrace()
//                }
            }
            "MESS" -> {
                try {
                    val you = uid == uniqueID1 || uid == uniqueID2
                    val date = formatDate.parse(commMes[1])
                    for (hl in listeners)
                        hl?.reciveMessage(you, uid, commMes[2], date)
                }
                catch (e: Exception) {
                    Log.d(TAG, "ERROR parse incoming message")
                    e.printStackTrace()
                }
            }
        }
    }

    fun sendMessageIDIE() {
        val mes = "IDIE᳀†\u058D:$uniqueID1"
        session?.send_message(mes)
    }

    fun sendMessage(str: String) {
        if (str.isNotEmpty()) {
            val dateString = formatDate.format(Date())
            val mes = "MESS᳀†\u058D:${dateString}᳀†\u058D:$str"
            session?.send_message(mes)
        }
    }

    fun generateOrGetUID() {
        val uiid = ReadObjectFromFile(APP_PREFERENCES_NAME1)
        val uiid1 = ReadObjectFromFile(APP_PREFERENCES_NAME2)

        if (uiid == null || uiid == "") {
            uniqueID1 = UUID.randomUUID().toString()

            WriteObjectToFile(uniqueID1, APP_PREFERENCES_NAME1)
        } else
            uniqueID1 = uiid

        if (uiid1 == null || uiid1 == "") {
            uniqueID2 = UUID.randomUUID().toString()

            WriteObjectToFile(uniqueID2, APP_PREFERENCES_NAME2)
        } else
            uniqueID2 = uiid1
    }
}