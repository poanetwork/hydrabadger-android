package net.korul.hbbft.Dialogs

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesListAdapter
import com.stfalcon.chatkit.utils.DateFormatter
import kotlinx.android.synthetic.main.fragment_default_messages.*
import net.korul.hbbft.CommonData.data.fixtures.MessagesFixtures
import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.data.model.Message
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.data.model.conversation.Conversations
import net.korul.hbbft.CommonData.data.model.core.Getters
import net.korul.hbbft.CommonData.data.model.core.Getters.getDialogByRoomId
import net.korul.hbbft.CommonData.utils.AppUtils
import net.korul.hbbft.CoreHBBFT.CoreHBBFTListener
import net.korul.hbbft.CoreHBBFT.IAddToContacts
import net.korul.hbbft.CoreHBBFT.UserWork.getUserFromLocalOrDownloadFromFirebase
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.DatabaseApplication.Companion.mCoreHBBFT2X
import net.korul.hbbft.Dialogs.holder.IncomingVoiceMessageViewHolder
import net.korul.hbbft.Dialogs.holder.OutcomingVoiceMessageViewHolder
import net.korul.hbbft.Dialogs.holder.holders.messages.CustomIncomingImageMessageViewHolder
import net.korul.hbbft.Dialogs.holder.holders.messages.CustomIncomingTextMessageViewHolder
import net.korul.hbbft.Dialogs.holder.holders.messages.CustomOutcomingImageMessageViewHolder
import net.korul.hbbft.Dialogs.holder.holders.messages.CustomOutcomingTextMessageViewHolder
import net.korul.hbbft.R
import java.util.*
import kotlin.concurrent.thread


