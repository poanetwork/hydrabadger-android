package net.korul.hbbft.Dialogs.holder.holders.messages

import android.view.View

import com.stfalcon.chatkit.messages.MessageHolders
import net.korul.hbbft.CommonData.data.model.Message

class CustomOutcomingTextMessageViewHolder(itemView: View) :
    MessageHolders.OutcomingTextMessageViewHolder<Message>(itemView) {

    override fun onBind(message: Message) {
        super.onBind(message)

//        time.text = message.status + " " + time.text
        time.text = time.text
    }
}
