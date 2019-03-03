package net.korul.hbbft

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import net.korul.hbbft.CommonFragments.tabContacts.ContactsFragment
import net.korul.hbbft.CommonFragments.tabLenta.ListNewsFragment
import net.korul.hbbft.CommonFragments.tabSettings.DialogThemeFragment
import net.korul.hbbft.CommonFragments.tabSettings.SettingsFragment
import net.korul.hbbft.common.data.fixtures.DialogsFixtures
import net.korul.hbbft.features.DefaultDialogsFragment
import net.korul.hbbft.features.DefaultMessagesFragment

//import com.judemanutd.autostarter.AutoStartPermissionHelper


class MainActivity : AppCompatActivity() {

    private var TAG = "HYDRABADGERTAG:MainActivity"

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

                supportFragmentManager.popBackStack(getString(R.string.tag_lenta), POP_BACK_STACK_INCLUSIVE)

                val transaction = supportFragmentManager.beginTransaction()
                transaction.replace(R.id.view, ListNewsFragment.newInstance(), getString(R.string.tag_lenta))
                transaction.addToBackStack(getString(R.string.tag_lenta))
                transaction.commit()

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
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        (this as AppCompatActivity).supportActionBar?.show()

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        navView.selectedItemId = R.id.navigation_chats

        val prefs = this.getSharedPreferences("HYRABADGER", Application.MODE_PRIVATE)
        val theme = prefs!!.getString("Theme", "night")
        if (theme == "night") {
            this.setTheme(R.style.App_Black_Theme)
            this.theme.applyStyle(R.style.App_Black_Theme, true)
            navView.setBackgroundDrawable(this.resources.getDrawable(R.drawable.footer_bar_dark))
        } else if (theme == "light") {
            this.setTheme(R.style.App_Healin_Theme)
            this.theme.applyStyle(R.style.App_Healin_Theme, true)
            navView.setBackgroundDrawable(this.resources.getDrawable(R.drawable.footer_bar_healin))
        }

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
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent()
                val packageName = packageName
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:net.korul.hbbft")
                    startActivity(intent)

//                    AutoStartPermissionHelper.getInstance().getAutoStartPermission(this)
                }
            }
        }

        val themeRestart = prefs.getBoolean("Theme_NeedRestart", false)
        if (themeRestart) {
            val editor = prefs.edit()
            editor.putBoolean("Theme_NeedRestart", false)
            editor.apply()

            navView.selectedItemId = R.id.navigation_settings

            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.view, DialogThemeFragment.newInstance(), getString(R.string.tag_settings))
            transaction.addToBackStack(getString(R.string.tag_settings))
            transaction.commit()
        }
    }


    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        if (fragments.size > 0) {
            val lastFragment = fragments[fragments.size - 1]
            if (lastFragment != null && lastFragment.isVisible) {
                if (lastFragment is DefaultMessagesFragment) {
                    if (lastFragment.onBackPressed()) {
                        supportFragmentManager.popBackStack(getString(R.string.tag_chats2), POP_BACK_STACK_INCLUSIVE)
                        supportFragmentManager.popBackStack(getString(R.string.tag_chats), POP_BACK_STACK_INCLUSIVE)

                        val transaction = supportFragmentManager.beginTransaction()
                        transaction.replace(R.id.view, DefaultDialogsFragment.newInstance())
                        transaction.addToBackStack(getString(R.string.tag_chats))
                        transaction.commit()
                    }
                }
            }
        }
        if (supportFragmentManager.fragments.count() > 1)
            super.onBackPressed()
    }
}
