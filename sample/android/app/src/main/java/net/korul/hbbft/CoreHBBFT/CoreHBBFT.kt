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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.korul.hbbft.CommonData.data.fixtures.MessagesFixtures
import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.data.model.Message
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.data.model.conversation.Conversations
import net.korul.hbbft.CommonData.data.model.core.Getters
import net.korul.hbbft.CommonData.data.model.core.Getters.getAllDialogsUids
import net.korul.hbbft.CommonData.data.model.core.Getters.getDialog
import net.korul.hbbft.CommonData.data.model.core.Getters.getMessagesLessDate
import net.korul.hbbft.CommonData.data.model.core.Getters.getMessagesLessGreaterDate
import net.korul.hbbft.CommonData.utils.AppUtils
import net.korul.hbbft.CoreHBBFT.FileUtil.ReadAnyObjectFromFile
import net.korul.hbbft.CoreHBBFT.FileUtil.ReadObjectFromFile
import net.korul.hbbft.CoreHBBFT.FileUtil.WriteAnyObjectToFile
import net.korul.hbbft.CoreHBBFT.FileUtil.WriteObjectToFile
import net.korul.hbbft.CoreHBBFT.PushWork.preSendPushToStart
import net.korul.hbbft.CoreHBBFT.PushWork.registerForPush
import net.korul.hbbft.CoreHBBFT.RoomDescrWork.updateAllRoomsFromFirebase
import net.korul.hbbft.CoreHBBFT.RoomWork.getUIDsInRoomFromFirebase
import net.korul.hbbft.CoreHBBFT.RoomWork.isSomeBodyOnlineInList
import net.korul.hbbft.CoreHBBFT.RoomWork.registerInFirebase
import net.korul.hbbft.CoreHBBFT.RoomWork.setOfflineModeInRoomInFirebase
import net.korul.hbbft.CoreHBBFT.RoomWork.setOnlineModeInRoomInFirebase
import net.korul.hbbft.CoreHBBFT.UserWork.getUserFromLocalOrDownloadFromFirebase
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.math.abs


object CoreHBBFT : IGetData {
    val TAG = "HYDRA:CoreHBBFT"

    // p2p
    var mP2PMesh: P2PMesh? = null
    var mSocketWrapper: SocketWrapper? = null

    val APP_PREFERENCES = "mysettings"
    val APP_PREFERENCES_NAME1 = "UUID1" // UUID
    val APP_PREFERENCES_NAME2 = "UUID2" // UUID
    val APP_PREFERENCES_MESSDATE = "APP_PREFERENCES_MESSDATE" // UUID

    lateinit var uniqueID1: String
    lateinit var uniqueID2: String

    var session: Session? = null
    var mShowError: Boolean = true

    private val listeners = ArrayList<CoreHBBFTListener?>()

    var mCurRoomId: String = ""
    var lastMes: String = ""
    var lastUid: String = ""
    var lastMestime = Calendar.getInstance().timeInMillis

    val mFunctions: FirebaseFunctions
    var mAuth: FirebaseAuth

    lateinit var mApplicationContext: Context
    var mDatabase: DatabaseReference

    private val format = "yyyy-MM-dd HH:mm:ss.SSS Z"
    private val formatWithoutZone = "yyyy-MM-dd HH:mm:ss.SSS"
    private val dateLastSync = "1970-04-12 12:18:11.000".toDate(formatWithoutZone)

    fun String.toDate(dateFormat: String = format, timeZone: TimeZone = TimeZone.getTimeZone("UTC")): Date {
        val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
        parser.timeZone = timeZone
        return parser.parse(this)
    }

    fun Date.formatTo(dateFormat: String = format, timeZone: TimeZone = TimeZone.getDefault()): String {
        val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
        formatter.timeZone = timeZone
        return formatter.format(this)
    }

    // Message types
    enum class MessageType(s: String) {
        ILIVE("ILIVE"),
        IDIE("IDIE"),
        MESS("MESS"),
        GETHIST("GETHIST"),
        SENDHIST("SENDHIST"),
        GETSYNC("GETSYNC"),
        SENDSYNC("SENDSYNC")

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                     Sync  _  Online
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // roomName --  useruid - online/offline
    private val listOnlineUsers: HashMap<String, MutableList<String>> = hashMapOf()
    // roomName -- sync/notsync
    private val mFlagsSyncMess: HashMap<String, Boolean> = hashMapOf()
    // roomName -- update online/ offline
    val listFlagsUpdatedStateToOnline: HashMap<String, Boolean> = hashMapOf()
    private val listFlagsSendRequestToHistory: HashMap<String, Boolean> = hashMapOf()
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // roomName - list of dates less date
    private var mNotSyncMessInDialog: HashMap<String, MutableList<String>> = hashMapOf()
    // roomName - list of users try sync
    private var mSyncUsersUids: HashMap<String, MutableList<String>> = hashMapOf()
    // start new connection timer
    private val handlerTimer = Handler()
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    var stopWaitConnect: Boolean = false
    var waitConnectLooping: Boolean = false
    var mFlagSendNotSync: Boolean = false

