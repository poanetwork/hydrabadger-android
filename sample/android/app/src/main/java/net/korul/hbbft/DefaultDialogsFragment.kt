package net.korul.hbbft

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import com.stfalcon.chatkit.utils.DateFormatter
import kotlinx.android.synthetic.main.activity_default_dialogs.*
import net.korul.hbbft.CoreHBBFT.CoreHBBFTListener
import net.korul.hbbft.common.data.fixtures.DialogsFixtures
import net.korul.hbbft.common.data.fixtures.MessagesFixtures
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.conversation.Conversations
import net.korul.hbbft.common.data.model.core.Getters
import net.korul.hbbft.common.data.model.core.Getters.getDialogByRoomName
import net.korul.hbbft.features.DemoDialogsFragment
import net.korul.hbbft.features.holder.holders.dialogs.CustomDialogViewHolder
import java.util.*


private var handlerMes = Handler()

class DefaultDialogsFragment :
    DemoDialogsFragment(),
    DateFormatter.Formatter,
    CoreHBBFTListener {

    private var TAG = "HYDRABADGERTAG:DefaultDialogsFragment"

    companion object {
        fun newInstance(): DefaultDialogsFragment {
            return DefaultDialogsFragment()
        }

        fun newInstance(Start_App: Boolean, RoomName: String): DefaultDialogsFragment {
            val f = DefaultDialogsFragment()
            val b = Bundle()
            b.putBoolean("Start_App", Start_App)
            b.putString("RoomName", RoomName)
            f.arguments = b

            return f
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DatabaseApplication.mCoreHBBFT2X.addListener(this)

        val bundle = arguments
        if (bundle != null && !bundle.isEmpty && bundle.getBoolean("Start_App", false)) {
            Log.d(TAG, "Receive push and start activity")
            val roomName = bundle.getString("RoomName")

            val dialogs = DialogsFixtures.dialogs
            for (diag in dialogs) {
                if (diag.dialogName == roomName) {
                    Log.d(TAG, "Found dialog and start it $roomName")
                    startMesFragment(diag, true)
                }
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_default_dialogs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addDialog.setOnClickListener {
            onAddDialog()
        }
    }

    override fun onResume() {
        super.onResume()

        super.dialogsAdapter?.clear()
        initAdapter()
    }

    fun startMesFragment(dialog: Dialog, startHbbft: Boolean) {
        val transaction = activity!!.supportFragmentManager.beginTransaction()
        transaction.replace(R.id.view, DefaultMessagesFragment.newInstance(dialog, dialog.users[0], startHbbft))
        transaction.addToBackStack(getString(R.string.tag_chats))
        transaction.commit()
    }

    override fun onDialogClick(dialog: Dialog) {
        val transaction = activity!!.supportFragmentManager.beginTransaction()
        transaction.replace(R.id.view, DefaultMessagesFragment.newInstance(dialog, dialog.users[0]))
        transaction.addToBackStack(getString(R.string.tag_chats))
        transaction.commit()
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
        super.dialogsAdapter = DialogsListAdapter(
            R.layout.item_custom_dialog_view_holder,
            CustomDialogViewHolder::class.java,
            super.imageLoader
        )

        super.dialogsAdapter!!.setItems(DialogsFixtures.dialogs)

        super.dialogsAdapter!!.setOnDialogClickListener(this)
        super.dialogsAdapter!!.setOnDialogLongClickListener(this)
        super.dialogsAdapter!!.setDatesFormatter(this)

        dialogsList!!.setAdapter(super.dialogsAdapter)
    }

    override fun updateStateToOnline() {
//        menu!!.findItem(R.id.action_online).icon = DatabaseApplication.instance.resources.getDrawable(R.mipmap.ic_online_round)
    }

    override fun reciveMessage(you: Boolean, uid: String, mes: String) {
        try {
            handlerMes.postDelayed({
                if (!you) {
                    val roomName = DatabaseApplication.mCoreHBBFT2X.mRoomName
                    val dialog = getDialogByRoomName(roomName)

                    var found = false
                    for (user in dialog.users) {
                        if (user.uid == uid)
                            found = true
                    }
                    if (!found) {
                        val id = Getters.getNextUserID()
                        val user = User(
                            id,
                            uid,
                            id.toString(),
                            dialog.id,
                            "name${dialog.users.size}",
                            "http://i.imgur.com/pv1tBmT.png",
                            true
                        )
                        dialog.users.add(user)
                        Conversations.getDUser(user).insert()
                    }

                    val user = Getters.getUserbyUID(uid, dialog.id)
                    val mess = MessagesFixtures.setNewMessage(mes, dialog, user!!)

                    dialog.unreadCount++
                    Conversations.getDDialog(dialog).update()

                    onNewMessage(dialog.id, mess)
                }
            }, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun onAddDialog() {
        CreateNewDialog.open(context!!)
    }

    //for example
    fun onNewMessage(dialogId: String, message: Message) {
        val isUpdated = dialogsAdapter!!.updateDialogWithMessage(dialogId, message)
        if (!isUpdated) {
            //Dialog with this ID doesn't exist, so you can create new Dialog or update all dialogs list
        }
    }

    //for example
    fun onNewDialog(dialog: Dialog) {
        dialogsAdapter!!.addItem(dialog)
    }
}
