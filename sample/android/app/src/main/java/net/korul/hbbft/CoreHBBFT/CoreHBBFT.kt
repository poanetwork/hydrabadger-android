package net.korul.hbbft.CoreHBBFT

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.korul.hbbft.CommonFragments.tabContacts.IAddToContacts
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.P2P.IGetData
import net.korul.hbbft.P2P.P2PMesh
import net.korul.hbbft.P2P.SocketWrapper
import net.korul.hbbft.R
import net.korul.hbbft.Session
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.conversation.Conversations
import net.korul.hbbft.common.data.model.core.Getters
import net.korul.hbbft.common.data.model.core.Getters.getAllUsers
import net.korul.hbbft.common.data.model.core.Getters.getAllUsersDistinct
import net.korul.hbbft.common.data.model.core.Getters.updateUserbyUID
import net.korul.hbbft.firebaseStorage.MyDownloadService
import net.korul.hbbft.services.ClosingService
import org.json.JSONObject
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.math.abs


interface CoreHBBFTListener {
    fun updateStateToOnline()

    fun reciveMessage(you: Boolean, uid: String, mes: String)
}

data class Uids(
    var UID: String? = null,
    var isOnline: Boolean? = null
)

data class Users(
    var UID: String? = null,
    var isOnline: Boolean? = null,
    var name: String? = null,
    var nick: String? = null
)


object CoreHBBFT : IGetData {
    // p2p
    var mP2PMesh: P2PMesh? = null
    var mSocketWrapper: SocketWrapper? = null

    val APP_PREFERENCES = "mysettings"
    val APP_PREFERENCES_NAME1 = "UUID1" // UUID
    val APP_PREFERENCES_NAME2 = "UUID2" // UUID

    lateinit var uniqueID1: String
    lateinit var uniqueID2: String

    private val TAG = "HYDRABADGERTAG"

    var session: Session? = null

    var mShowError: Boolean = true

    private val listeners = ArrayList<CoreHBBFTListener?>()

    var mUpdateStateToOnline = false
    var mRoomName: String = ""

    var lastMes: String = ""
    var lastMestime = Calendar.getInstance().timeInMillis

    private val mFunctions: FirebaseFunctions
    private var mAuth: FirebaseAuth

    private lateinit var mApplicationContext: Context
    private var mDatabase: DatabaseReference

    lateinit var broadcastReceiver: BroadcastReceiver

