package net.korul.hbbft.CommonFragments.tabSettings

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.raizlabs.android.dbflow.config.FlowManager
import kotlinx.android.synthetic.main.dialog_emergency_delete.*
import kotlinx.android.synthetic.main.fragment_settings_storage.*
import net.korul.hbbft.CommonData.data.model.core.Getters
import net.korul.hbbft.CommonData.data.model.coreDataBase.AppDatabase
import net.korul.hbbft.CommonFragments.WarningFragment
import net.korul.hbbft.CoreHBBFT.RoomWork.unregisterInRoomInFirebase
import net.korul.hbbft.R


class SettingsStorageFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_storage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_back.setOnClickListener {
            (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }

        action_remove.setOnClickListener {
            val dialog = Dialog(context!!)
            dialog.setContentView(R.layout.dialog_emergency_delete)
            dialog.setTitle("")
            dialog.dialog_info.text =
                SpannableStringBuilder(context!!.getString(R.string.settings_emergency_delete_data))
            dialog.dialog_accept_button.text = SpannableStringBuilder(context!!.getString(R.string.action_ok))

            dialog.dialog_accept_button.setOnClickListener {
                val dialogs = Getters.getAllDialog()
                for (dial in dialogs)
                    unregisterInRoomInFirebase(dial.id)

                FlowManager.getDatabase(AppDatabase::class.java).reset(context)
                dialog.dismiss()
            }

            dialog.dialog_cancel_button.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }


        action_select_all.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                WarningFragment.newInstance(), getString(R.string.tag_settings)
            )
            transaction.addToBackStack(getString(R.string.tag_settings))
            transaction.commit()
        }
    }

    companion object {
        fun newInstance() = SettingsStorageFragment()
    }
}
