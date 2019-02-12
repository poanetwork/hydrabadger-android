package net.korul.hbbft.CoreHBBFT

import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.P2P.*
import net.korul.hbbft.Session
import net.korul.hbbft.services.ClosingService
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.abs

interface CoreHBBFTListener {
    fun updateStateToOnline()

    fun reciveMessage(you: Boolean, uid: String, mes: String)
}

class CoreHBBFT: IGetData {
    // p2p
    var mP2PMesh: P2PMesh? = null
    var mSocketWrapper: SocketWrapper? = null
    var mSocketWrapper2X: SocketWrapper2X? = null

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


    init {
        System.loadLibrary("hydra_android")

        session = net.korul.hbbft.Session()

        generateOrGetUID()
    }

    fun Init(applicationContext: Context) {
        val serviceIntent = Intent(applicationContext, ClosingService::class.java)
        applicationContext.startService(serviceIntent)

        mP2PMesh  = P2PMesh(applicationContext, this)
        mSocketWrapper2X = SocketWrapper2X(mP2PMesh!!)
        mSocketWrapper   = SocketWrapper(mP2PMesh!!)
    }

    fun start_node(RoomName: String) {
        mRoomName = RoomName

        mP2PMesh?.initOneMesh(RoomName, uniqueID1)
        mP2PMesh?.publishAboutMe(RoomName, uniqueID1)

        waitForConnect()

        mSocketWrapper!!.initSocketWrapper(RoomName, uniqueID1, mP2PMesh!!.usersCon.toList())

        thread {
            var strTosend = ""
            for(clients in mSocketWrapper!!.clientsBusyPorts) {
                if(clients.key != uniqueID1)
                    strTosend += "127.0.0.1:${clients.value};"
            }
            if (strTosend.endsWith(";"))
                strTosend = strTosend.substring(0, strTosend.length - 1)

            session?.start_node(
                "127.0.0.1:${mSocketWrapper!!.myLocalPort}",
                strTosend
            )
        }
    }

    fun start_node_2x(RoomName: String) {
        mRoomName = RoomName

        mP2PMesh?.initOneMesh(RoomName, uniqueID1)
        mP2PMesh?.initOneMesh(RoomName, uniqueID2)
        mP2PMesh?.publishAboutMe(RoomName, uniqueID1)
        Thread.sleep(100)
        mP2PMesh?.publishAboutMe(RoomName, uniqueID2)

        waitForConnect2()

        mSocketWrapper2X!!.initSocketWrapper(RoomName, uniqueID1, uniqueID2, mP2PMesh!!.usersCon.toList())

        thread {
            var strTosend = ""
            for(clients in mSocketWrapper2X!!.clientsBusyPorts) {
                if(clients.key != uniqueID1 && clients.key != uniqueID2)
                    strTosend += "127.0.0.1:${clients.value};"
            }
            if (strTosend.endsWith(";"))
                strTosend = strTosend.substring(0, strTosend.length - 1)

            session?.start_node(
                    "127.0.0.1:${mSocketWrapper2X!!.myLocalPort1}",
                    strTosend
            )

            strTosend = ""
            for(clients in mSocketWrapper2X!!.clientsBusyPorts) {
                if(clients.key != uniqueID2) {
                    strTosend += "127.0.0.1:${clients.value};"
                }
            }
            if (strTosend.endsWith(";"))
                strTosend = strTosend.substring(0, strTosend.length - 1)

            session?.start_node(
                "127.0.0.1:${mSocketWrapper2X!!.myLocalPort2}",
                strTosend
            )
        }
    }

    fun waitForConnect2() {
        val async = GlobalScope.async {
            var ready = false
            while (!ready) {
                Thread.sleep(1000)
                ready = true
                for (con in mP2PMesh?.mConnections!!.values) {
                    if(con.myName == uniqueID1 || con.myName == uniqueID2)
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
        mSocketWrapper2X?.mAllStop = true
        mSocketWrapper?.mAllStop = true
    }

    override fun dataReceived(bytes: ByteArray) {
        if(mSocketWrapper2X?.mStarted != null && mSocketWrapper2X?.mStarted!!)
            mSocketWrapper2X?.sendReceivedDataToHydra(bytes)
        if(mSocketWrapper?.mStarted != null && mSocketWrapper?.mStarted!!)
            mSocketWrapper?.sendReceivedDataToHydra(bytes)
    }


    fun addListener(toAdd: CoreHBBFTListener?) {
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
                for (hl in listeners) {
                    hl?.updateStateToOnline()
                    mUpdateStateToOnline = true
                }
            }

            if (!uid.isEmpty() && !mes.isEmpty() && mes != "[None]" && uid != uniqueID2) {
                if(lastMes == mes) {
                    val mestime = Calendar.getInstance().timeInMillis
                    if(abs(mestime - lastMestime) < 500) {
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
    }

    fun sendMessage(str: String) {
        if(str.isNotEmpty()) {
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