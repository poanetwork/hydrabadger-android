package net.korul.hbbft

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Menu
import android.widget.ScrollView
import com.crashlytics.android.Crashlytics
import com.google.firebase.FirebaseApp
import io.fabric.sdk.android.Fabric
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import net.korul.hbbft.services.ClosingService
import org.json.JSONArray
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

data class Optional<M>(val value : M?)

class MainActivity : AppCompatActivity() {

    var session: Session? = null

    private val lock = ReentrantLock()

    private var TAG = "HYDRA"

    lateinit var thread1: Thread
    lateinit var thread2: Thread
    lateinit var thread3: Thread

    lateinit var text1: String
    lateinit var text2: String
    lateinit var text3: String

    // это будет именем файла настроек
    val APP_PREFERENCES = "mysettings"
    val APP_PREFERENCES_NAME1 = "UUID1" // UUID
    val APP_PREFERENCES_NAME2 = "UUID2" // UUID
    val APP_PREFERENCES_NAME3 = "UUID3" // UUID

    lateinit var uniqueID1: String
    lateinit var uniqueID2: String
    lateinit var uniqueID3: String

    private val use1 = true
    private val use2 = true
    private val use3 = true


    private var mClose: Boolean = false

    private var myIP1 = ""
    private var myPort1: Int = 0
    private var myRecievePort1: Int = 0
    private var myPortForOther1: Int = 0

    private var myIP2 = ""
    private var myPort2: Int = 0
    private var myRecievePort2: Int = 0
    private var myPortForOther2: Int = 0

    private var myIP3 = ""
    private var myPort3: Int = 0
    private var myRecievePort3: Int = 0
    private var myPortForOther3: Int = 0

    private var showError = true

    companion object {
        private val TAG = "Hydrabadger"

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("hydra_android")
        }
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        FirebaseApp.initializeApp(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        session = net.korul.hbbft.Session()

        generateOrGetUID()

        subscribeSession()

        sendButtonInitClickListener()

        session?.after_subscribe()

        if(showError) {
            val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            } else {
                AlertDialog.Builder(this)
            }
            builder.setTitle("Error with dll")
                .setMessage("")
                .setPositiveButton(android.R.string.yes) { dialog, which ->
                    dialog.cancel()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }

        resetAllConnectionsByUids()

        initConnect()

        val serviceIntent = Intent(this, ClosingService::class.java)
        serviceIntent.putExtra("uniqueID1", uniqueID1)
        serviceIntent.putExtra("uniqueID2", uniqueID2)
        serviceIntent.putExtra("uniqueID3", uniqueID3)

        this.startService(serviceIntent)
    }

