package net.korul.hbbft.Dialogs

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.stfalcon.chatkit.utils.DateFormatter
import kotlinx.android.synthetic.main.fragment_default_dialogs.*
import net.korul.hbbft.AdapterRecycler.DialogsSwipeListAdapter
import net.korul.hbbft.CommonData.data.fixtures.DialogsFixtures
import net.korul.hbbft.CommonData.data.fixtures.MessagesFixtures
import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.data.model.Message
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.data.model.conversation.Conversations
import net.korul.hbbft.CommonData.data.model.core.Getters
import net.korul.hbbft.CommonData.data.model.core.Getters.getDialogByRoomId
import net.korul.hbbft.CommonFragments.tabChats.AboutRoomFragment
import net.korul.hbbft.CommonFragments.tabChats.AddDialogExistFragment
import net.korul.hbbft.CommonFragments.tabChats.AddNewDialogFragment
import net.korul.hbbft.CoreHBBFT.CoreHBBFT
import net.korul.hbbft.CoreHBBFT.CoreHBBFTListener
import net.korul.hbbft.CoreHBBFT.IAddToContacts
import net.korul.hbbft.CoreHBBFT.UserWork.getUserFromLocalOrDownloadFromFirebase
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.R
import java.util.*


interface DialogClickListener {
    fun onItemButtonAboutClick(view: View, dialog: Dialog)
    fun onItemButtonRemoeClick(view: View, dialog: Dialog)
}

