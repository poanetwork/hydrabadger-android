package net.korul.hbbft

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import net.korul.hbbft.CommonData.data.fixtures.DialogsFixtures
import net.korul.hbbft.CommonData.utils.AppUtils
import net.korul.hbbft.CommonFragments.tabChats.AddDialogFragment
import net.korul.hbbft.CommonFragments.tabContacts.ContactsFragment
import net.korul.hbbft.CommonFragments.tabLenta.ListNewsFragment
import net.korul.hbbft.CommonFragments.tabSettings.DialogThemeFragment
import net.korul.hbbft.CommonFragments.tabSettings.SettingsFragment
import net.korul.hbbft.CommonFragments.tabSettings.SettingsUserFragment
import net.korul.hbbft.CoreHBBFT.CoreHBBFT.uniqueID1
import net.korul.hbbft.CoreHBBFT.UserWork.getAnyLocalUserByUid
import net.korul.hbbft.CoreHBBFT.UserWork.saveCurUser
import net.korul.hbbft.CoreHBBFT.UserWork.updateAvatarInAllLocalUserByUid
import net.korul.hbbft.DatabaseApplication.Companion.mCurUser
import net.korul.hbbft.Dialogs.DialogsFragment
import net.korul.hbbft.Dialogs.MessagesFragment
import net.korul.hbbft.FirebaseStorageDU.MyDownloadRoomService
import net.korul.hbbft.FirebaseStorageDU.MyDownloadUserService
import net.korul.hbbft.FirebaseStorageDU.MyUploadRoomService
import net.korul.hbbft.FirebaseStorageDU.MyUploadUserService
import java.io.File

//import com.judemanutd.autostarter.AutoStartPermissionHelper

class MainActivity : AppCompatActivity() {

    private var TAG = "HYDRABADGERTAG:MainActivity"
    lateinit var broadcastReceiver: BroadcastReceiver

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_chats -> {

                supportFragmentManager.popBackStack(getString(R.string.tag_chats2), POP_BACK_STACK_INCLUSIVE)
                supportFragmentManager.popBackStack(getString(R.string.tag_chats), POP_BACK_STACK_INCLUSIVE)

                val transaction = supportFragmentManager.beginTransaction()
                transaction.replace(R.id.view, DialogsFragment.newInstance(), getString(R.string.tag_chats))
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
            val RoomId = intent.getStringExtra("RoomId")

            val dialogs = DialogsFixtures.dialogs
            for (diag in dialogs) {
                if (diag.id == RoomId) {
                    Log.d(TAG, "Found dialog and start it $RoomId")

                    supportFragmentManager.popBackStack(getString(R.string.tag_chats2), POP_BACK_STACK_INCLUSIVE)
                    supportFragmentManager.popBackStack(getString(R.string.tag_chats), POP_BACK_STACK_INCLUSIVE)

                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.view, DialogsFragment.newInstance(true, RoomId))
                    transaction.addToBackStack(getString(R.string.tag_chats))
                    transaction.commit()
                }
            }
        }
//        else {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                val intent = Intent()
//                val packageName = packageName
//                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
//                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
//                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
//                    intent.data = Uri.parse("package:net.korul.hbbft")
//                    startActivity(intent)