    fun requestGetClient(RoomName: String, myUid: String): MutableList<String> {
        // 1. Declare a URL Connection
        val arraysToSend: MutableList<String> = arrayListOf()
        try {
            //http://korul.esy.es/ServerSignal.php?action=GetClients&roomName=RoomName
            val url = URL("http://korul.esy.es/ServerSignal.php?action=GetClients&roomName=$RoomName")
            val conn = url.openConnection() as HttpURLConnection
            // 2. Open InputStream to connection
            conn.connect()
            val `in` = conn.inputStream
            // 3. Download and decode the string response using builder
            val stringBuilder = StringBuilder()
            val reader = BufferedReader(InputStreamReader(`in`))

            var line: String?
            do {
                line = reader.readLine()
                if (line == null)
                    break

                stringBuilder.append(line)
            } while (true)

            val jsonArr = JSONArray(stringBuilder.toString())
            for (i in 0 until jsonArr.length()) {
                val jsonObj = jsonArr.getJSONObject(i)

                val tosend = jsonObj.getString("MyIpPort")
                if (jsonObj.getString("Login") != myUid) {
                    println(tosend)
                    arraysToSend.add(tosend)
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return arraysToSend
    }


    fun requestDeleteClient(args: Array<String>) {
        if (args.isEmpty())
            return

        try {
            // 1. Declare a URL Connection
            //http://korul.esy.es/ServerSignal.php?action=DeleteClient&Login=author
            val url = URL("http://korul.esy.es/ServerSignal.php?action=DeleteClient&login=${args[0]}")
            val conn = url.openConnection() as HttpURLConnection
            // 2. Open InputStream to connection
            conn.connect()
            val `in` = conn.inputStream
            // 3. Download and decode the string response using builder
            val stringBuilder = StringBuilder()
            val reader = BufferedReader(InputStreamReader(`in`))
            reader.readLine()
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun requestInsertClient(args: Array<String>) {
        if (args.size < 3)
            return

        try {
            // 1. Declare a URL Connection
            //http://korul.esy.es/ServerSignal.php?action=InsertClient&login=author&roomName=RoomName&myIpPort=ip;port1
            val url =
                URL("http://korul.esy.es/ServerSignal.php?action=InsertClient&login=${args[0]}&roomName=${args[1]}&myIpPort=${args[2]}")
            val conn = url.openConnection() as HttpURLConnection
            // 2. Open InputStream to connection
            conn.connect()
            val `in` = conn.inputStream
            // 3. Download and decode the string response using builder
            val stringBuilder = StringBuilder()
            val reader = BufferedReader(InputStreamReader(`in`))
            reader.readLine()
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun initConnect() {
        startALL.setOnClickListener {
            startALL.isEnabled = false

            Observable.just(Optional(null))
                .subscribeOn(Schedulers.newThread())
                .doOnNext {
                    if (use1) {
                        Log.d(TAG, "connectToStun uniqueID1")
                        connectToStun(uniqueID1, 0)
                    }
                }
                .delay(10 , TimeUnit.MILLISECONDS)
                .doOnNext {
                    if (use1) {
                        Log.d(TAG, "requestInsertClient uniqueID1")
                        val arr: Array<String> = arrayOf(uniqueID1, room1.text.toString(), "${ipserver.text}:${myPortForOther1}")
                        requestInsertClient(arr)
                    }
                }
                .delay(10 , TimeUnit.MILLISECONDS)
                .doOnNext {
                    if (use1) {
                        Log.d(TAG, "requestGetClient uniqueID1")
                        val clients = requestGetClient(room1.text.toString(), uniqueID1)

                        Log.d(TAG, "retranslateAllToMainSocket 1")
                        retranslateAllToMainSocket("${ipserver.text}", myRecievePort1, "127.0.0.1", myRecievePort1)

                        var strTosend = ""
                        if(clients.isNotEmpty()) {
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
                                "127.0.0.1:$myRecievePort1",
                                "${ipserver.text}:$myPortForOther1",
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
                        connectToStun(uniqueID2, 1)
                    }
                }
                .delay(10 , TimeUnit.MILLISECONDS)
                .doOnNext {
                    if (use2) {
                        Log.d(TAG, "requestInsertClient uniqueID2")
                        val arr: Array<String> = arrayOf(uniqueID2, room2.text.toString(), "${ipserver.text}:${myPortForOther2}")
                        requestInsertClient(arr)
                    }
                }
                .delay(10 , TimeUnit.MILLISECONDS)
                .doOnNext {
                    if (use2) {
                        Log.d(TAG, "requestGetClient uniqueID2")
                        val clients = requestGetClient(room2.text.toString(), uniqueID2)

                        Log.d(TAG, "retranslateAllToMainSocket 2")
                        retranslateAllToMainSocket("${ipserver.text}", myRecievePort2, "127.0.0.1", myRecievePort2)

                        var strTosend = ""
                        if(clients.isNotEmpty()) {
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
                                "127.0.0.1:$myRecievePort2",
                                "${ipserver.text}:$myPortForOther2",
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
                        connectToStun(uniqueID3, 2)
                    }
                }
                .delay(10 , TimeUnit.MILLISECONDS)
                .doOnNext {
                    if (use3) {
                        Log.d(TAG, "requestInsertClient uniqueID3")
                        val arr: Array<String> = arrayOf(uniqueID3, room3.text.toString(), "${ipserver.text}:${myPortForOther3}")
                        requestInsertClient(arr)
                    }
                }
                .delay(100 , TimeUnit.MILLISECONDS)
                .doOnNext {
                    if (use3) {
                        Log.d(TAG, "requestGetClient uniqueID3")
                        val clients = requestGetClient(room3.text.toString(), uniqueID3)

                        Log.d(TAG, "retranslateAllToMainSocket 3")
                        retranslateAllToMainSocket("${ipserver.text}", myRecievePort3, "127.0.0.1", myRecievePort3)

                        var strTosend = ""
                        if(clients.isNotEmpty()) {
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
                                "127.0.0.1:$myRecievePort3",
                                "${ipserver.text}:$myPortForOther3",
                                strTosend
                            )
                        }
                        thread3.start()
                    }
                }
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    startALL.isEnabled = false
                }, { it ->
                    startALL.isEnabled = true

                    val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    } else {
                        AlertDialog.Builder(this)
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
    }


    @SuppressLint("CheckResult")
    fun resetAllConnectionsByUids() {
        startALL.isEnabled = false
        Observable.just(Optional(null))
            .subscribeOn(Schedulers.newThread())
            .doOnNext {
                if (use1) {
                    val arr: Array<String> = arrayOf(uniqueID1)
                    requestDeleteClient(arr)
                    resetConnectOnServer(uniqueID1)
                }
                if (use2) {
                    val arr: Array<String> = arrayOf(uniqueID2)
                    requestDeleteClient(arr)
                    resetConnectOnServer(uniqueID2)
                }
                if (use3) {
                    val arr: Array<String> = arrayOf(uniqueID3)
                    requestDeleteClient(arr)
                    resetConnectOnServer(uniqueID3)
                }
            }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({

                startALL.isEnabled = true

            }, { it ->
                val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                } else {
                    AlertDialog.Builder(this)
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


    fun sendButtonInitClickListener() {
        send1.setOnClickListener {
            if (!CommentEditText1.text.isEmpty()) {
                text1 = CommentEditText1.text.toString()
                CommentEditText1.text.clearSpans()
                CommentEditText1.text = SpannableStringBuilder("")
                session?.send_message(0, text1)
            }
        }


        send2.setOnClickListener {
            if (!CommentEditText2.text.isEmpty()) {
                text2 = CommentEditText2.text.toString()
                CommentEditText2.text.clearSpans()
                CommentEditText2.text = SpannableStringBuilder("")
                session?.send_message(1, text2)
            }
        }


        send3.setOnClickListener {
            if (!CommentEditText3.text.isEmpty()) {
                text3 = CommentEditText3.text.toString()
                CommentEditText3.text.clearSpans()
                CommentEditText3.text = SpannableStringBuilder("")
                session?.send_message(2, text3)
            }
        }
    }

    fun subscribeSession() {
        session?.subscribe { you: Boolean, uid: String, mes: String ->

            if(uid == "test" && mes == "test") {
                Log.d(TAG, "subscribeSession - init")
                showError = false
                return@subscribe
            }

            updateFloatBut1()

            if (!uid.isEmpty() && !mes.isEmpty() && mes != "[None]") {
                val text1 = Text1

                if (you) {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append("you: ")
                    var mess = mes.removeRange(0, 19)
                    mess = mess.removeRange(mess.count() - 5, mess.count())
                    str.append(mess)

                    setTextAndScrollDown1(str.toString())
                } else {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append(uid)
                    str.append(": ")
                    var mess = mes.removeRange(0, 19)
                    mess = mess.removeRange(mess.count() - 5, mess.count())
                    str.append(mess)

                    setTextAndScrollDown1(str.toString())
                }
            }
        }

        session?.subscribe { you: Boolean, uid: String, mes: String ->

            updateFloatBut2()

            if (!uid.isEmpty() && !mes.isEmpty() && mes != "[None]") {
                val text1 = Text2
                if (you) {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append("you: ")
                    var mess = mes.removeRange(0, 19)
                    mess = mess.removeRange(mess.count() - 5, mess.count())
                    str.append(mess)

                    setTextAndScrollDown2(str.toString())
                } else {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append(uid)
                    str.append(": ")
                    var mess = mes.removeRange(0, 19)
                    mess = mess.removeRange(mess.count() - 5, mess.count())
                    str.append(mess)

                    setTextAndScrollDown2(str.toString())
                }
            }
        }

        session?.subscribe { you: Boolean, uid: String, mes: String ->

            updateFloatBut3()

            if (!uid.isEmpty() && !mes.isEmpty() && mes != "[None]") {
                val text1 = Text3
                if (you) {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append("you: ")
                    var mess = mes.removeRange(0, 19)
                    mess = mess.removeRange(mess.count() - 5, mess.count())
                    str.append(mess)

                    setTextAndScrollDown3(str.toString())
                } else {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append(uid)
                    str.append(": ")
                    var mess = mes.removeRange(0, 19)
                    mess = mess.removeRange(mess.count() - 5, mess.count())
                    str.append(mess)

                    setTextAndScrollDown3(str.toString())
                }
            }
        }
    }

    fun generateOrGetUID() {
        val mSettings = this.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
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

    fun resetConnectOnServer(uniqueID: String) {
        try {
//            62.176.10.54
            val soc = Socket("${ipserver.text}", 2999)
            val dout = DataOutputStream(soc.getOutputStream())
            val din = DataInputStream(soc.getInputStream())

            val magick = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xCA.toByte(), 0xFE.toByte())
            val bytesLengthString = ByteBuffer.allocate(4).putInt(uniqueID.count()).array()
            val original = uniqueID
            val utf8Bytes = original.toByteArray(charset("UTF8"))

            dout.write(magick)
            dout.write(bytesLengthString, 0, 4)
            dout.write(utf8Bytes, 0, utf8Bytes.count())
            dout.flush()

            dout.close()
            din.close()
            soc.close()
        } catch (e: Exception) {
            e.printStackTrace()

            throw e
        }
    }

    fun connectToStun(uniqueID: String, numNode: Int) {
        try {
            val soc = Socket("${ipserver.text}", 3000)
            val dout = DataOutputStream(soc.getOutputStream())
            val din = DataInputStream(soc.getInputStream())

            val magick = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xCA.toByte(), 0xFE.toByte())
            val bytesLengthString = ByteBuffer.allocate(4).putInt(uniqueID.count()).array()
            val original = uniqueID
            val utf8Bytes = original.toByteArray(charset("UTF8"))

            dout.write(magick)
            dout.write(bytesLengthString, 0, 4)
            dout.write(utf8Bytes, 0, utf8Bytes.count())
            dout.flush()

            var bytesnum = din.available()
            while (bytesnum < 10) {
                Thread.sleep(10)
                bytesnum = din.available()
            }

            when (numNode) {
                0 -> {
                    myRecievePort1 = din.readUnsignedShort()
                    myPortForOther1 = din.readUnsignedShort()
                    myPort1 = din.readUnsignedShort()
                }
                1 -> {
                    myRecievePort2 = din.readUnsignedShort()
                    myPortForOther2 = din.readUnsignedShort()
                    myPort2 = din.readUnsignedShort()
                }
                2 -> {
                    myRecievePort3 = din.readUnsignedShort()
                    myPortForOther3 = din.readUnsignedShort()
                    myPort3 = din.readUnsignedShort()
                }
            }

            val sizeString = din.readInt()

            bytesnum = din.available()
            while (bytesnum < sizeString) {
                Thread.sleep(10)
                bytesnum = din.available()
            }
            val ip = din.readBytes()

            when (numNode) {
                0 -> myIP1 = String(ip, charset("UTF8"))
                1 -> myIP2 = String(ip, charset("UTF8"))
                2 -> myIP3 = String(ip, charset("UTF8"))
            }

            dout.close()
            din.close()
            soc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mClose = true
    }

    @SuppressLint("CheckResult")
    private fun setTextAndScrollDown1(str: String) {
        Log.d("HYDRABADGERTAG", "!!setTextAndScrollDown1")
        Observable.just(Optional(null))
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { _ ->
                val text = Text1
                text?.text = str
                scrollView1?.fullScroll(ScrollView.FOCUS_DOWN)
            }
    }

    @SuppressLint("CheckResult")
    private fun setTextAndScrollDown2(str: String) {
        Log.d("HYDRABADGERTAG", "!!setTextAndScrollDown2")
        Observable.just(Optional(null))
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { _ ->
                val text = Text2
                text?.text = str
                scrollView2?.fullScroll(ScrollView.FOCUS_DOWN)
            }
    }

    @SuppressLint("CheckResult")
    private fun setTextAndScrollDown3(str: String) {
        Log.d("HYDRABADGERTAG", "!!setTextAndScrollDown3")
        Observable.just(Optional(null))
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { _ ->
                val text = Text3
                text?.text = str
                scrollView3?.fullScroll(ScrollView.FOCUS_DOWN)
            }
    }


    @SuppressLint("CheckResult")
    private fun updateFloatBut1() {
        val fab = floatingActionButton1
        if (fab?.backgroundTintList != ColorStateList.valueOf(0xFF4CAF50.toInt())) {
            Log.d("HYDRABADGERTAG", "!!updateFloatBut1")
            Observable.just(Optional(null))
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    val fab = floatingActionButton1
                    fab?.backgroundTintList = ColorStateList.valueOf(0xFF4CAF50.toInt())
                }
        }
    }

    @SuppressLint("CheckResult")
    private fun updateFloatBut2() {
        val fab = floatingActionButton2
        if (fab?.backgroundTintList != ColorStateList.valueOf(0xFF4CAF50.toInt())) {
            Log.d("HYDRABADGERTAG", "!!updateFloatBut2")
            Observable.just(Optional(null))
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    val fab = floatingActionButton2
                    fab?.backgroundTintList = ColorStateList.valueOf(0xFF4CAF50.toInt())
                }
        }
    }

    @SuppressLint("CheckResult")
    private fun updateFloatBut3() {
        val fab = floatingActionButton3
        if (fab?.backgroundTintList != ColorStateList.valueOf(0xFF4CAF50.toInt())) {
            Log.d("HYDRABADGERTAG", "!!updateFloatBut3")
            Observable.just(Optional(null))
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    val fab = floatingActionButton3
                    fab?.backgroundTintList = ColorStateList.valueOf(0xFF4CAF50.toInt())
                }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

}
