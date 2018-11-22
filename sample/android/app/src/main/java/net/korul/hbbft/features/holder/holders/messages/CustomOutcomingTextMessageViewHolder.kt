package net.korul.hbbft.features.holder.holders.messages

import android.view.View

import com.stfalcon.chatkit.messages.MessageHolders
import net.korul.hbbft.common.data.model.Message

class CustomOutcomingTextMessageViewHolder(itemView: View, payload: Any) :
    MessageHolders.OutcomingTextMessageViewHolder<Message>(itemView, payload) {

    override fun onBind(message: Message) {
        super.onBind(message)

        time.text = message.status + " " + time.text
    }
}
