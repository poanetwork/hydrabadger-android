package net.korul.hbbft.Dialogs.holder.holders.messages

import android.view.View
import android.widget.TextView

import com.stfalcon.chatkit.messages.MessageHolders
import net.korul.hbbft.CommonData.data.model.Message
import net.korul.hbbft.R

class CustomOutcomingTextMessageViewHolder(itemView: View) :
    MessageHolders.OutcomingTextMessageViewHolder<Message>(itemView) {

    private val messageTime: TextView = itemView.findViewById<View>(R.id.messageTime) as TextView

    override fun onBind(message: Message) {
        super.onBind(message)

//        time.text = message.status + " " + time.text
        messageTime.text = time.text
    }
}
