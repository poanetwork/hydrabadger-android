package net.korul.hbbft.CoreHBBFT

import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.util.Log
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.Session
import net.korul.hbbft.services.ClosingService
import ru.hintsolutions.myapplication2.IGetData
import ru.hintsolutions.myapplication2.P2PMesh
import ru.hintsolutions.myapplication2.SocketWrapper
import java.util.*

interface CoreHBBFTListener {
    fun updateStateToOnline()

    fun reciveMessage(you: Boolean, uid: String, mes: String)
}


class CoreHBBFT: IGetData {
    // p2p
    var mP2PMesh: P2PMesh? = null
    var mSocketWrapper: SocketWrapper? = null

    val APP_PREFERENCES = "mysettings"
    val APP_PREFERENCES_NAME1 = "UUID1" // UUID

    lateinit var uniqueID1: String

    private val TAG = "HYDRABADGERTAG"

    var session: Session? = null

    var mShowError: Boolean = true

    private val listeners = ArrayList<CoreHBBFTListener?>()

    var thread: Thread? = null

    var mUpdateStateToOnline = false
    var mRoomName: String = ""

    // Used to load the 'native-lib' library on application startup.
    init {
        System.loadLibrary("hydra_android")

        session = net.korul.hbbft.Session()

        generateOrGetUID()
    }

    fun Init(applicationContext: Context) {
        val serviceIntent = Intent(applicationContext, ClosingService::class.java)
        applicationContext.startService(serviceIntent)

        mP2PMesh = P2PMesh(applicationContext, this)
        mSocketWrapper = SocketWrapper(mP2PMesh!!)
    }

    fun start_node(RoomName: String) {
        mRoomName = RoomName

        //clear all node
//        mP2PMesh?.clearAllUsersFromDataBase(RoomName)

        mP2PMesh?.initOneMesh(RoomName, uniqueID1)

        mSocketWrapper!!.initSocketWrapper(RoomName, uniqueID1)

        var strTosend = ""
        for(clients in mSocketWrapper!!.clientsBusyPorts) {
            if(clients.key != uniqueID1)
                strTosend += "127.0.0.1:${clients.value};"
        }
        if (strTosend.endsWith(";"))
            strTosend = strTosend.substring(0, strTosend.length - 1)

        thread = Thread {
            Thread.sleep(2000)

            session?.start_node(
                    "127.0.0.1:${mSocketWrapper!!.myLocalPort}",
                    strTosend
            )
        }
        thread!!.start()
    }

    fun Free() {
        mP2PMesh?.Free()
        mSocketWrapper?.mAllStop = true
    }

    override fun dataReceived(bytes: ByteArray) {
        mSocketWrapper?.sendReceivedDataToHydra(bytes)
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
                for (hl in listeners) {
                    hl?.updateStateToOnline()
                    mUpdateStateToOnline = true
                }
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

    fun sendMessage(str: String) {
        if(str.isNotEmpty()) {
            session?.send_message(str)
        }
    }

    fun generateOrGetUID() {
        val mSettings = DatabaseApplication.instance.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        val uiid = mSettings.getString(APP_PREFERENCES_NAME1, "")

        if (uiid == null || uiid == "") {
            uniqueID1 = UUID.randomUUID().toString()

            val editor = mSettings.edit()
            editor.putString(APP_PREFERENCES_NAME1, uniqueID1)
            editor.apply()
        } else
            uniqueID1 = uiid
    }
}