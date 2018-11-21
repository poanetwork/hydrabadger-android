package net.korul.hbbft.chatkit.sample.features.demo.holder.holders.messages

import android.view.View

import com.stfalcon.chatkit.messages.MessageHolders
import net.korul.hbbft.chatkit.sample.common.data.model.Message

class CustomOutcomingTextMessageViewHolder(itemView: View, payload: Any) :
    MessageHolders.OutcomingTextMessageViewHolder<Message>(itemView, payload) {

    override fun onBind(message: Message) {
        super.onBind(message)

        time.text = message.status + " " + time.text
    }
}
