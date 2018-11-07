package net.korul.hbbft


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.tab1_content.*
import ru.hintsolutions.diabets.services.ClosingService
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.locks.ReentrantLock


data class Optional<M>(val value : M?)

class Tab1 : Fragment() {
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


    private var mClose: Boolean = false

    private var myIP = ""
    private var myPort: Int = 0
    private var myRecievePort: Int = 0
    private var myPortForOther: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.tab1_content, container, false)
    }


    override fun onDestroy() {
        super.onDestroy()
        mClose = true
    }


    @SuppressLint("CheckResult")
    private fun startTimerThread1(str: String) {
        Observable.just(Optional(null))
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    val text = view?.findViewById<TextView>(R.id.Text1)
                    text?.text = str
                }
    }

    @SuppressLint("CheckResult")
    private fun startTimerThread2(str: String) {
        Observable.just(Optional(null))
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    val text = view?.findViewById<TextView>(R.id.Text2)
                    text?.text = str
                }
    }

    @SuppressLint("CheckResult")
    private fun startTimerThread3(str: String) {
        Observable.just(Optional(null))
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { _ ->
                    val text = view?.findViewById<TextView>(R.id.Text3)
                    text?.text = str
                }
    }


    @SuppressLint("CheckResult")
    private fun updateFloatBut1() {
        val fab = view?.findViewById<FloatingActionButton>(R.id.floatingActionButton1)
        if(fab?.rippleColorStateList != ColorStateList.valueOf(0xFF4CAF50.toInt())) {
            Observable.just(Optional(null))
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { _ ->
                        val fab = view?.findViewById<FloatingActionButton>(R.id.floatingActionButton1)
                        fab?.backgroundTintList = ColorStateList.valueOf(0xFF4CAF50.toInt())
                    }
        }
    }

    @SuppressLint("CheckResult")
    private fun updateFloatBut2() {
        val fab = view?.findViewById<FloatingActionButton>(R.id.floatingActionButton2)
        if(fab?.rippleColorStateList != ColorStateList.valueOf(0xFF4CAF50.toInt())) {
            Observable.just(Optional(null))
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { _ ->
                        val fab = view?.findViewById<FloatingActionButton>(R.id.floatingActionButton2)
                        fab?.backgroundTintList = ColorStateList.valueOf(0xFF4CAF50.toInt())
                    }
        }
    }

    @SuppressLint("CheckResult")
    private fun updateFloatBut3() {
        val fab = view?.findViewById<FloatingActionButton>(R.id.floatingActionButton3)
        if(fab?.rippleColorStateList != ColorStateList.valueOf(0xFF4CAF50.toInt())) {
            Observable.just(Optional(null))
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { _ ->
                        val fab = view?.findViewById<FloatingActionButton>(R.id.floatingActionButton3)
                        fab?.backgroundTintList = ColorStateList.valueOf(0xFF4CAF50.toInt())
                    }
        }
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mSettings = context!!.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        var uiid = mSettings.getString(APP_PREFERENCES_NAME1, "")

        if(uiid == null || uiid == ""){
            uniqueID1 = UUID.randomUUID().toString()

            val editor = mSettings.edit()
            editor.putString(APP_PREFERENCES_NAME1, uniqueID1)
            editor.apply()
        }
        else
            uniqueID1 = uiid

        uiid = mSettings.getString(APP_PREFERENCES_NAME2, "")

        if(uiid == null || uiid == ""){
            uniqueID2 = UUID.randomUUID().toString()

            val editor = mSettings.edit()
            editor.putString(APP_PREFERENCES_NAME2, uniqueID2)
            editor.apply()
        }
        else
            uniqueID2 = uiid

        uiid = mSettings.getString(APP_PREFERENCES_NAME3, "")

        if(uiid == null || uiid == ""){
            uniqueID3 = UUID.randomUUID().toString()

            val editor = mSettings.edit()
            editor.putString(APP_PREFERENCES_NAME3, uniqueID3)
            editor.apply()
        }
        else
            uniqueID3 = uiid

        hbbft.get().session?.subscribe { you: Boolean, uid: String, mes: String ->

            updateFloatBut1()

            if(!uid.isEmpty() && !mes.isEmpty()) {
                val text1 = view.findViewById<TextView>(R.id.Text1)
                if (you) {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append("you: ")
                    var mess = mes.removeRange(0, 15)
                    mess = mess.removeRange(mess.count()-5, mess.count())
                    str.append(mess)

                    startTimerThread1(str.toString())
                }
                else {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append(uid)
                    str.append(": ")
                    var mess = mes.removeRange(0, 15)
                    mess = mess.removeRange(mess.count()-5, mess.count())
                    str.append(mess)

                    startTimerThread1(str.toString())
                }
            }
        }

        hbbft.get().session?.subscribe { you: Boolean, uid: String, mes: String ->

            updateFloatBut2()

            if(!uid.isEmpty() && !mes.isEmpty()) {
                val text1 = view.findViewById<TextView>(R.id.Text2)
                if (you) {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append("you: ")
                    var mess = mes.removeRange(0, 15)
                    mess = mess.removeRange(mess.count() - 5, mess.count())
                    str.append(mess)

                    startTimerThread2(str.toString())
                } else {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append(uid)
                    str.append(": ")
                    var mess = mes.removeRange(0, 15)
                    mess = mess.removeRange(mess.count() - 5, mess.count())
                    str.append(mess)

                    startTimerThread2(str.toString())
                }
            }
        }

        hbbft.get().session?.subscribe { you: Boolean, uid: String, mes: String ->

            updateFloatBut3()

            if(!uid.isEmpty() && !mes.isEmpty()) {
                val text1 = view.findViewById<TextView>(R.id.Text3)
                if (you) {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append("you: ")
                    var mess = mes.removeRange(0, 15)
                    mess = mess.removeRange(mess.count() - 5, mess.count())
                    str.append(mess)

                    startTimerThread3(str.toString())
                } else {
                    val str = SpannableStringBuilder()
                    str.append(text1.text)
                    str.append("\n")
                    str.append(uid)
                    str.append(": ")
                    var mess = mes.removeRange(0, 15)
                    mess = mess.removeRange(mess.count() - 5, mess.count())
                    str.append(mess)

                    startTimerThread3(str.toString())
                }
            }
        }

        send1.setOnClickListener {
            if(!CommentEditText1.text.isEmpty()) {
                text1 = CommentEditText1.text.toString()
                CommentEditText1.text.clearSpans()
                CommentEditText1.text = SpannableStringBuilder("")
                hbbft.get().session?.send_message(0, text1)
            }
        }


        send2.setOnClickListener {
            if(!CommentEditText2.text.isEmpty()) {
                text2 = CommentEditText2.text.toString()
                CommentEditText2.text.clearSpans()
                CommentEditText2.text = SpannableStringBuilder("")
                hbbft.get().session?.send_message(1, text2)
            }
        }


        send3.setOnClickListener {
            if(!CommentEditText3.text.isEmpty()) {
                text3 = CommentEditText3.text.toString()
                CommentEditText3.text.clearSpans()
                CommentEditText3.text = SpannableStringBuilder("")
                hbbft.get().session?.send_message(2, text3)
            }
        }


        val use3 = true
        val use2 = true
        val use1 = true

//        val use3 = true
//        val use2 = false
//        val use1 = false

        Observable.just(Optional(null))
                .subscribeOn(Schedulers.newThread())
                .doOnNext {
                    if(use1) {
                        resetConnectOnServer(uniqueID1)
                    }
                    if(use2) {
                        resetConnectOnServer(uniqueID2)
                    }
                    if(use3) {
                        resetConnectOnServer(uniqueID3)
                    }
                }
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe ({ _ ->

                }, { it ->
                    val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
                    } else {
                        AlertDialog.Builder(context)
                    }
                    builder.setTitle("Error")
                            .setMessage(it.message)
                            .setPositiveButton(android.R.string.yes) { dialog, which ->
                                dialog.cancel()
                            }
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show()

                })


        if(use1) {
            button1.setOnClickListener {
                Observable.just(Optional(null))
                        .subscribeOn(Schedulers.newThread())
                        .doOnNext {
                            if(use1) {
                                Log.d(TAG, "connectToStun uniqueID1")
                                connectToStun(uniqueID1)
                            }
                        }
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { _ ->
                            if(use1) {
                                Log.d(TAG, "retranslateAllToMainSocket 1")
                                retranslateAllToMainSocket("62.176.10.54", 50001, "127.0.0.1", 50010, 0)

                                thread1 = Thread {
                                    hbbft.get().session?.start_node(ip1.text.toString(), "62.176.10.54:50002", iplist1.text.toString())
                                }
                                thread1.start()
                            }
                        }
                button1.isEnabled = false
            }
        }
        else {
            button1.isEnabled = false
        }

        if(use2) {
            button2.setOnClickListener {
                Observable.just(Optional(null))
                        .subscribeOn(Schedulers.newThread())
                        .doOnNext {
                            if(use2) {
                                Log.d(TAG, "connectToStun uniqueID2")
                                connectToStun(uniqueID2)
                            }
                        }
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { _ ->
                            if(use2) {
                                Log.d(TAG, "retranslateAllToMainSocket 2")
                                retranslateAllToMainSocket("62.176.10.54", 50003, "127.0.0.1", 50011, 0)

                                thread2 = Thread {
                                    hbbft.get().session?.start_node(ip2.text.toString(), "62.176.10.54:50004", iplist2.text.toString())
                                }
                                thread2.start()
                            }
                        }

                button2.isEnabled = false
            }
        }
        else {
            button2.isEnabled = false
        }

        if(use3) {
            button3.setOnClickListener {
                Observable.just(Optional(null))
                        .subscribeOn(Schedulers.newThread())
                        .doOnNext {
                            if(use3) {
                                Log.d(TAG, "connectToStun uniqueID3")
                                connectToStun(uniqueID3)
                            }
                        }
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { _ ->
                            if(use3) {
                                Log.d(TAG, "retranslateAllToMainSocket 3")
                                retranslateAllToMainSocket("62.176.10.54", 50005, "127.0.0.1", 50012, 0)

                                thread3 = Thread {
                                    hbbft.get().session?.start_node(ip3.text.toString(), "62.176.10.54:50006", iplist3.text.toString())
                                }
                                thread3.start()
                            }
                        }
                button3.isEnabled = false
            }
        }
        else {
            button3.isEnabled = false
        }

        val serviceIntent = Intent(context, ClosingService::class.java)
        serviceIntent.putExtra("uniqueID1", uniqueID1)
        serviceIntent.putExtra("uniqueID2", uniqueID2)
        serviceIntent.putExtra("uniqueID3", uniqueID3)

        activity?.startService(serviceIntent)
    }


    fun writeAndConfigLocal(bytes: ByteArray, size: Int, socketDescription: Int, HashMapSocket: HashMap<Int, Socket?>, HashMapThread: HashMap<Int, Thread >,
                            addrTo: String, portTo: Int, socOut: Socket) {
        if(!HashMapSocket.contains(socketDescription)) {
            while (true) {
                if (mClose)
                    break
                try {
                    HashMapSocket[socketDescription] = Socket(addrTo, portTo)
                    break
                } catch (e: Exception) {
                }
            }


            HashMapThread[socketDescription] = Thread {
                while (true) {
                    if(mClose)
                        break

                    val dout = DataOutputStream(socOut.getOutputStream())
                    val din  = DataInputStream(HashMapSocket[socketDescription]?.getInputStream())

                    val size = din.available()
                    if(size > 0) {
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

    fun retranslateAllToMainSocket(addrFrom: String, portFrom: Int, addrTo: String, portTo: Int, sleep: Long) {
        val thread = Thread {
            Thread.sleep(sleep)
            try {
                val soc = Socket(addrFrom, portFrom)
                val din  = DataInputStream(soc.getInputStream())

                val HashMapSocket: HashMap<Int, Socket?> = hashMapOf()
                val HashMapThread: HashMap<Int, Thread > = hashMapOf()

                while (true) {
                    if (mClose)
                        break

                    val bytesAvailable = din.available()
                    if(bytesAvailable > 0) {
                        var bytesnum = din.available()
                        while (bytesnum < 8) {
                            Thread.sleep(10)
                            bytesnum = din.available()
                        }

                        val size  = din.readInt()
                        val socketDescription = din.readInt()

                        bytesnum = din.available()
                        while (bytesnum < size && !mClose) {
                            Thread.sleep(10)
                            bytesnum = din.available()
                        }

                        val bytes = ByteArray(size)
                        val reads = din.read(bytes, 0, size)

                        writeAndConfigLocal(bytes, reads, socketDescription, HashMapSocket, HashMapThread, addrTo, portTo, soc)
                        continue
                    }
                    else {
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

    fun resetConnectOnServer(uniqueID: String) {
        try {
//            62.176.10.54
            val soc = Socket("62.176.10.54", 49999)
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
        }
        catch (e:Exception){
            e.printStackTrace()

            throw e
        }
    }

    fun connectToStun(uniqueID: String) {
        try {
            val soc = Socket("62.176.10.54", 50000)
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

            myRecievePort  = din.readUnsignedShort()
            myPortForOther = din.readUnsignedShort()
            myPort = din.readUnsignedShort()

            val sizeString = din.readInt()

            bytesnum = din.available()
            while (bytesnum < sizeString) {
                Thread.sleep(10)
                bytesnum = din.available()
            }
            val ip = din.readBytes()
            myIP = String(ip, charset("UTF8"))

            dout.close()
            din.close()
            soc.close()
        }
        catch (e:Exception){
            e.printStackTrace()
            throw e
        }
    }
}