//                    AutoStartPermissionHelper.getInstance().getAutoStartPermission(this)
//                }
//            }
//        }

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


        initSyncSystem()
    }

    public override fun onStart() {
        super.onStart()

        // Register receiver for uploads and downloads
        val manager = LocalBroadcastManager.getInstance(this)
        manager.registerReceiver(broadcastReceiver, MyDownloadUserService.intentFilter)
        manager.registerReceiver(broadcastReceiver, MyUploadUserService.intentFilter)
    }

    public override fun onStop() {
        super.onStop()
        // Unregister download receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    private fun initSyncSystem() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "onReceive:$intent")

                when (intent.action) {
                    // User
                    MyDownloadUserService.DOWNLOAD_COMPLETED -> {
                        val filepath = intent.getStringExtra(MyDownloadUserService.EXTRA_FILE_DOWNLOADED)
                        val uid = intent.getStringExtra(MyDownloadUserService.EXTRA_UID_DOWNLOADED)

                        val avatarFile = File(filepath)

                        AppUtils.showToast(
                            context,
                            "Download Avatar complete - ${avatarFile.path}", true
                        )

                        updateAvatarInAllLocalUserByUid(uid, avatarFile)
                    }

                    MyDownloadUserService.DOWNLOAD_ERROR -> {
                        AppUtils.showToast(
                            context,
                            "Download Avatar Error", true
                        )
                    }

                    MyDownloadUserService.DOWNLOAD_FILE_NOT_FOUND -> {
                        Log.d(TAG, getString(R.string.avatar_file_not_found))
                    }

                    MyUploadUserService.UPLOAD_COMPLETED -> {
                        val filepath = intent.getStringExtra(MyUploadUserService.EXTRA_FILE_URI)
                        if (filepath != null) {
                            val file = File(filepath)
                            updateAvatarInAllLocalUserByUid(uniqueID1, file)

                            mCurUser = getAnyLocalUserByUid(uniqueID1)!!
                            saveCurUser(mCurUser)

                            val myFragment =
                                supportFragmentManager.findFragmentByTag(getString(R.string.tag_settings)) as SettingsUserFragment?
                            if (myFragment != null && myFragment.isVisible) {
                                myFragment.setImageAvatar(file)
                                myFragment.dismissProgressBar()
                            }

                            AppUtils.showToast(
                                context,
                                "UPLOAD Avatar COMPLETED", true
                            )
                        }
                    }

                    MyUploadUserService.UPLOAD_ERROR -> {
                        val myFragment =
                            supportFragmentManager.findFragmentByTag(getString(R.string.tag_settings)) as SettingsUserFragment?
                        if (myFragment != null && myFragment.isVisible) {
                            myFragment.dismissProgressBar()
                        }
                        AppUtils.showToast(
                            context,
                            "UPLOAD Avatar Error", true
                        )
                    }


                    // Room
                    MyDownloadRoomService.DOWNLOAD_COMPLETED -> {
                        val uid = intent.getStringExtra(MyDownloadRoomService.EXTRA_UID_DOWNLOADED)

                        AppUtils.showToast(
                            context,
                            "ADDED DIALOG complete - $uid", true
                        )
                    }

                    MyDownloadRoomService.DOWNLOAD_ERROR -> {
                        AppUtils.showToast(
                            context,
                            "ADDED DIALOGError", true
                        )
                    }

                    MyDownloadRoomService.DOWNLOAD_FILE_NOT_FOUND -> {
                        Log.d(TAG, getString(R.string.room_file_not_found))
                    }

                    MyUploadRoomService.UPLOAD_ROOM_COMPLETED -> {
                        val filepath = intent.getStringExtra(MyUploadRoomService.EXTRA_ROOM_FILE_URI)
                        val roomID = intent.getStringExtra(MyUploadRoomService.EXTRA_ROOM_ID)
                        if (filepath != null && roomID != null) {
                            File(filepath)

                            val myFragment =
                                supportFragmentManager.findFragmentByTag(getString(R.string.tag_chats)) as AddDialogFragment?
                            if (myFragment != null && myFragment.isVisible) {
                                myFragment.dismissProgressBar()
                            }

                            AppUtils.showToast(
                                context,
                                "ADDED DIALOG COMPLETED", true
                            )
                        }
                    }

                    MyUploadRoomService.UPLOAD_ROOM_ERROR -> {
                        val myFragment =
                            supportFragmentManager.findFragmentByTag(getString(R.string.tag_chats)) as AddDialogFragment?
                        if (myFragment != null && myFragment.isVisible) {
                            myFragment.dismissProgressBar()
                        }
                        AppUtils.showToast(
                            context,
                            "ADDED DIALOG Error", true
                        )
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        if (fragments.size > 0) {
            val lastFragment = fragments[fragments.size - 1]
            if (lastFragment != null && lastFragment.isVisible) {
                if (lastFragment is MessagesFragment) {
                    if (lastFragment.onBackPressed()) {
                        supportFragmentManager.popBackStack(getString(R.string.tag_chats2), POP_BACK_STACK_INCLUSIVE)
                        supportFragmentManager.popBackStack(getString(R.string.tag_chats), POP_BACK_STACK_INCLUSIVE)

                        val transaction = supportFragmentManager.beginTransaction()
                        transaction.replace(R.id.view, DialogsFragment.newInstance())
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
