package net.korul.hbbft.CommonFragments.tabSettings

import android.app.Application
import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.raizlabs.android.dbflow.config.FlowManager
import kotlinx.android.synthetic.main.dialog_emergency_delete.*
import kotlinx.android.synthetic.main.fragment_settings.*
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.data.model.core.Getters.getAllDialog
import net.korul.hbbft.CommonData.data.model.coreDataBase.AppDatabase
import net.korul.hbbft.CoreHBBFT.CoreHBBFT
import net.korul.hbbft.CoreHBBFT.RoomWork.unregisterInRoomInFirebase
import net.korul.hbbft.CoreHBBFT.UserWork.unregisterUserInFirebase
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.DatabaseApplication.Companion.mCurUser
import net.korul.hbbft.R


class SettingsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onResume() {
        super.onResume()

        settings_account_name.text = mCurUser.nick
        settings_account_email.text = mCurUser.name
        settings_account_id.text = mCurUser.uid

        if (mCurUser.avatar != "") {
            val image = BitmapFactory.decodeFile(mCurUser.avatar)
            settings_account_icon.setImageBitmap(image)
        } else {
            settings_account_icon.setImageResource(R.drawable.ic_contact)
        }

        accout_info.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                SettingsUserFragment.newInstance(), getString(R.string.tag_settings)
            )
            transaction.addToBackStack(getString(R.string.tag_settings))
            transaction.commit()
        }

        settings_interface.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                DialogThemeFragment.newInstance(), getString(R.string.tag_settings)
            )
            transaction.addToBackStack(getString(R.string.tag_settings))
            transaction.commit()
        }

        settings_notifications.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                NotificationFragment.newInstance(), getString(R.string.tag_settings)
            )
            transaction.addToBackStack(getString(R.string.tag_settings))
            transaction.commit()
        }

        settings_security.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                SettingsSecurityFragment.newInstance(), getString(R.string.tag_settings)
            )
            transaction.addToBackStack(getString(R.string.tag_settings))
            transaction.commit()
        }

        settings_media_storage.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                SettingsStorageFragment.newInstance(), getString(R.string.tag_settings)
            )
            transaction.addToBackStack(getString(R.string.tag_settings))
            transaction.commit()
        }

        settings_emergency_delete.setOnClickListener {
            val dialog = Dialog(context!!)
            dialog.setContentView(R.layout.dialog_emergency_delete)
            dialog.setTitle("")
            dialog.dialog_info.text =
                SpannableStringBuilder(context!!.getString(R.string.settings_emergency_delete_data_info))
            dialog.dialog_accept_button.text =
                SpannableStringBuilder(context!!.getString(R.string.settings_emergency_delete_data_accept))

            dialog.dialog_accept_button.setOnClickListener {
                val dialogs = getAllDialog()
                for (dialog in dialogs)
                    unregisterInRoomInFirebase(dialog.id)

                unregisterUserInFirebase(CoreHBBFT.uniqueID1)

                val prefs = CoreHBBFT.mApplicationContext.getSharedPreferences("cur_user", Application.MODE_PRIVATE)
                prefs.edit().putString("current_user", "").apply()

                DatabaseApplication.mCurUser = User(1L, "", "", "", "", "", "", true, true)

                FlowManager.getDatabase(AppDatabase::class.java).reset(context)
                val mSettings =
                    DatabaseApplication.instance.getSharedPreferences(CoreHBBFT.APP_PREFERENCES, Context.MODE_PRIVATE)
                val editor = mSettings.edit()
                editor.putString(CoreHBBFT.APP_PREFERENCES_NAME1, "")
                editor.putString(CoreHBBFT.APP_PREFERENCES_NAME2, "")
                editor.apply()
                dialog.dismiss()
            }

            dialog.dialog_cancel_button.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
