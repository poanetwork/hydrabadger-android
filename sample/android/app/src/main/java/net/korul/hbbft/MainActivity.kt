package net.korul.hbbft

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import net.korul.hbbft.CommonFragments.ContactsFragment
import net.korul.hbbft.CommonFragments.SettingsFragment
import net.korul.hbbft.common.data.fixtures.DialogsFixtures
import net.korul.hbbft.features.DefaultDialogsFragment


class MainActivity : AppCompatActivity() {

    private var TAG = "MainActivity"

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_chats -> {

                supportFragmentManager.popBackStack(getString(R.string.tag_chats2), POP_BACK_STACK_INCLUSIVE)
                supportFragmentManager.popBackStack(getString(R.string.tag_chats), POP_BACK_STACK_INCLUSIVE)

                val transaction = supportFragmentManager.beginTransaction()
                transaction.replace(R.id.view, DefaultDialogsFragment.newInstance(), getString(R.string.tag_chats))
                transaction.addToBackStack(getString(R.string.tag_chats))
                transaction.commit()

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_contacts -> {

                supportFragmentManager.popBackStack(getString(R.string.tag_contacts), POP_BACK_STACK_INCLUSIVE)

                val transaction = supportFragmentManager.beginTransaction()
                transaction.replace(R.id.view, ContactsFragment.newInstance(), getString(R.string.tag_contacts))
                transaction.addToBackStack(getString(R.string.tag_contacts))
                transaction.commit()

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_lents -> {

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {

                supportFragmentManager.popBackStack(getString(R.string.tag_settings), POP_BACK_STACK_INCLUSIVE)

                val transaction = supportFragmentManager.beginTransaction()
                transaction.replace(R.id.view, SettingsFragment.newInstance(), getString(R.string.tag_settings))
                transaction.addToBackStack(getString(R.string.tag_settings))
                transaction.commit()

                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        (this as AppCompatActivity).supportActionBar?.show()

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        navView.selectedItemId = R.id.navigation_chats

        if (this.intent.getBooleanExtra("Start_App", false)) {
            Log.d(TAG, "Receive push and start activity")
            val roomName = intent.getStringExtra("RoomName")

            val dialogs = DialogsFixtures.dialogs
            for (diag in dialogs) {
                if (diag.dialogName == roomName) {
                    Log.d(TAG, "Found dialog and start it $roomName")

                    supportFragmentManager.popBackStack(getString(R.string.tag_chats2), POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.popBackStack(getString(R.string.tag_chats), POP_BACK_STACK_INCLUSIVE)

                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.view, DefaultDialogsFragment.newInstance(true, roomName))
                    transaction.addToBackStack(getString(R.string.tag_chats))
                    transaction.commit()
                }
            }
        }
    }


    override fun onBackPressed() {
        if(supportFragmentManager.fragments.count() > 1)
            super.onBackPressed()
    }
}