    init {
        System.loadLibrary("hydra_android")

        session = net.korul.hbbft.Session()

        generateOrGetUID()

        mFunctions = FirebaseFunctions.getInstance()
        mAuth = FirebaseAuth.getInstance()

        mDatabase = FirebaseDatabase.getInstance().reference

//        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }

    fun Init(applicationContext: Context) {
        val serviceIntent = Intent(applicationContext, ClosingService::class.java)
        applicationContext.startService(serviceIntent)

        mP2PMesh = P2PMesh(applicationContext, this)
        mSocketWrapper = SocketWrapper(mP2PMesh!!)

        mApplicationContext = applicationContext

        registerForPush(applicationContext)

        FirebaseMessaging.getInstance().subscribeToTopic("$uniqueID1")
            .addOnCompleteListener { task ->
                var msg = mApplicationContext.getString(R.string.msg_subscribed)
                if (!task.isSuccessful) {
                    msg = mApplicationContext.getString(R.string.msg_subscribe_failed)
                }
                Log.d(TAG, msg)
            }

        thread {
            authAnonymously()
        }

        initUser()
    }

    fun setRoomName(roomname: String) {
        mRoomName = roomname
    }

    fun registerForPush(applicationContext: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            val channelId = applicationContext.getString(R.string.default_notification_channel_id)
            val channelName = applicationContext.getString(R.string.default_notification_channel_name)
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                // Log
                val msg = applicationContext.getString(R.string.msg_token_fmt, token)
                Log.d(TAG, msg)

                DatabaseApplication.mToken = token.toString()
            })
    }

    fun authAnonymously(): CountDownLatch {
        val latch = CountDownLatch(1)
        mAuth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInAnonymously:success")
                    val user = mAuth.currentUser
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInAnonymously:failure", task.exception)
                    Toast.makeText(
                        mApplicationContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                latch.countDown()
            }
            .addOnCanceledListener {
                Toast.makeText(
                    mApplicationContext, "Authentication canceled.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    mApplicationContext, "Authentication Failure.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        return latch
    }


    fun preSendPushToStart(listOfUIDs: List<String>, text: String, title: String) {
        val currentUser = mAuth.currentUser

        if (currentUser == null) {
            val latch = authAnonymously()
            latch.await()
        }

        for (uid in listOfUIDs)
            sendPushToTopic(uid, title, text)
    }

    fun sendPushToTopic(UIDtopic: String, title: String, text: String): Task<String> {
        val json = JSONObject()
        json.put("text", text)
        json.put("title", title)
        json.put("topic", UIDtopic)

        return mFunctions
            .getHttpsCallable("sendToTopic")
            .call(json)
            .continueWith(object : Continuation<HttpsCallableResult, String> {
                override fun then(task: Task<HttpsCallableResult>): String {
                    return task.result?.data.toString()
                }
            })
    }


    fun initUser() {
        val prefs = mApplicationContext.getSharedPreferences("HYRABADGER", Application.MODE_PRIVATE)
        val strUser = prefs!!.getString("CurUser", "")
        if (strUser == null || strUser.isEmpty()) {
            val user = User(
                0,
                DatabaseApplication.mCoreHBBFT2X.uniqueID1,
                0.toString(),
                "",
                "name",
                "nick",
                "",
                true
            )

            val editor = prefs.edit()
            editor.putString("CurUser", Gson().toJson(user))
            editor.apply()

            DatabaseApplication.mCurUser = user
            Conversations.getDUser(user).insert()
        } else
            DatabaseApplication.mCurUser = Gson().fromJson(strUser, User::class.java)

        registerUpdateUserInDataBase(DatabaseApplication.mCurUser)
    }

    fun registerUpdateUserInDataBase(user: User) {
        val queryRef = mDatabase.child("Users").child(user.uid).orderByChild("uid").equalTo(user.uid)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val userToSave = Users(
                    user.uid,
                    user.isOnline,
                    user.name,
                    user.nick
                )

                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }
                snapshot.ref.push().setValue(userToSave)
                //TODO update all in database
                Log.d(TAG, "Succes registerUpdateUserInDataBase $uniqueID1")
            }
        })
    }

    fun saveCurUser(user: User) {
        updateUserbyUID(uniqueID1, user)
        val prefs = mApplicationContext.getSharedPreferences("HYRABADGER", Application.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("CurUser", Gson().toJson(user))
        editor.apply()

        registerUpdateUserInDataBase(user)
    }

    fun saveCurUser() {
        updateUserbyUID(uniqueID1, DatabaseApplication.mCurUser)
        val prefs = mApplicationContext.getSharedPreferences("HYRABADGER", Application.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("CurUser", Gson().toJson(DatabaseApplication.mCurUser))
        editor.apply()

        registerUpdateUserInDataBase(DatabaseApplication.mCurUser)
    }

    fun AddUser(uid: String, listener: IAddToContacts): User? {
        val ref = mDatabase.child("Users").child(uid)

        val latch = CountDownLatch(1)
        val listObjectsOfUsers: MutableList<Users> = arrayListOf()
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val objectMap = dataSnapshot.value as HashMap<String, Any>
                for (obj in objectMap.values) {
                    val mapObj: Map<String, Any> = obj as Map<String, Any>

                    val user = Users()
                    user.UID = mapObj["uid"] as String
                    user.isOnline = mapObj["online"] as Boolean
                    user.name = mapObj["name"] as String
                    user.nick = mapObj["nick"] as String

                    listObjectsOfUsers.add(user)
                }
                try {
                    latch.countDown()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        ref.addValueEventListener(postListener)
        latch.await()

        listObjectsOfUsers.distinctBy { it.UID }
        if (listObjectsOfUsers.isEmpty())
            listener.errorAddContact()
        else {
            for (user in listObjectsOfUsers) {
                val id = Getters.getNextUserID()
                val users = User(
                    id,
                    uid,
                    id.toString(),
                    (-1).toString(),
                    user.name!!,
                    user.nick!!,
                    "",
                    user.isOnline!!
                )
                Conversations.getDUser(users).insert()

                // Kick off MyDownloadService to download the file
                val intent = Intent(mApplicationContext, MyDownloadService::class.java)
                    .putExtra(MyDownloadService.EXTRA_DOWNLOAD_USERID, user.UID)
                    .setAction(MyDownloadService.ACTION_DOWNLOAD)
                mApplicationContext.startService(intent)
            }

            return getLocalUser(uid)
        }

        return null
    }

    fun getUserFromLocalOrDownload(uid: String, dialog: Dialog): User {
        val LocalUser = getLocalUser(uid)
        if (LocalUser != null) {
            val id = Getters.getNextUserID()
            val user = User(
                id,
                uid,
                id.toString(),
                dialog.id,
                LocalUser.name,
                LocalUser.nick,
                LocalUser.avatar,
                LocalUser.isOnline
            )

            return user
        } else {
            val User = AddUser(uid, object : IAddToContacts {
                override fun errorAddContact() {
                }
            })

            val id = Getters.getNextUserID()
            val user = User(
                id,
                uid,
                id.toString(),
                dialog.id,
                User!!.name,
                User.nick,
                User.avatar,
                User.isOnline
            )

            return user
        }
    }

    fun updateAvatarInAllUserByUid(uid: String, avatarFile: File) {
        for (user in getAllUsers(uid)) {
            if (user.uid == uid) {
                val us = User(
                    user.id_,
                    user.uid,
                    user.id,
                    user.idDialog,
                    user.name,
                    user.nick,
                    avatarFile.path,
                    user.isOnline
                )

                Conversations.getDUser(us).update()
            }
        }
    }

    fun getLocalUser(uid: String): User? {
        for (user in getAllUsersDistinct()) {
            if (user.uid == uid)
                return user
        }
        return null
    }


    fun unregisterUser(uid: String) {
        val queryRef = mDatabase.child("Users").child(uid).orderByChild("uid").equalTo(uid)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }
                Log.d(TAG, "Succes unregisterUser $uniqueID1")
            }
        })
    }

    fun unregisterInDatabase(RoomName: String) {
        val queryRef = mDatabase.child("Rooms").child(RoomName).orderByChild("uid").equalTo(uniqueID1)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    for (postsnapshot in snapshot.children) {
                        postsnapshot.key
                        postsnapshot.ref.removeValue()
                    }
                    Log.d(TAG, "Succes unregisterInDatabase $uniqueID1")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    fun registerInDatabase(RoomName: String) {
        val queryRef = mDatabase.child("Rooms").child(RoomName).orderByChild("uid").equalTo(uniqueID1)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    for (postsnapshot in snapshot.children) {
                        postsnapshot.key
                        postsnapshot.ref.removeValue()
                    }
                    Log.d(TAG, "Succes unregisterInDatabase $uniqueID1")

                    // Add user
                    val uid = Uids()
                    uid.UID = uniqueID1
                    uid.isOnline = false
                    val ref = mDatabase.child("Rooms").child(RoomName)
                    ref.push().setValue(uid).addOnSuccessListener {
                        Log.d(TAG, "Succes registerInDatabase $uniqueID1")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    fun setOfflineModeToDatabase(RoomName: String) {
        val queryRef = mDatabase.child("Rooms").child(RoomName).orderByChild("uid").equalTo(uniqueID1)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val uid = Uids()
                uid.UID = uniqueID1
                uid.isOnline = false
                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }
                snapshot.ref.push().setValue(uid)
                Log.d(TAG, "Succes setOnlineModeToDatabase $uniqueID1")
            }
        })
    }

    fun setOnlineModeToDatabase(RoomName: String) {
        val queryRef = mDatabase.child("Rooms").child(RoomName).orderByChild("uid").equalTo(uniqueID1)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }

                val uid = Uids()
                uid.UID = uniqueID1
                uid.isOnline = true
                val ref = snapshot.ref.push()
                ref.setValue(uid)

                val uid1 = Uids()
                uid1.UID = uniqueID1
                uid1.isOnline = false
                ref.onDisconnect().setValue(uid1)

                Log.d(TAG, "Succes setOnlineModeToDatabase $uniqueID1")
            }
        })
    }

    fun getUIDsFromDataBase(RoomName: String): MutableList<Uids> {
        val ref = mDatabase.child("Rooms").child(RoomName)

        val latch = CountDownLatch(1)
        val listObjectsOfUIds: MutableList<Uids> = arrayListOf()
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val objectMap = dataSnapshot.value as HashMap<String, Any>
                for (obj in objectMap.values) {
                    val mapObj: Map<String, Any> = obj as Map<String, Any>
                    val uids = Uids()
                    uids.UID = mapObj["uid"] as String
                    uids.isOnline = mapObj["online"] as Boolean

                    listObjectsOfUIds.add(uids)
                }
                try {
                    latch.countDown()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        ref.addValueEventListener(postListener)
        latch.await()

        listObjectsOfUIds.distinctBy { it.UID }
        return listObjectsOfUIds
    }

    fun isSomeBodyOnline(listOfUids: List<Uids>): Boolean {
        for (uids in listOfUids) {
            if (uids.isOnline!!)
                return true
        }
        return false
    }

    fun startAllNode(RoomName: String) {
        thread {
            val listObjectsOfUIds = getUIDsFromDataBase(RoomName)
            val isSomebodyOnline = isSomeBodyOnline(listObjectsOfUIds)
            val cntUsers = listObjectsOfUIds.count()

            // if 1 user
            if (cntUsers < 2) {
                Toast.makeText(mApplicationContext, "Room is empty", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Room is empty $RoomName")

            }
            // if 2 users and i am first
            else if (cntUsers == 2 && !isSomebodyOnline) {
                start_node_2x(RoomName)

                var uid = ""
                for (ui in listObjectsOfUIds) {
                    if (ui.UID != uniqueID1) {
                        uid = ui.UID!!
                        break
                    }
                }
                if (uid.isNotEmpty()) {
                    Log.d(TAG, "2 users and i am first in room $RoomName")
                    preSendPushToStart(listOf(uid), RoomName, uniqueID1)
                } else {
                    Log.d(TAG, "Room uid is empty $RoomName")
                    Toast.makeText(mApplicationContext, "Room uid is empty", Toast.LENGTH_LONG).show()
                }
            }
            // if 2 users and second start
            else if (cntUsers == 2 && isSomebodyOnline) {
                Log.d(TAG, "2 users and second start in room $RoomName")
                start_node(RoomName)
            }
            // if many users and i am first
            else if (cntUsers > 2 && !isSomebodyOnline) {
                start_node(RoomName)

                val uids = mutableListOf<String>()
                for (ui in listObjectsOfUIds) {
                    if (ui.UID != uniqueID1) {
                        uids.add(ui.UID!!)
                    }
                }
                if (uids.isNotEmpty()) {
                    Log.d(TAG, "many users and i am first in room $RoomName")
                    preSendPushToStart(uids, RoomName, uniqueID1)
                } else {
                    Log.d(TAG, "Room uid is empty $RoomName")
                    Toast.makeText(mApplicationContext, "Room uids is empty", Toast.LENGTH_LONG).show()
                }
            }
            // if many users and i am not first
            else {
                Log.d(TAG, "many users and i am not first in room $RoomName")
                start_node(RoomName)
            }
        }
    }


    fun start_node(RoomName: String) {
        setOnlineModeToDatabase(RoomName)

        mRoomName = RoomName

        mP2PMesh?.initOneMesh(RoomName, uniqueID1)
        mP2PMesh?.publishAboutMe(RoomName, uniqueID1)

        waitForConnect()

        mSocketWrapper!!.initSocketWrapper(RoomName, uniqueID1, mP2PMesh!!.usersCon.toList())

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

    fun start_node_2x(RoomName: String) {
        setOnlineModeToDatabase(RoomName)

        mRoomName = RoomName

        mP2PMesh?.initOneMesh(RoomName, uniqueID1)
        mP2PMesh?.initOneMesh(RoomName, uniqueID2)
        mP2PMesh?.publishAboutMe(RoomName, uniqueID1)
        Thread.sleep(100)
        mP2PMesh?.publishAboutMe(RoomName, uniqueID2)

        waitForConnectWithoutSelf()

        mSocketWrapper!!.initSocketWrapper2X(RoomName, uniqueID1, uniqueID2, mP2PMesh!!.usersCon.toList())

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

        setOfflineModeToDatabase(mRoomName)
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
        session?.subscribe { you: Boolean, uid: String, mes: String ->
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

            if (!uid.isEmpty() && !mes.isEmpty() && mes != "[None]" && uid != uniqueID2) {
                if (lastMes == mes) {
                    val mestime = Calendar.getInstance().timeInMillis
                    if (abs(mestime - lastMestime) < 500) {
                        lastMestime = Calendar.getInstance().timeInMillis
                        return@subscribe
                    }
                }

                val str = SpannableStringBuilder()
                var mess = mes.removeRange(0, 19)
                mess = mess.removeRange(mess.count() - 5, mess.count())
                str.append(mess)

                // Notify everybody that may be interested.
                for (hl in listeners)
                    hl?.reciveMessage(you, uid, str.toString())

                lastMes = mes
                lastMestime = Calendar.getInstance().timeInMillis
            }
        }
        session?.subscribe { you: Boolean, uid: String, mes: String ->

        }
    }

    fun sendMessage(str: String) {
        if (str.isNotEmpty()) {
            session?.send_message(str)
        }
    }

    fun generateOrGetUID() {
        val mSettings = DatabaseApplication.instance.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        val uiid = mSettings.getString(APP_PREFERENCES_NAME1, "")
        val uiid1 = mSettings.getString(APP_PREFERENCES_NAME2, "")

        if (uiid == null || uiid == "") {
            uniqueID1 = UUID.randomUUID().toString()

            val editor = mSettings.edit()
            editor.putString(APP_PREFERENCES_NAME1, uniqueID1)
            editor.apply()
        } else
            uniqueID1 = uiid

        if (uiid1 == null || uiid1 == "") {
            uniqueID2 = UUID.randomUUID().toString()

            val editor = mSettings.edit()
            editor.putString(APP_PREFERENCES_NAME2, uniqueID2)
            editor.apply()
        } else
            uniqueID2 = uiid1
    }
}