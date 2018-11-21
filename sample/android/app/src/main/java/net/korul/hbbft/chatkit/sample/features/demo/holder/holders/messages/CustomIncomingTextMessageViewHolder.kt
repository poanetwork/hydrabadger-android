package net.korul.hbbft.chatkit.sample.features.demo.holder.holders.messages

import android.view.View

import com.stfalcon.chatkit.messages.MessageHolders
import net.korul.hbbft.R
import net.korul.hbbft.chatkit.sample.common.data.model.Message

class CustomIncomingTextMessageViewHolder(itemView: View, payload: Any) :
    MessageHolders.IncomingTextMessageViewHolder<Message>(itemView, payload) {

    private val onlineIndicator: View

    init {
        onlineIndicator = itemView.findViewById(R.id.onlineIndicator)
    }

    override fun onBind(message: Message) {
        super.onBind(message)

        val isOnline = message.user.isOnline
        if (isOnline) {
            onlineIndicator.setBackgroundResource(R.drawable.shape_bubble_online)
        } else {
            onlineIndicator.setBackgroundResource(R.drawable.shape_bubble_offline)
        }

        //We can set click listener on view from payload
        val payload = this.payload as Payload
        userAvatar.setOnClickListener {
            if (payload != null && payload.avatarClickListener != null) {
                payload.avatarClickListener!!.onAvatarClick()
            }
        }
    }

    class Payload {
        var avatarClickListener: OnAvatarClickListener? = null
    }

    interface OnAvatarClickListener {
        fun onAvatarClick()
    }
}
