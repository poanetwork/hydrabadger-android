package net.korul.hbbft.features

import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.gson.Gson
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesListAdapter
import com.stfalcon.chatkit.utils.DateFormatter
import kotlinx.android.synthetic.main.fragment_default_messages.*
import net.korul.hbbft.CoreHBBFT.CoreHBBFTListener
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.DatabaseApplication.Companion.mCoreHBBFT2X
import net.korul.hbbft.R
import net.korul.hbbft.common.data.fixtures.MessagesFixtures
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.conversation.Conversations
import net.korul.hbbft.common.data.model.core.Getters
import net.korul.hbbft.common.data.model.core.Getters.getDialog
import net.korul.hbbft.common.data.model.core.Getters.getNextUserID
import net.korul.hbbft.common.utils.AppUtils
import net.korul.hbbft.features.holder.IncomingVoiceMessageViewHolder
import net.korul.hbbft.features.holder.OutcomingVoiceMessageViewHolder
import net.korul.hbbft.features.holder.holders.messages.CustomIncomingImageMessageViewHolder
import net.korul.hbbft.features.holder.holders.messages.CustomIncomingTextMessageViewHolder
import net.korul.hbbft.features.holder.holders.messages.CustomOutcomingImageMessageViewHolder
import net.korul.hbbft.features.holder.holders.messages.CustomOutcomingTextMessageViewHolder
import java.util.*

class DefaultMessagesFragment :
    DemoMessagesFragment(),
    MessageInput.InputListener,
    MessageInput.AttachmentsListener,
    MessageInput.TypingListener,
    DateFormatter.Formatter,
    MessageHolders.ContentChecker<Message>,
    DialogInterface.OnClickListener,
    CoreHBBFTListener {

    companion object {
        private val CONTENT_TYPE_VOICE: Byte = 1

        private val handler = Handler()

        fun newInstance(dialog: Dialog, user: User): DefaultMessagesFragment {
            mCoreHBBFT2X.setRoomName(dialog.dialogName)

            val f = DefaultMessagesFragment()
            val b = Bundle()
            b.putString("dialog", Gson().toJson(dialog))
            b.putString("user", Gson().toJson(user))
            b.putBoolean("startHbbft", false)
            f.arguments = b

            return f
        }

        fun newInstance(dialog: Dialog, user: User, startHbbft: Boolean): DefaultMessagesFragment {
            mCoreHBBFT2X.setRoomName(dialog.dialogName)

            val f = DefaultMessagesFragment()
            val b = Bundle()
            b.putString("dialog", Gson().toJson(dialog))
            b.putString("user", Gson().toJson(user))
            b.putBoolean("startHbbft", startHbbft)
            f.arguments = b

            return f
        }
    }

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

        if(bundle != null && bundle.getBoolean("startHbbft", false))
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
        val mes = MessagesFixtures.setNewMessage(input.toString(), mCurDialog!!, mCurUser!!)
        super.messagesAdapter!!.addToStart(
            mes, true
        )

        DatabaseApplication.mCoreHBBFT2X.sendMessage(mes.text.toString())
        mCurDialog = getDialog(mCurDialog!!.id)

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

    fun startAll() {
        handler.post {
            progress.show()
        }
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

        DatabaseApplication.mCoreHBBFT2X.startAllNode(mCurDialog!!.dialogName)
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

                DatabaseApplication.mCoreHBBFT2X.sendMessage(mes.text.toString())
                mCurDialog = getDialog(mCurDialog!!.id)
            }
            1 -> {
                val mes = MessagesFixtures.getVoiceMessage(mCurDialog!!, mCurUser!!)
                messagesAdapter!!.addToStart(mes, true)

                DatabaseApplication.mCoreHBBFT2X.sendMessage(mes.text.toString())
                mCurDialog = getDialog(mCurDialog!!.id)
            }
        }
    }

    override fun updateStateToOnline() {
        try {
            handler.post {
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

    override fun reciveMessage(you: Boolean, uid: String, mes: String) {
        try {
            handler.post {
                if (!you) {
                    var found = false
                    for (user in mCurDialog!!.users) {
                        if (user.uid == uid)
                            found = true
                    }
                    if (!found) {
                        val id = getNextUserID()
                        val user = User(
                            id,
                            uid,
                            id.toString(),
                            mCurDialog!!.id,
                            "name${mCurDialog!!.users.size}",
                            "http://i.imgur.com/pv1tBmT.png",
                            true
                        )
                        mCurDialog!!.users.add(user)
                        Conversations.getDUser(user).insert()
                        Conversations.getDDialog(mCurDialog!!).update()
                    }

                    val user = Getters.getUserbyUID(uid, mCurDialog!!.id)

                    super.messagesAdapter!!.addToStart(
                        MessagesFixtures.setNewMessage(mes, mCurDialog!!, user!!), true
                    )
                    super.messagesAdapter!!.notifyDataSetChanged()
                    mCurDialog = getDialog(mCurDialog!!.id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initAdapter() {
        //We can pass any data to ViewHolder with payload
        val payload = CustomIncomingTextMessageViewHolder.Payload()
        //For example click listener
        payload.avatarClickListener = object : CustomIncomingTextMessageViewHolder.OnAvatarClickListener {
            override fun onAvatarClick() {
                Toast.makeText(
                    context!!,
                    "Text message avatar clicked", Toast.LENGTH_SHORT
                ).show()
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
        ) { view, message ->
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