    val handler = Handler()
    val handlerNewMes = Handler()

    init {
        System.loadLibrary("hydra_android")

        session = net.korul.hbbft.Session()

        mFunctions = FirebaseFunctions.getInstance()
        mAuth = FirebaseAuth.getInstance()

        mDatabase = FirebaseDatabase.getInstance().reference

//        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }

    fun initCoreHBBFT(applicationContext: Context) {
        mApplicationContext = applicationContext

        generateOrGetUID()

        val any = ReadAnyObjectFromFile(APP_PREFERENCES_MESSDATE)
        mNotSyncMessInDialog = if (any != null)
            any as HashMap<String, MutableList<String>>
        else
            hashMapOf()

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
            registerInFirebase(getAllDialogsUids(), uniqueID1)
        }
    }

    fun allUpdateFirebase() {
        Log.d(CoreHBBFT.TAG, "allUpdateFirebase")
        thread {
            updateAllUsersFromFirebase()
            updateAllRoomsFromFirebase()
        }
    }

    fun setCurRoomId(roomid: String) {
        mCurRoomId = roomid
    }

    fun authAnonymouslyInFirebase(): CountDownLatch {
        val latch = CountDownLatch(1)
        mAuth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInAnonymously:success")
                    val user = mAuth.currentUser
                } else {
                    Log.w(TAG, "signInAnonymously:failure", task.exception)
                    handler.post {
                        AppUtils.showToast(
                            mApplicationContext,
                            "Authentication failed", true
                        )
                    }
                }
                latch.countDown()
            }
            .addOnCanceledListener {
                handler.post {
                    AppUtils.showToast(
                        mApplicationContext,
                        "Authentication canceled", true
                    )
                }
            }
            .addOnFailureListener {
                handler.post {
                    AppUtils.showToast(
                        mApplicationContext,
                        "Authentication Failure", true
                    )
                }
            }
        return latch
    }

    fun startAllNode(RoomId: String) {
        Log.d(CoreHBBFT.TAG, "startAllNode")
        thread {
            Log.d(CoreHBBFT.TAG, "startAllNode: getUIDsInRoomFromFirebase")
            val listObjectsOfUIds = getUIDsInRoomFromFirebase(RoomId)
            val isSomebodyOnline = isSomeBodyOnlineInList(listObjectsOfUIds)
            Log.d(CoreHBBFT.TAG, "startAllNode: isSomebodyOnline - $isSomebodyOnline")
            val cntUsers = listObjectsOfUIds.count()
            Log.d(CoreHBBFT.TAG, "startAllNode: cntUsers - $cntUsers")

            when {
                // if 1 dialog
                cntUsers < 2 -> {
                    Log.d(CoreHBBFT.TAG, "startAllNode: cntUsers - $cntUsers < 2")

                    for (hl in listeners)
                        hl?.updateStateToError()
                    setOfflineModeInRoomInFirebase(RoomId)
                    Log.d(TAG, "Room is empty $RoomId")
                }
                // if 2 users and i am first
                cntUsers == 2 && !isSomebodyOnline -> {
                    Log.d(
                        CoreHBBFT.TAG,
                        "startAllNode: cntUsers - $cntUsers == 2 and isSomebodyOnline - $isSomebodyOnline - start_node_2x"
                    )

                    start_node_2x(RoomId, isSomebodyOnline)

                    var uid = ""
                    for (ui in listObjectsOfUIds) {
                        if (ui.UID != uniqueID1) {
                            uid = ui.UID!!
                            break
                        }
                    }
                    if (uid.isNotEmpty()) {
                        Log.d(TAG, "startAllNode: 2 users and i am first in room $RoomId")
                        preSendPushToStart(listOf(uid), RoomId, uniqueID1)
                    } else {
                        Log.d(TAG, "startAllNode: Room uid is empty $RoomId")
                        AppUtils.showToast(
                            mApplicationContext,
                            "Room is empty", true
                        )
                    }
                }
                // if 2 users and second start
                cntUsers == 2 && isSomebodyOnline -> {
                    Log.d(TAG, "startAllNode: 2 users and second start in room $RoomId")
                    start_node_first(RoomId, isSomebodyOnline)
                }
                // if many users and i am first
                cntUsers > 2 && !isSomebodyOnline -> {
                    start_node_2x(RoomId, isSomebodyOnline)
                    //TODO fixxxxxxxxxxx
//                    start_node_first(RoomId, isSomebodyOnline)

                    val uids = mutableListOf<String>()
                    listObjectsOfUIds.filter { it.UID != uniqueID1 && it.UID != null }.forEach { uids.add(it.UID!!) }


//                    E/HYDRABADGERTAG: thread 'tokio-runtime-worker-1' panicked at '::handle_key_gen_part: State must be `GeneratingKeys`. State:
//                    AwaitingPeers { required_peers: [], available_peers: [] }
//
//                    [FIXME: Enqueue these parts!]
//
//                    ': src/hydrabadger/key_gen.rs:333
//                    handlerTimer.postDelayed({
//                        start_node_second(RoomId, true)
//                    }, 30000)

                    if (uids.isNotEmpty()) {
                        Log.d(TAG, "startAllNode: many users and i am first in room $RoomId")
                        preSendPushToStart(uids, RoomId, uniqueID1)
                    } else {
                        Log.d(TAG, "startAllNode: Room uid is empty $RoomId")
                        handler.post {
                            AppUtils.showToast(
                                mApplicationContext,
                                "Room uids is empty", true
                            )
                        }
                    }
                }
                // if many users and i am not first
                else -> {
                    Log.d(TAG, "startAllNode: many users and i am not first in room $RoomId")
                    start_node_first(RoomId, isSomebodyOnline)
                }
            }
        }
    }


    private fun start_node_first(RoomId: String, isSomebodyOnline: Boolean) {
        Log.d(TAG, "start_node_first: RoomId - $RoomId")
        setOnlineModeInRoomInFirebase(RoomId)

        mCurRoomId = RoomId
        listFlagsUpdatedStateToOnline[mCurRoomId] = false
        listFlagsSendRequestToHistory[mCurRoomId] = false
        mFlagsSyncMess[mCurRoomId] = false

        Log.d(TAG, "start_node_first: initOneMesh and publishAboutMe - $RoomId")
        mP2PMesh?.initOneMesh(RoomId, uniqueID1)
        mP2PMesh?.publishAboutMe(RoomId, uniqueID1)

        waitForConnect(false, isSomebodyOnline)
        Log.d(TAG, "start_node_first: waitForConnect finish - $RoomId")

        mSocketWrapper!!.initSocketWrapperFirst(RoomId, uniqueID1, mP2PMesh!!.usersCon.toList())

        thread {
            var strTosend = ""
            mSocketWrapper!!.clientsBusyPorts.filter { it.key != uniqueID1 }
                .forEach { strTosend += "127.0.0.1:${it.value};" }
            if (strTosend.endsWith(";"))
                strTosend = strTosend.substring(0, strTosend.length - 1)

            Log.d(TAG, "start_node_first: session?.start_node_first - $RoomId")
            session?.start_node(
                "127.0.0.1:${mSocketWrapper!!.myLocalPort1}",
                strTosend,
                uniqueID1
            )
        }
    }

    private fun start_node_second(RoomId: String, isSomebodyOnline: Boolean) {
        if (listFlagsUpdatedStateToOnline.containsKey(mCurRoomId) && listFlagsUpdatedStateToOnline[mCurRoomId] != null && !listFlagsUpdatedStateToOnline[mCurRoomId]!!) {
            Log.d(TAG, "start_node_second: RoomId - $RoomId")
            setOnlineModeInRoomInFirebase(RoomId)

            mCurRoomId = RoomId
            listFlagsUpdatedStateToOnline[mCurRoomId] = false
            listFlagsSendRequestToHistory[mCurRoomId] = false
            mFlagsSyncMess[mCurRoomId] = false

            Log.d(TAG, "start_node_second: initOneMesh and publishAboutMe - $RoomId")
            mP2PMesh?.initOneMesh(RoomId, uniqueID2)
            mP2PMesh?.publishAboutMe(RoomId, uniqueID2)

            stopWaitConnect = true
            Thread.sleep(200)
            while (waitConnectLooping)
                Thread.sleep(100)
            waitForConnect(true, isSomebodyOnline)
            Log.d(TAG, "start_node_second: waitForConnect finish - $RoomId")

            mSocketWrapper!!.initSocketWrapperSecond(RoomId, uniqueID2, mP2PMesh!!.usersCon.toList())

            thread {
                var strTosend = ""
                mSocketWrapper!!.clientsBusyPorts.filter { it.key != uniqueID2 }
                    .forEach { strTosend += "127.0.0.1:${it.value};" }
                if (strTosend.endsWith(";"))
                    strTosend = strTosend.substring(0, strTosend.length - 1)

                Log.d(TAG, "start_node_second: session?.start_node_second - $RoomId")
                session?.start_node(
                    "127.0.0.1:${mSocketWrapper!!.myLocalPort2}",
                    strTosend,
                    uniqueID2
                )
            }
        }
    }

    private fun start_node_2x(RoomId: String, isSomebodyOnline: Boolean) {
        Log.d(TAG, "start_node_2x: RoomId - $RoomId")
        setOnlineModeInRoomInFirebase(RoomId)

        mCurRoomId = RoomId
        listFlagsUpdatedStateToOnline[mCurRoomId] = false
        listFlagsSendRequestToHistory[mCurRoomId] = false
        mFlagsSyncMess[mCurRoomId] = false

        mP2PMesh?.initOneMesh(RoomId, uniqueID1)
        mP2PMesh?.initOneMesh(RoomId, uniqueID2)
        mP2PMesh?.publishAboutMe(RoomId, uniqueID1)
        Thread.sleep(200)
        mP2PMesh?.publishAboutMe(RoomId, uniqueID2)
        Log.d(TAG, "start_node_2x: initOneMesh and publishAboutMe - $RoomId")

        waitForConnect(true, isSomebodyOnline)
        Log.d(TAG, "start_node_2x: waitForConnect finish - $RoomId")

        mSocketWrapper!!.initSocketWrapper2X(RoomId, uniqueID1, uniqueID2, mP2PMesh!!.usersCon.toList())

        thread {
            var strTosend = ""
            mSocketWrapper!!.clientsBusyPorts.filter { it.key != uniqueID1 && it.key != uniqueID2 }
                .forEach { strTosend += "127.0.0.1:${it.value};" }
            if (strTosend.endsWith(";"))
                strTosend = strTosend.substring(0, strTosend.length - 1)

            Log.d(TAG, "start_node_2x: session?.start_node_first 1  - $RoomId")
            session?.start_node(
                "127.0.0.1:${mSocketWrapper!!.myLocalPort1}",
                strTosend,
                uniqueID1
            )

            strTosend = ""
            mSocketWrapper!!.clientsBusyPorts.filter { it.key != uniqueID2 }
                .forEach { strTosend += "127.0.0.1:${it.value};" }
            if (strTosend.endsWith(";"))
                strTosend = strTosend.substring(0, strTosend.length - 1)

            Log.d(TAG, "start_node_2x: session?.start_node_first 2  - $RoomId")
            session?.start_node(
                "127.0.0.1:${mSocketWrapper!!.myLocalPort2}",
                strTosend,
                uniqueID2
            )
        }
    }

    private fun waitForConnect(withoutSelf: Boolean, isSomebodyOnline: Boolean) {
        stopWaitConnect = false
        val async = GlobalScope.async {
            var ready = false
            while (!ready) {
                waitConnectLooping = true
                if (mSocketWrapper!!.mAllStop || stopWaitConnect)
                    break

                if (isSomebodyOnline) {
                    if (mP2PMesh?.mConnections!!.values.isEmpty()) {
                        Thread.sleep(500)
                        continue
                    }
                } else
                    Thread.sleep(500)

                ready = try {
                    if (withoutSelf)
                        !mP2PMesh?.mConnections!!.values.filter { it.myName != uniqueID1 && it.myName != uniqueID2 }.any { !it.mIamReadyToDataTranfer }
                    else
                        !mP2PMesh?.mConnections!!.values.any { !it.mIamReadyToDataTranfer }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            waitConnectLooping = false
        }
        runBlocking { async.await() }
        waitConnectLooping = false
    }

    fun freeCoreHBBFT() {
        Log.d(TAG, "freeCoreHBBFT: FreeConnect")

        mP2PMesh?.FreeConnect()
        mSocketWrapper?.mAllStop = true

        WriteAnyObjectToFile(mNotSyncMessInDialog as Any, APP_PREFERENCES_MESSDATE)

        Log.d(TAG, "freeCoreHBBFT: setOfflineModeInRoomInFirebase")
        for (roomId in listFlagsUpdatedStateToOnline.keys)
            setOfflineModeInRoomInFirebase(roomId)
    }

    private fun generateOrGetUID() {
        Log.d(TAG, "generateOrGetUID")

        val uiid = ReadObjectFromFile(APP_PREFERENCES_NAME1)
        val uiid1 = ReadObjectFromFile(APP_PREFERENCES_NAME2)

        if (uiid == null || uiid == "") {
            uniqueID1 = UUID.randomUUID().toString()
            Log.d(TAG, "generateOrGetUID: uniqueID1 $uniqueID1")
            WriteObjectToFile(uniqueID1, APP_PREFERENCES_NAME1)
        } else
            uniqueID1 = uiid

        if (uiid1 == null || uiid1 == "") {
            uniqueID2 = UUID.randomUUID().toString()
            Log.d(TAG, "generateOrGetUID: uniqueID2 $uniqueID2")
            WriteObjectToFile(uniqueID2, APP_PREFERENCES_NAME2)
        } else
            uniqueID2 = uiid1
    }

    override fun dataReceived(bytes: ByteArray) {
        Log.d(TAG, "dataReceived ${bytes.size} bytes")
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

            if (!listFlagsUpdatedStateToOnline.containsKey(mCurRoomId) || listFlagsUpdatedStateToOnline[mCurRoomId] == false) {
                listFlagsUpdatedStateToOnline[mCurRoomId] = true
                Log.d(TAG, "subscribeSession: listFlagsUpdatedStateToOnline")
                for (hl in listeners) {
                    hl?.updateStateToOnline()
                }
            }

            if (!uid.isEmpty() && !mes.isEmpty() && uid != uniqueID2 && uid != uniqueID1) {
                getHistOrSync(uid)

                if (lastMes == mes && uid == lastUid) {
                    val mestime = Calendar.getInstance().timeInMillis
                    if (abs(mestime - lastMestime) < 500) {
                        Log.d(TAG, "subscribeSession: clear mes because  lastMes == mes ")
                        lastMestime = Calendar.getInstance().timeInMillis
                        return@subscribe
                    }
                }

                val messJson = getJsonArrayMess(mes)
                for (i in 0 until messJson!!.length()) {
                    var mess = messJson.getString(i)
                    if(mess.endsWith('!'))
                        mess = mess.removeRange(mess.count() - 1, mess.count())

                    Log.d(TAG, "subscribeSession: start parseMessage ")

                    parseMessage(mess, uid)
                }

                lastMes = mes
                lastUid = uid
                lastMestime = Calendar.getInstance().timeInMillis
            }
        }
        session?.subscribe { _: String, _: String ->

        }
    }

    private fun getHistOrSync(uid: String) {
        if (!listFlagsSendRequestToHistory[mCurRoomId]!! && !mFlagSendNotSync) {
            Log.d(TAG, "getHistOrSync: sendMessageGETHIST")
            listFlagsSendRequestToHistory[mCurRoomId] = true
            sendMessageGETHIST(uid)
        } else if (listFlagsSendRequestToHistory[mCurRoomId]!! && !mFlagSendNotSync) {
            if (mFlagsSyncMess[mCurRoomId] != null && !mFlagsSyncMess[mCurRoomId]!!) {
                Log.d(TAG, "getHistOrSync: try requestNotSyncMes")
                requestNotSyncMes(uid)
            }
        }
    }

    private fun getJsonArrayMess(mes: String): JSONArray? {
        var message = mes.replace("[Transaction ", "")
        message = message.removeRange(message.count() - 1, message.count())
        val jsonObj = JSONObject(message)
        val messJson = jsonObj.getJSONArray("trVec")
        return messJson
    }

    inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

    private fun parseMessage(mess: String, uid: String) {
        val commMes = mess.split("᳀†\u058D:")
        if(commMes.size >= 2) when(commMes[0]) {
            MessageType.ILIVE.name -> {
                Log.d(TAG, "parseMessage: MessageType.ILIVE ")

                if (listOnlineUsers[mCurRoomId] != null && listOnlineUsers[mCurRoomId]!!.contains(uid))
                    return
                else {
                    try {
                        if (listOnlineUsers[mCurRoomId] == null)
                            listOnlineUsers[mCurRoomId] = arrayListOf()

                        listOnlineUsers[mCurRoomId]!!.add(uid)
                    } catch (e: Exception) {
                        Log.d(TAG, "parseMessage: ERROR MessageType.ILIVE ")
                        e.printStackTrace()
                    }
                    handler.post {
                        Log.d(TAG, "parseMessage: setUnOnlineUserWithUID $uid")

                        setUnOnlineUserWithUID(uid, true)
                        for (hl in listeners)
                            hl?.setOnlineUser(uid, true)
                    }
                }
            }
            MessageType.IDIE.name -> {
                Log.d(TAG, "parseMessage: MessageType.IDIE ")

                if (listOnlineUsers[mCurRoomId] != null && !listOnlineUsers[mCurRoomId]!!.contains(uid))
                    return
                else {
                    listOnlineUsers[mCurRoomId]!!.remove(uid)
                    handler.post {
                        setUnOnlineUserWithUID(uid, false)
                        for (hl in listeners)
                            hl?.setOnlineUser(uid, false)
                    }
                }
            }
            MessageType.MESS.name -> {
                Log.d(TAG, "parseMessage: MessageType.MESS ")
                try {
                    val you = uid == uniqueID1 || uid == uniqueID2
                    val date = commMes[1].toDate(format, TimeZone.getDefault())
                    for (hl in listeners)
                        hl?.reciveMessage(you, uid, commMes[2], date)
                } catch (e: Exception) {
                    Log.d(TAG, "ERROR parse MESS incoming message")
                    e.printStackTrace()
                }
            }
            MessageType.GETHIST.name -> {
                Log.d(TAG, "parseMessage: MessageType.GETHIST ")
                try {
                    if (commMes[1] == uniqueID1 || commMes[1] == uniqueID2) {
                        val curRoom = commMes[2]
                        val lessDate = commMes[3].toDate(format, TimeZone.getDefault())
                        val greaterDate = commMes[4].toDate(format, TimeZone.getDefault())

                        val messeges = getMessagesLessGreaterDate(lessDate, greaterDate, curRoom).filterNotNull()
                        messeges.forEach {
                            it.createdAt?.formatTo(format, TimeZone.getTimeZone("UTC"))
                                ?.toDate(format, TimeZone.getTimeZone("UTC"))
                        }
                        val mes =
                            "SENDHIST᳀†\u058D:$uid᳀†\u058D:${uniqueID1}᳀†\u058D:${mCurRoomId}᳀†\u058D:${GsonBuilder().setDateFormat(
                                DateFormat.FULL,
                                DateFormat.FULL
                            ).create().toJson(messeges)}"

                        Log.d(TAG, "parseMessage: MessageType.GETHIST session?.send_message")
                        session?.send_message(mes)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "ERROR parse GETHIST incoming message")
                    e.printStackTrace()
                }
            }
            MessageType.GETSYNC.name -> {
                Log.d(TAG, "parseMessage: MessageType.GETSYNC ")
                try {
                    if (commMes[1] == uniqueID1 || commMes[1] == uniqueID2) {
                        val curRoom = commMes[2]
                        val lessDate = commMes[3].toDate(format, TimeZone.getDefault())
                        val greaterDate = commMes[4].toDate(format, TimeZone.getDefault())

                        val messeges = getMessagesLessGreaterDate(lessDate, greaterDate, curRoom).filterNotNull()
                        messeges.forEach {
                            it.createdAt?.formatTo(format, TimeZone.getTimeZone("UTC"))
                                ?.toDate(format, TimeZone.getTimeZone("UTC"))
                        }
                        val mes =
                            "SENDSYNC᳀†\u058D:$uid᳀†\u058D:${uniqueID1}᳀†\u058D:${mCurRoomId}᳀†\u058D:${GsonBuilder().setDateFormat(
                                DateFormat.FULL,
                                DateFormat.FULL
                            ).create().toJson(messeges)}"

                        Log.d(TAG, "parseMessage: MessageType.GETSYNC session?.send_message")
                        session?.send_message(mes)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "ERROR parse GETSYNC incoming message")
                    e.printStackTrace()
                }
            }
            MessageType.SENDSYNC.name -> {
                Log.d(TAG, "parseMessage: MessageType.SENDSYNC ")
                try {
                    if (commMes[1] == uniqueID1 || commMes[1] == uniqueID2) {
                        mFlagSendNotSync = false
                        val uidFrom = commMes[2]
                        val uidRoom = commMes[3]

                        // get dialog if not current
                        var curDialog: Dialog? = null
                        if (uidRoom == mCurRoomId)
                            curDialog = getDialog(uidRoom)

                        if (commMes[4] == "[]") {
                            Log.d(TAG, "parseMessage: MessageType.SENDSYNC - Empty mess")
                            if (mSyncUsersUids[mCurRoomId] == null)
                                mSyncUsersUids[mCurRoomId] = arrayListOf()
                            mSyncUsersUids[mCurRoomId]!!.add(uid)
                            return
                        }

                        var messages: List<Message> =
                            GsonBuilder().setDateFormat(DateFormat.FULL, DateFormat.FULL).create().fromJson(commMes[4])
                        messages = messages.sortedBy { it.createdAt }
                        for (mess in messages) {
                            addMessageToFragment(mess, uidRoom, curDialog, "SENDSYNC")
                        }

                        //  send not sync mes to get and save mNotSyncMessInDialog
                        requestNotSyncMes(uidRoom)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "ERROR parse SENDSYNC incoming message")
                    e.printStackTrace()
                }
            }
            MessageType.SENDHIST.name -> {
                Log.d(TAG, "parseMessage: MessageType.SENDHIST ")
                try {
                    if (commMes[1] == uniqueID1 || commMes[1] == uniqueID2) {
                        mFlagSendNotSync = false
                        val uidFrom = commMes[2]
                        val uidRoom = commMes[3]

                        // get dialog if not current
                        var curDialog: Dialog? = null
                        if (uidRoom == mCurRoomId)
                            curDialog = getDialog(uidRoom)

                        if (commMes[4] == "[]") {
                            Log.d(TAG, "parseMessage: MessageType.SENDHIST - Empty mess")
                            return
                        }

                        var messages: List<Message> =
                            GsonBuilder().setDateFormat(DateFormat.FULL, DateFormat.FULL).create().fromJson(commMes[4])
                        messages = messages.sortedBy { it.createdAt }
                        for (mess in messages) {
                            addMessageToFragment(mess, uidRoom, curDialog, "SENDHIST")
                        }

                        //  send not sync mes to get and save mNotSyncMessInDialog
                        requestNotSyncMes(uidRoom)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "ERROR parse SENDHIST incoming message")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun requestNotSyncMes(uidRoom: String) {
        if (mNotSyncMessInDialog.containsKey(uidRoom) && mNotSyncMessInDialog[uidRoom]!!.isNotEmpty()) {
            Log.d(TAG, "requestNotSyncMes: getRandUID   mFlagsSyncMess - false")
            val randUid = getRandUID(uidRoom)
            if (randUid != "")
                sendNotSync(randUid)
        } else if (mNotSyncMessInDialog.containsKey(uidRoom) && mNotSyncMessInDialog[uidRoom]!!.isEmpty()) {
            Log.d(TAG, "requestNotSyncMes: mFlagsSyncMess - true")
            mFlagsSyncMess[uidRoom] = true
        } else if (!mNotSyncMessInDialog.containsKey(uidRoom)) {
            Log.d(TAG, "requestNotSyncMes: mFlagsSyncMess - true")
            mFlagsSyncMess[uidRoom] = true
        }
    }

    private fun addMessageToFragment(
        mess: Message,
        uidRoom: String,
        curDialog: Dialog?,
        subTag: String
    ) {
        var curDialog1 = curDialog
        try {
            // if deleted message
            if (!mess.isVisible && mess.text == "") {
                Log.d(TAG, "addMessageToFragment: MessageType.$subTag - deleted message")
                if (mNotSyncMessInDialog[uidRoom] == null)
                    mNotSyncMessInDialog[uidRoom] = arrayListOf()

                mNotSyncMessInDialog[uidRoom]!!.add(
                    mess.createdAt!!.formatTo(
                        format,
                        TimeZone.getTimeZone("UTC")
                    )
                )
            }
            // if message receive
            else {
                val you = mess.user.uid == uniqueID1 || mess.user.uid == uniqueID2
                val dateString = mess.createdAt?.formatTo(format, TimeZone.getTimeZone("UTC"))!!

                // if message was delete in other user but in this exist
                if (mNotSyncMessInDialog.containsKey(uidRoom) && mNotSyncMessInDialog[uidRoom]!!.contains(
                        dateString
                    )
                ) {
                    Log.d(TAG, "addMessageToFragment: MessageType.$subTag - received deleted message")
                    mNotSyncMessInDialog[uidRoom]!!.remove(dateString)
                }

                val date = dateString.toDate(format, TimeZone.getDefault())
                // if current dialog - then show new message
                if (uidRoom == mCurRoomId) {
                    Log.d(
                        TAG,
                        "addMessageToFragment: MessageType.$subTag - current dialog - then show new message - reciveMessage"
                    )
                    for (hl in listeners)
                        hl?.reciveMessage(you, mess.user.uid, mess.text!!, date)
                }
                // if not current dialog - then get user and save mes
                else {
                    if (curDialog1 != null) {
                        Log.d(
                            TAG,
                            "addMessageToFragment: MessageType.$subTag - not current dialog - getUserFromLocalOrDownloadFromFirebase"
                        )
                        if (!curDialog1.users.any { it.uid == mess.user.uid }) {
                            // user not exist - download it
                            getUserFromLocalOrDownloadFromFirebase(
                                mess.user.uid,
                                curDialog1.id,
                                object : IAddToContacts {
                                    override fun errorAddContact() {
                                    }

                                    override fun user(user: User) {
                                        handlerNewMes.post {
                                            curDialog1!!.users.add(user)
                                            Conversations.getDUser(user).insert()

                                            Log.d(
                                                TAG,
                                                "addMessageToFragment: MessageType.$subTag - not current dialog - getting user and add message"
                                            )

                                            val userMes = Getters.getUserbyUIDFromDialog(
                                                mess.user.uid,
                                                curDialog1!!.id
                                            )
                                            val mess = MessagesFixtures.setNewMessage(
                                                mess.text!!,
                                                curDialog1!!,
                                                userMes!!,
                                                date
                                            )
                                            curDialog1 = getDialog(curDialog1!!.id)
                                        }
                                    }
                                })
                        } else {
                            // past message
                            handlerNewMes.post {
                                Log.d(
                                    TAG,
                                    "addMessageToFragment: MessageType.$subTag - not current dialog - add message"
                                )

                                val user = Getters.getUserbyUIDFromDialog(
                                    mess.user.uid,
                                    curDialog1!!.id
                                )
                                val mess = MessagesFixtures.setNewMessage(
                                    mess.text!!,
                                    curDialog1!!,
                                    user!!,
                                    date
                                )
                                curDialog1 = getDialog(curDialog1!!.id)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "addMessageToFragment: ERROR parse current message MessageType.$subTag ")
            e.printStackTrace()
        }
    }

    private fun getRandUID(uidRoom: String): String {
        Log.d(TAG, "getRandUID")
        val uid: String
        val list = listOnlineUsers[uidRoom] ?: return ""

        try {
            if (mSyncUsersUids[uidRoom] == null || !mSyncUsersUids.containsKey(uidRoom))
                return list.first()

            uid = list.first { !mSyncUsersUids[uidRoom]!!.contains(it) }
        } catch (e: NoSuchElementException) {
            return ""
        }

        Log.d(TAG, "getRandUID: uid - $uid")
        return uid
    }

    private fun sendMessageGETHIST(uid: String) {
        try {
            lateinit var lastmesDate: String
            val lastMes = getMessagesLessDate(Date(), mCurRoomId)
            lastmesDate = if (lastMes == null || lastMes.isEmpty()) {
                dateLastSync.formatTo(format, TimeZone.getTimeZone("UTC"))
            } else {
                lastMes[0]!!.createdAt!!.formatTo(format, TimeZone.getTimeZone("UTC"))
            }
            val curDate = Date().formatTo(format, TimeZone.getTimeZone("UTC"))

            Log.d(TAG, "sendMessageGETHIST: to uid - $uid from date - $lastmesDate to date - $curDate")
            val mes = "GETHIST᳀†\u058D:$uid᳀†\u058D:${mCurRoomId}᳀†\u058D:$lastmesDate᳀†\u058D:$curDate"

            mFlagSendNotSync = true
            session?.send_message(mes)
        } catch (e: Exception) {
            Log.d(TAG, "sendMessageGETHIST: ERROR uid - $uid")
            e.printStackTrace()
        }
    }

    private fun sendNotSync(uid: String) {
        if (mNotSyncMessInDialog.containsKey(mCurRoomId) && mNotSyncMessInDialog[mCurRoomId] != null && mNotSyncMessInDialog[mCurRoomId]!!.isNotEmpty()) {
            for (value in mNotSyncMessInDialog[mCurRoomId]!!) {
                Log.d(TAG, "sendNotSync: to uid - $uid date - $value")
                val mes = "GETSYNC᳀†\u058D:$uid᳀†\u058D:${mCurRoomId}᳀†\u058D:$value᳀†\u058D:$value"

                mFlagSendNotSync = true
                session?.send_message(mes)
            }
        } else if (mNotSyncMessInDialog.containsKey(mCurRoomId) && mNotSyncMessInDialog[mCurRoomId]!!.isEmpty()) {
            Log.d(TAG, "sendNotSync: mFlagsSyncMess - true")
            mFlagsSyncMess[mCurRoomId] = true
        } else if (!mNotSyncMessInDialog.containsKey(mCurRoomId)) {
            Log.d(TAG, "sendNotSync: mFlagsSyncMess - true")
            mFlagsSyncMess[mCurRoomId] = true
        }
    }

    fun sendMessageIDIE() {
        Log.d(TAG, "sendMessageIDIE")
        val mes = "IDIE᳀†\u058D:$uniqueID1"
        session?.send_message(mes)
    }

    fun sendMessage(str: String) {
        Log.d(TAG, "sendMessage")
        if (str.isNotEmpty()) {
            val dateString = Date().formatTo(format, TimeZone.getTimeZone("UTC"))
            val mes = "MESS᳀†\u058D:${dateString}᳀†\u058D:$str"
            session?.send_message(mes)
        }
    }
}