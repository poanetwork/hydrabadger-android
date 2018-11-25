package net.korul.hbbft.features.def

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.messages.MessageInput
import com.stfalcon.chatkit.messages.MessagesList
import com.stfalcon.chatkit.messages.MessagesListAdapter
import com.stfalcon.chatkit.utils.DateFormatter
import net.korul.hbbft.CoreHBBFT.CoreHBBFT
import net.korul.hbbft.CoreHBBFT.CoreHBBFTListener
import net.korul.hbbft.DatabaseApplication
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
import net.korul.hbbft.features.DemoMessagesActivity
import net.korul.hbbft.features.holder.IncomingVoiceMessageViewHolder
import net.korul.hbbft.features.holder.OutcomingVoiceMessageViewHolder
import net.korul.hbbft.features.holder.holders.messages.CustomIncomingImageMessageViewHolder
import net.korul.hbbft.features.holder.holders.messages.CustomIncomingTextMessageViewHolder
import net.korul.hbbft.features.holder.holders.messages.CustomOutcomingImageMessageViewHolder
import net.korul.hbbft.features.holder.holders.messages.CustomOutcomingTextMessageViewHolder
import java.util.*

class DefaultMessagesActivity :
    DemoMessagesActivity(),
    MessageInput.InputListener,
    MessageInput.AttachmentsListener,
    MessageInput.TypingListener,
    DateFormatter.Formatter,
    MessageHolders.ContentChecker<Message>,
    DialogInterface.OnClickListener,
    CoreHBBFTListener
{
    private var messagesList: MessagesList? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_messages)

        val extraDialog = intent.getStringExtra("dialog")
        super.mCurDialog = Gson().fromJson(extraDialog, Dialog::class.java)

        val extraUser = intent.getStringExtra("user")
        super.mCurUser = Gson().fromJson(extraUser, User::class.java)

        this.messagesList = findViewById<View>(R.id.messagesList) as MessagesList
        initAdapter()

        val input = findViewById<View>(R.id.input) as MessageInput
        input.setInputListener(this)
        input.setTypingListener(this)
        input.setAttachmentsListener(this)

        CoreHBBFT.addListener(this)
    }

    override fun onSubmit(input: CharSequence): Boolean {
        val mes = MessagesFixtures.setNewMessage(input.toString(), mCurDialog!!, mCurUser!!)
        super.messagesAdapter!!.addToStart(
            mes, true
        )

        CoreHBBFT.sendMessage(mCurUser!!.uid, mes.text.toString())

        mCurDialog = getDialog(mCurDialog!!.id)

        return true
    }

    override fun onAddAttachments() {
        AlertDialog.Builder(this)
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
            CONTENT_TYPE_VOICE -> return (message.voice != null
                    && message.voice!!.url != null
                    && !message.voice!!.url.isEmpty())
        }
        return false
    }

    override fun onClick(dialogInterface: DialogInterface, i: Int) {
        when (i) {
            0 ->  {
                val mes = MessagesFixtures.getImageMessage(mCurDialog!!, mCurUser!!)
                mCurDialog = getDialog(mCurDialog!!.id)
                messagesAdapter!!.addToStart(mes, true)
            }
            1 -> {
                val mes = MessagesFixtures.getVoiceMessage(mCurDialog!!, mCurUser!!)
                mCurDialog = getDialog(mCurDialog!!.id)
                messagesAdapter!!.addToStart(mes, true)
            }
        }
    }

    override fun updateStateToOnline() {
        super.menu!!.findItem(R.id.action_online).icon = DatabaseApplication.instance.resources.getDrawable(R.mipmap.ic_online_round)
    }

    override fun reciveMessage(you: Boolean, uid: String, mes: String) {
        if(mCurUser!!.uid != uid) {
            var found = false
            for (user in mCurDialog!!.users) {
                if(user.uid == uid)
                    found = true
            }
            if(!found) {
                val id = getNextUserID()
                val muser: User = User(id, uid, id.toString(), mCurDialog!!.id, "name${mCurDialog!!.users.size}", "http://i.imgur.com/pv1tBmT.png", true)
                mCurDialog!!.users.add(muser)
                Conversations.getDUser(muser).insert()
                Conversations.getDDialog(mCurDialog!!).update()
            }

            val user = Getters.getUserbyUID(uid, mCurDialog!!.id)

            super.messagesAdapter!!.addToStart(
                MessagesFixtures.setNewMessage(mes, mCurDialog!!, user!!), true
            )
        }
    }

    private fun initAdapter() {
        //We can pass any data to ViewHolder with payload
        val payload = CustomIncomingTextMessageViewHolder.Payload()
        //For example click listener
        payload.avatarClickListener = object : CustomIncomingTextMessageViewHolder.OnAvatarClickListener {
            override fun onAvatarClick() {
                Toast.makeText(
                    this@DefaultMessagesActivity,
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
                this@DefaultMessagesActivity,
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

    companion object {

        private val CONTENT_TYPE_VOICE: Byte = 1

        fun open(context: Context, dialog: Dialog, user: User) {
            val intent = Intent(context, DefaultMessagesActivity::class.java)
            intent.putExtra("dialog", Gson().toJson(dialog))
            intent.putExtra("user", Gson().toJson(user))
            context.startActivity(intent)
        }
    }
}