class MessagesFragment :
    BaseMessagesFragment(),
    MessageInput.InputListener,
    MessageInput.AttachmentsListener,
    MessageInput.TypingListener,
    DateFormatter.Formatter,
    MessageHolders.ContentChecker<Message>,
    DialogInterface.OnClickListener,
    CoreHBBFTListener {

    companion object {
        private const val CONTENT_TYPE_VOICE: Byte = 1

        private val handlerProgress = Handler()
        private val handlerNewMes = Handler()

        fun newInstance(dialog: Dialog, user: User): MessagesFragment {
            mCoreHBBFT2X.setCurRoomId(dialog.id)

            val f = MessagesFragment()
            val b = Bundle()
            b.putString("dialog", Gson().toJson(dialog))
            b.putString("user", Gson().toJson(user))
            b.putBoolean("startHbbft", false)
            f.arguments = b

            return f
        }

        fun newInstance(dialog: Dialog, user: User, startHbbft: Boolean): MessagesFragment {
            mCoreHBBFT2X.setCurRoomId(dialog.id)

            val f = MessagesFragment()
            val b = Bundle()
            b.putString("dialog", Gson().toJson(dialog))
            b.putString("user", Gson().toJson(user))
            b.putBoolean("startHbbft", startHbbft)
            f.arguments = b

            return f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            progress.dismiss()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var handlerShowProcess = Handler(Handler.Callback { msg ->
        try {
            progress.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = arguments
        if (bundle != null && !bundle.isEmpty) {
            val extraDialog = bundle.getString("dialog")
            super.mCurDialog = Gson().fromJson(extraDialog, Dialog::class.java)
            mCurDialog!!.unreadCount = 0

            Conversations.getDDialog(mCurDialog!!).update()

            val extraUser = bundle.getString("user")
            super.mCurUser = Gson().fromJson(extraUser, User::class.java)
        }

        if (bundle != null && bundle.getBoolean("startHbbft", false))
            startAll()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_default_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAdapter()

        input.setInputListener(this)
        input.setTypingListener(this)
        input.setAttachmentsListener(this)
    }

    override fun onSubmit(input: CharSequence): Boolean {
        if (isNeedVilibleMenuHbbft()) {
            val mes = MessagesFixtures.setNewMessage(input.toString(), mCurDialog!!, mCurUser!!)
            super.messagesAdapter!!.addToStart(
                mes, true
            )

            DatabaseApplication.mCoreHBBFT2X.sendMessage(mes)
            mCurDialog = getDialogByRoomId(mCurDialog!!.id)
        } else {
            val mSnackbar = Snackbar.make(view!!, getString(R.string.need_online), Snackbar.LENGTH_LONG)
                .setAction("Action", null)

            val snackbarView = mSnackbar.view
            snackbarView.setBackgroundColor(Color.BLUE)
            mSnackbar.show()
        }

        return true
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        DatabaseApplication.mCoreHBBFT2X.addListener(this)
    }

    override fun onDetach() {
        super.onDetach()

        DatabaseApplication.mCoreHBBFT2X.delListener(this)
    }

    fun onBackPressed(): Boolean {
        return if (super.selectionCount == 0) {
            true
        } else {
            messagesAdapter!!.unselectAllItems()
            false
        }
    }

    fun startAll() {
        val msg = handlerShowProcess.obtainMessage(0)
        msg.obj = ""
        handlerShowProcess.sendMessage(msg)

        DatabaseApplication.mCoreHBBFT2X.subscribeSession()
        DatabaseApplication.mCoreHBBFT2X.afterSubscribeSession()

        if (DatabaseApplication.mCoreHBBFT2X.mShowError) {
            val builder: android.app.AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.app.AlertDialog.Builder(context!!, android.R.style.Theme_Material_Dialog_Alert)
            } else {
                android.app.AlertDialog.Builder(context!!)
            }
            builder.setTitle("Error ")
                .setMessage("Dll Error")
                .setPositiveButton(android.R.string.yes) { dialog, _ ->
                    dialog.cancel()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }

        DatabaseApplication.mCoreHBBFT2X.startAllNode(mCurDialog!!.id)
    }

    override fun onAddAttachments() {
        AlertDialog.Builder(context!!)
            .setItems(R.array.view_types_dialog, this)
            .show()
    }

    override fun format(date: Date): String {
        return when {
            DateFormatter.isToday(date) -> getString(R.string.date_header_today)
            DateFormatter.isYesterday(date) -> getString(R.string.date_header_yesterday)
            else -> DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH_YEAR)
        }
    }

    override fun hasContentFor(message: Message, type: Byte): Boolean {
        when (type) {
            CONTENT_TYPE_VOICE -> return (message.voice != null && !message.voice!!.url.isEmpty())
        }
        return false
    }

    override fun onClick(dialogInterface: DialogInterface, i: Int) {
        when (i) {
            0 -> {
                val mes = MessagesFixtures.getImageMessage(mCurDialog!!, mCurUser!!)
                messagesAdapter!!.addToStart(mes, true)

                DatabaseApplication.mCoreHBBFT2X.sendMessage(mes)
                mCurDialog = getDialogByRoomId(mCurDialog!!.id)
            }
            1 -> {
                val mes = MessagesFixtures.getVoiceMessage(mCurDialog!!, mCurUser!!)
                messagesAdapter!!.addToStart(mes, true)

                DatabaseApplication.mCoreHBBFT2X.sendMessage(mes)
                mCurDialog = getDialogByRoomId(mCurDialog!!.id)
            }
        }
    }

    override fun updateStateToError() {
        try {
            handlerProgress.post {
                progress.dismiss()
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
        try {
            handlerProgress.post {
                progress.dismiss()
                menu!!.findItem(R.id.action_online).icon =
                    DatabaseApplication.instance.resources.getDrawable(R.mipmap.ic_online_round)
                hideMenuHbbft1()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideMenuHbbft1() {
        menu?.findItem(R.id.action_startALL)?.isVisible = false
    }

    override fun setOnlineUser(uid: String, online: Boolean) {
        val users = mCurDialog!!.users.filter { it.uid == uid && !it.isOnline }
        for (user in users) {
            val us = Conversations.getDUser(user)
            us.isOnline = online
            us.update()
        }
//        messagesAdapter!!.notifyDataSetChanged()
    }

    override fun reciveMessageWithDate(you: Boolean, uid: String, mes: String, mesID: Long, date: Date) {
        thread {
            try {
                if (!you) {
                    if (!mCurDialog!!.users.any { it.uid == uid }) {
                        getUserFromLocalOrDownloadFromFirebase(uid, mCurDialog!!.id, object : IAddToContacts {
                            override fun errorAddContact() {
                            }

                            override fun user(user: User) {
                                handlerNewMes.post {
                                    mCurDialog!!.users.add(user)
                                    Conversations.getDUser(user).insert()

                                    val userMes = Getters.getUserbyUIDFromDialog(uid, mCurDialog!!.id)
                                    val mess = MessagesFixtures.setNewMessage(mes, mesID, mCurDialog!!, userMes!!, date)
//                                    messagesAdapter!!.addToStart(mess, true)
                                    val listMes: MutableList<Message> = arrayListOf()
                                    listMes.add(mess)

                                    messagesAdapter!!.notifyDataSetChanged()
                                    loadMessages()
                                    mCurDialog = getDialogByRoomId(mCurDialog!!.id)
                                }
                            }
                        })
                    } else {
                        handlerNewMes.post {
                            val user = Getters.getUserbyUIDFromDialog(uid, mCurDialog!!.id)
                            val mess = MessagesFixtures.setNewMessage(mes, mesID, mCurDialog!!, user!!, date)
//                            messagesAdapter!!.addToStart(mess, true)
                            val listMes: MutableList<Message> = arrayListOf()
                            listMes.add(mess)

                            messagesAdapter!!.notifyDataSetChanged()
                            loadMessages()
                            mCurDialog = getDialogByRoomId(mCurDialog!!.id)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun reciveMessage(you: Boolean, uid: String, mes: String, mesID: Long, date: Date) {
        thread {
            try {
                if (!you) {
                    if (!mCurDialog!!.users.any { it.uid == uid }) {
                        getUserFromLocalOrDownloadFromFirebase(uid, mCurDialog!!.id, object : IAddToContacts {
                            override fun errorAddContact() {
                            }

                            override fun user(user: User) {
                                handlerNewMes.post {
                                    mCurDialog!!.users.add(user)
                                    Conversations.getDUser(user).insert()

                                    val userMes = Getters.getUserbyUIDFromDialog(uid, mCurDialog!!.id)
                                    val mess = MessagesFixtures.setNewMessage(mes, mesID, mCurDialog!!, userMes!!, date)
                                    messagesAdapter!!.addToStart(mess, true)
                                    mCurDialog = getDialogByRoomId(mCurDialog!!.id)
                                }
                            }
                        })
                    } else {
                        handlerNewMes.post {
                            val user = Getters.getUserbyUIDFromDialog(uid, mCurDialog!!.id)
                            val mess = MessagesFixtures.setNewMessage(mes, mesID, mCurDialog!!, user!!, date)
                            messagesAdapter!!.addToStart(mess, true)
                            mCurDialog = getDialogByRoomId(mCurDialog!!.id)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initAdapter() {
        val payload = CustomIncomingTextMessageViewHolder.Payload()
        payload.avatarClickListener = object : CustomIncomingTextMessageViewHolder.OnAvatarClickListener {
            override fun onAvatarClick() {
                AppUtils.showToast(
                    context!!,
                    "message avatar clicked", true
                )
            }
        }

        val holders = MessageHolders()
            // custom type
            .registerContentType(
                CONTENT_TYPE_VOICE,
                IncomingVoiceMessageViewHolder::class.java,
                R.layout.item_custom_incoming_voice_message,
                OutcomingVoiceMessageViewHolder::class.java,
                R.layout.item_custom_outcoming_voice_message,
                this
            )
            // custom holder
            .setIncomingTextConfig(
                CustomIncomingTextMessageViewHolder::class.java,
                R.layout.item_custom_incoming_text_message,
                payload
            )
            .setOutcomingTextConfig(
                CustomOutcomingTextMessageViewHolder::class.java,
                R.layout.item_custom_outcoming_text_message
            )
            .setIncomingImageConfig(
                CustomIncomingImageMessageViewHolder::class.java,
                R.layout.item_custom_incoming_image_message
            )
            .setOutcomingImageConfig(
                CustomOutcomingImageMessageViewHolder::class.java,
                R.layout.item_custom_outcoming_image_message
            )
            // custom layout
            .setIncomingTextLayout(R.layout.item_custom_incoming_text_message)
            .setOutcomingTextLayout(R.layout.item_custom_outcoming_text_message)
            .setIncomingImageLayout(R.layout.item_custom_incoming_image_message)
            .setOutcomingImageLayout(R.layout.item_custom_outcoming_image_message)


        super.messagesAdapter = MessagesListAdapter(mCurUser?.id, holders, super.imageLoader)
        super.messagesAdapter!!.enableSelectionMode(this)
        super.messagesAdapter!!.setLoadMoreListener(this)
        super.messagesAdapter!!.setDateHeadersFormatter(this)
        super.messagesAdapter!!.registerViewClickListener(
            R.id.messageUserAvatar
        ) { _, message ->
            AppUtils.showToast(
                context!!,
                message.user.name + " avatar click",
                false
            )
        }
        this.messagesList!!.setAdapter(super.messagesAdapter)
    }

    override fun onStartTyping() {
        Log.v("Typing listener", getString(R.string.start_typing_status))
    }

    override fun onStopTyping() {
        Log.v("Typing listener", getString(R.string.stop_typing_status))
    }

}
