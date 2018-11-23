package net.korul.hbbft.features.holder.holders.messages

import android.view.View

import com.stfalcon.chatkit.messages.MessageHolders
import net.korul.hbbft.R
import net.korul.hbbft.common.data.model.Message

/*
 * Created by troy379 on 05.04.17.
 */
class CustomIncomingImageMessageViewHolder(itemView: View, payload: Any) :
    MessageHolders.IncomingImageMessageViewHolder<Message>(itemView, payload) {

    private val onlineIndicator: View

    init {
        onlineIndicator = itemView.findViewById(R.id.onlineIndicator)
    }

    override fun onBind(message: Message) {
        super.onBind(message)

        val isOnline = message.user?.isOnline
        if (isOnline != null && isOnline) {
            onlineIndicator.setBackgroundResource(R.drawable.shape_bubble_online)
        } else {
            onlineIndicator.setBackgroundResource(R.drawable.shape_bubble_offline)
        }
    }
}