class DialogsFragment :
    BaseDialogsFragment(),
    DateFormatter.Formatter,
    CoreHBBFTListener {
    private var TAG = "HYDRA:DialogsFragment"

    companion object {
        //        private val handlerProgress = Handler()
        private val handlerNewMes = Handler()

        fun newInstance(): DialogsFragment {
            return DialogsFragment()
        }

        fun newInstance(Start_App: Boolean, RoomId: String): DialogsFragment {
            val f = DialogsFragment()
            val b = Bundle()
            b.putBoolean("Start_App", Start_App)
            b.putString("RoomId", RoomId)
            f.arguments = b

            return f
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = arguments
        if (bundle != null && !bundle.isEmpty && bundle.getBoolean("Start_App", false)) {
            Log.d(TAG, "Receive push and start activity")
            val roomId = bundle.getString("RoomId")

            val dialogs = DialogsFixtures.dialogs
            for (diag in dialogs) {
                if (diag.id == roomId) {
                    val curuser = diag.users.first { it.uid == CoreHBBFT.uniqueID1 }
                    Log.d(TAG, "Found dialog and start it $roomId")
                    startMesFragment(diag, true, curuser)
                }
            }
        }

    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        DatabaseApplication.mCoreHBBFT2X.addListener(this)
    }

    override fun onDetach() {
        super.onDetach()

        DatabaseApplication.mCoreHBBFT2X.delListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_default_dialogs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addDialog.setOnClickListener {
            AlertDialog.Builder(context!!)
                .setItems(R.array.view_do_new_dialog) { _, which ->
                    when (which) {
                        0 -> {
                            onAddExistDialog()
                        }
                        1 -> {
                            onAddNewDialog()
                        }
                    }
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()

        super.dialogsAdapter?.clear()
        initAdapter()
    }

    fun startMesFragment(dialog: Dialog, startHbbft: Boolean, curuser: User) {
        activity!!.supportFragmentManager.popBackStack(
            getString(R.string.tag_chats),
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        val transaction = activity!!.supportFragmentManager.beginTransaction()
        transaction.replace(
            R.id.view,
            MessagesFragment.newInstance(dialog, curuser, startHbbft)
        )
        transaction.addToBackStack(getString(R.string.tag_chats2))
        transaction.commit()
    }

    override fun onDialogClick(dialog: Dialog) {
        activity!!.supportFragmentManager.popBackStack(
            getString(R.string.tag_chats),
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        val transaction = activity!!.supportFragmentManager.beginTransaction()
        val curuser = dialog.users.first { it.uid == CoreHBBFT.uniqueID1 }
        transaction.replace(
            R.id.view,
            MessagesFragment.newInstance(dialog, curuser)
        )
        transaction.addToBackStack(getString(R.string.tag_chats2))
        transaction.commit()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> {
                AlertDialog.Builder(context!!)
                    .setItems(R.array.view_do_new_dialog) { _, which ->
                        when (which) {
                            0 -> {
                                onAddExistDialog()
                            }
                            1 -> {
                                onAddNewDialog()
                            }
                        }
                    }
                    .show()
            }
        }

        return true
    }

    override fun format(date: Date): String {
        return when {
            DateFormatter.isToday(date) -> DateFormatter.format(date, DateFormatter.Template.TIME)
            DateFormatter.isYesterday(date) -> getString(R.string.date_header_yesterday)
            DateFormatter.isCurrentYear(date) -> DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH)
            else -> DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH_YEAR)
        }
    }


    private fun initAdapter() {
        super.dialogsAdapter = DialogsSwipeListAdapter(
            R.layout.item_custom_swipe_dialog_view_holder,
            super.imageLoader,
            object : DialogClickListener {
                override fun onItemButtonAboutClick(view: View, dialog: Dialog) {
                    val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                    transaction.add(
                        R.id.view,
                        AboutRoomFragment.newInstance(dialog), getString(R.string.tag_chats)
                    )
                    transaction.addToBackStack(getString(R.string.tag_chats))
                    transaction.commit()
                }

                override fun onItemButtonRemoeClick(view: View, dialog: Dialog) {
                    dialogsAdapter?.deleteById(dialog.id)
                    dialogsAdapter?.notifyDataSetChanged()
                    DialogsFixtures.deleteDialog(dialog)
                }
            })

        super.dialogsAdapter!!.setItems(DialogsFixtures.dialogs)
        super.dialogsAdapter!!.setOnDialogClickListener(this)
        super.dialogsAdapter!!.setOnDialogLongClickListener(this)
        super.dialogsAdapter!!.setDatesFormatter(this)
        super.dialogsAdapter!!.setOnDialogViewClickListener { _, dialog ->
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                AboutRoomFragment.newInstance(dialog), getString(R.string.tag_chats)
            )
            transaction.addToBackStack(getString(R.string.tag_chats))
            transaction.commit()
        }

        dialogsList!!.setAdapter(super.dialogsAdapter!!, false)
    }

    override fun updateStateToError() {
        try {
            Handler().post {
                val mSnackbar = Snackbar.make(view!!, getString(R.string.need_users), Snackbar.LENGTH_LONG)
                    .setAction("Action", null)

                val snackbarView = mSnackbar.view
                snackbarView.setBackgroundColor(Color.BLUE)
                mSnackbar.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun updateStateToOnline() {
        menu?.findItem(R.id.action_online)?.icon =
            DatabaseApplication.instance.resources.getDrawable(R.mipmap.ic_online_round)
    }

    override fun setOnlineUser(uid: String, online: Boolean) {
        val users =
            getDialogByRoomId(DatabaseApplication.mCoreHBBFT2X.mCurRoomId).users.filter { it.uid == uid && !it.isOnline }
        for (user in users) {
            val us = Conversations.getDUser(user)
            us.isOnline = online
            us.update()
        }
//        dialogsAdapter!!.notifyDataSetChanged()
    }

    override fun reciveMessage(you: Boolean, uid: String, mes: String, data: Date) {
        try {
            if (!you) {
                val dialog = getDialogByRoomId(DatabaseApplication.mCoreHBBFT2X.mCurRoomId)
                if (!dialog.users.any { it.uid == uid }) {
                    getUserFromLocalOrDownloadFromFirebase(uid, dialog.id, object : IAddToContacts {
                        override fun errorAddContact() {
                        }

                        override fun user(user: User) {
                            Handler().post {
                                dialog.users.add(user)
                                Conversations.getDUser(user).insert()

                                val userMes = Getters.getUserbyUIDFromDialog(uid, dialog.id)
                                val mess = MessagesFixtures.setNewMessage(mes, dialog, userMes!!, data)

                                dialog.unreadCount++
                                Conversations.getDDialog(dialog).update()

                                onNewMessage(dialog.id, mess)
                            }
                        }
                    })
                } else {
                        val user = Getters.getUserbyUIDFromDialog(uid, dialog.id)
                        val mess = MessagesFixtures.setNewMessage(mes, dialog, user!!, data)

                        dialog.unreadCount++
                        Conversations.getDDialog(dialog).update()

                        onNewMessage(dialog.id, mess)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onAddExistDialog() {
        val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
        transaction.add(R.id.view, AddDialogExistFragment.newInstance(), getString(R.string.tag_chats))
        transaction.addToBackStack(getString(R.string.tag_chats))
        transaction.commit()
    }


    fun onAddNewDialog() {
        val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
        transaction.add(R.id.view, AddNewDialogFragment.newInstance(), getString(R.string.tag_chats))
        transaction.addToBackStack(getString(R.string.tag_chats))
        transaction.commit()
    }


    fun onNewMessage(dialogId: String, message: Message) {
        try {
            handlerNewMes.post {
                val isUpdated = dialogsAdapter!!.updateDialogWithMessage(dialogId, message)
                dialogsAdapter!!.sortByLastMessageDate()
                if (!isUpdated) {
                    //Dialog with this ID doesn't exist, so you can create new Dialog or update all dialogs list
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun onNewDialog(dialog: Dialog) {
        dialogsAdapter!!.addItem(dialog)
    }
}
