package net.korul.hbbft

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import net.korul.hbbft.common.data.fixtures.DialogsFixtures


class MainActivity : AppCompatActivity() {

    private var TAG = "MainActivity"

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_chats -> {

                val transaction = supportFragmentManager.beginTransaction()
                transaction.replace(R.id.view, DefaultDialogsFragment.newInstance())
                transaction.addToBackStack(getString(R.string.tag_chats))
                transaction.commit()

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_contacts -> {

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_lents -> {

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {

                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        if (this.intent.getBooleanExtra("Start_App", false)) {
            Log.d(TAG, "Receive push and start activity")
            val roomName = intent.getStringExtra("RoomName")

            val dialogs = DialogsFixtures.dialogs
            for (diag in dialogs) {
                if (diag.dialogName == roomName) {
                    Log.d(TAG, "Found dialog and start it $roomName")

                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.view, DefaultDialogsFragment.newInstance(true, roomName))
                    transaction.addToBackStack(getString(R.string.tag_chats))
                    transaction.commit()
                }
            }
        }
    }
}
