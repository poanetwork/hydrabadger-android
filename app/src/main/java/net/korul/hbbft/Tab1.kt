package net.korul.hbbft


import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.tab1_content.*

class Tab1 : Fragment() {

    lateinit var thread1: Thread
    lateinit var thread2: Thread
    lateinit var thread3: Thread

    lateinit var text1: String
    lateinit var text2: String
    lateinit var text3: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.tab1_content, container, false)
    }



    private fun startTimerThread1(str: String) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            handler.post {
                val text = view?.findViewById<TextView>(R.id.Text1)
                text?.text = str
            }
        }
        Thread(runnable).start()
    }

    private fun startTimerThread2(str: String) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            handler.post {
                val text = view?.findViewById<TextView>(R.id.Text2)
                text?.text = str
            }
        }
        Thread(runnable).start()
    }

    private fun startTimerThread3(str: String) {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            handler.post {
                val text = view?.findViewById<TextView>(R.id.Text3)
                text?.text = str
            }
        }
        Thread(runnable).start()
    }


    private fun updateFloatBut1() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            handler.post {
                val fab = view?.findViewById<FloatingActionButton>(R.id.floatingActionButton1)
                fab?.backgroundTintList = ColorStateList.valueOf(0xFF4CAF50.toInt())
            }
        }
        Thread(runnable).start()
    }

    private fun updateFloatBut2() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            handler.post {
                val fab = view?.findViewById<FloatingActionButton>(R.id.floatingActionButton2)
                fab?.backgroundTintList = ColorStateList.valueOf(0xFF4CAF50.toInt())
            }
        }
        Thread(runnable).start()
    }

    private fun updateFloatBut3() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            handler.post {
                val fab = view?.findViewById<FloatingActionButton>(R.id.floatingActionButton3)
                fab?.backgroundTintList = ColorStateList.valueOf(0xFF4CAF50.toInt())
            }
        }
        Thread(runnable).start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hbbft.get().session?.subscribe { you: Boolean, uid: String, mes: String ->

            updateFloatBut1()

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

        hbbft.get().session?.subscribe { you: Boolean, uid: String, mes: String ->

            updateFloatBut2()

            val text1 = view.findViewById<TextView>(R.id.Text2)
            if (you) {
                val str = SpannableStringBuilder()
                str.append(text1.text)
                str.append("\n")
                str.append("you: ")
                var mess = mes.removeRange(0, 15)
                mess = mess.removeRange(mess.count()-5, mess.count())
                str.append(mess)

                startTimerThread2(str.toString())
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

                startTimerThread2(str.toString())
            }
        }

        hbbft.get().session?.subscribe { you: Boolean, uid: String, mes: String ->

            updateFloatBut3()

            val text1 = view.findViewById<TextView>(R.id.Text3)
            if (you) {
                val str = SpannableStringBuilder()
                str.append(text1.text)
                str.append("\n")
                str.append("you: ")
                var mess = mes.removeRange(0, 15)
                mess = mess.removeRange(mess.count()-5, mess.count())
                str.append(mess)

                startTimerThread3(str.toString())
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

                startTimerThread3(str.toString())
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


        button1.setOnClickListener {
            thread1 = Thread {
                hbbft.get().session?.start_node(ip1.text.toString(), iplist1.text.toString())
            }
            thread1.start()
            button1.isEnabled = false
        }

        button2.setOnClickListener {
            thread2 = Thread {
                hbbft.get().session?.start_node(ip2.text.toString(), iplist2.text.toString())
            }
            thread2.start()
            button2.isEnabled = false
        }

        button3.setOnClickListener {
            thread3 = Thread {
                hbbft.get().session?.start_node(ip3.text.toString(), iplist3.text.toString())
            }
            thread3.start()
            button3.isEnabled = false
        }
    }
}
