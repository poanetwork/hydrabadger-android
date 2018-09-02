package net.korul.hbbft

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import kotlinx.android.synthetic.main.content_main.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {

    lateinit var thread1: Thread
    lateinit var thread2: Thread
    lateinit var thread3: Thread

    lateinit var logCatTask : LogCatTask

    private val INITIAL_REQUEST  = 1337


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val fab = findViewById<View>(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        testCallbacks();

        button1.setOnClickListener {
            thread1 = Thread {
                hbbft.get().session?.start_node1()
            }
            thread1.start()
            button1.isEnabled = false
        }

        button2.setOnClickListener {
            thread2 = Thread {
                hbbft.get().session?.start_node2()
            }
            thread2.start()
            button2.isEnabled = false
        }

        button3.setOnClickListener {
            thread3 = Thread {
                hbbft.get().session?.start_node3()
            }
            thread3.start()
            button3.isEnabled = false
        }


        val permissions = ArrayList<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_LOGS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_LOGS)

            if (Build.VERSION.SDK_INT >= 23)
                requestPermissions(permissions.toTypedArray(), INITIAL_REQUEST)
            else
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), INITIAL_REQUEST)
        }
        else {
            setupTextView()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == INITIAL_REQUEST) {
            setupTextView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        return if (id == R.id.action_settings) {
            true
        } else
            super.onOptionsItemSelected(item)

    }


    private fun testCallbacks() {
        val states = arrayOf(intArrayOf(android.R.attr.state_enabled), // enabled
                intArrayOf(-android.R.attr.state_enabled), // disabled
                intArrayOf(-android.R.attr.state_checked), // unchecked
                intArrayOf(android.R.attr.state_pressed)  // pressed
        )

        val colors = intArrayOf(Color.BLACK, Color.RED, Color.GREEN, Color.BLUE)

        val myList = ColorStateList(states, colors)

        hbbft.get().session?.subscribe1 { butPr.backgroundTintList = myList }
        hbbft.get().session?.subscribe2 { butPr1.backgroundTintList = myList }
        hbbft.get().session?.subscribe3 { butPr2.backgroundTintList = myList }

        hbbft.get().session?.after_subscribe()
    }

    @SuppressLint("StaticFieldLeak")
    open inner class LogCatTask : AsyncTask<Void, String, Void>() {
        var run = AtomicBoolean(true)

        override fun doInBackground(vararg params: Void): Void? {
            try {
                Runtime.getRuntime().exec("logcat -c")
                val process = Runtime.getRuntime().exec("logcat")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                val log = StringBuilder()
                var line: String? = ""
                while (run.get()) {
                    line = bufferedReader.readLine()

                    if (line != null) {
                        if (line.contains("W hydrabadger")
                                || line.contains("W hydrabad")
                                || line.contains("W hydrabadger::hydrabad..:")
                                || line.contains("W HYDRABADGERTAG")) {
                            log.append(line)
                            log.append("\n")
                            publishProgress(log.toString())
                        }
                    }

                    line = null
                    Thread.sleep(10)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            return null
        }
    }

    fun setupTextView() {
        textView.movementMethod = ScrollingMovementMethod()
        logCatTask = @SuppressLint("StaticFieldLeak")
        object : LogCatTask() {
            override fun onProgressUpdate(vararg values: String) {
                textView.text = values[0]
                val scrollview = findViewById<View>(R.id.scrollView) as ScrollView
                scrollview.post { scrollview.fullScroll(ScrollView.FOCUS_DOWN) }
                super.onProgressUpdate(*values)
            }
        }
        logCatTask.execute()
    }
}
