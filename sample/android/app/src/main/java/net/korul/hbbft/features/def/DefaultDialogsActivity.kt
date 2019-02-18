package net.korul.hbbft.features.def

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.stfalcon.chatkit.dialogs.DialogsList
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import com.stfalcon.chatkit.utils.DateFormatter
import kotlinx.android.synthetic.main.activity_default_dialogs.*
import net.korul.hbbft.CoreHBBFT.CoreHBBFTListener
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.DatabaseApplication.Companion.mToken
import net.korul.hbbft.R
import net.korul.hbbft.common.data.fixtures.DialogsFixtures
import net.korul.hbbft.common.data.fixtures.MessagesFixtures
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.conversation.Conversations
import net.korul.hbbft.common.data.model.core.Getters
import net.korul.hbbft.common.data.model.core.Getters.getDialogByRoomName
import net.korul.hbbft.features.DemoDialogsActivity
import net.korul.hbbft.features.holder.holders.dialogs.CustomDialogViewHolder
import java.util.*

class DefaultDialogsActivity:
    DemoDialogsActivity(),
    DateFormatter.Formatter,
    CoreHBBFTListener
{
    private var dialogsList: DialogsList? = null
    private var handlerMes = Handler()
    private var TAG = "HYDRABADGERTAG:DefaultDialogsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_default_dialogs)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

        dialogsList = findViewById<View>(R.id.dialogsList) as DialogsList

        addDialog.setOnClickListener {
            onAddDialog()
        }

        DatabaseApplication.mCoreHBBFT2X.addListener(this)
    }

    override fun onResume() {
        super.onResume()

        super.dialogsAdapter?.clear()
        initAdapter()
    }

    override fun onDialogClick(dialog: Dialog) {
        DefaultMessagesActivity.open(this, dialog, dialog.users[0])
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
                if(!you) {
                    val roomName = DatabaseApplication.mCoreHBBFT2X.mRoomName
                    val dialog = getDialogByRoomName(roomName)

                    var found = false
                    for (user in dialog.users) {
                        if(user.uid == uid)
                            found = true
                    }
                    if(!found) {
                        val id = Getters.getNextUserID()
                        val user = User(id, uid, id.toString(), dialog.id, "name${dialog.users.size}", "http://i.imgur.com/pv1tBmT.png", true)
                        dialog.users.add(user)
                        Conversations.getDUser(user).insert()
                    }

                    val user = Getters.getUserbyUID(uid, dialog.id)
                    val mess =  MessagesFixtures.setNewMessage(mes, dialog, user!!)

                    dialog.unreadCount++
                    Conversations.getDDialog(dialog).update()

                    onNewMessage(dialog.id, mess)
                }
            }, 0)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, DefaultDialogsActivity::class.java))
        }
    }


    fun onAddDialog() {
        CreateNewDialog.open(this)
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
