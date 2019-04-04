package net.korul.hbbft.Dialogs.holder.holders.messages

import android.util.Pair
import android.view.View
import android.widget.TextView

import com.stfalcon.chatkit.messages.MessageHolders
import net.korul.hbbft.CommonData.data.model.Message
import net.korul.hbbft.R



class CustomOutcomingImageMessageViewHolder(itemView: View) :
    MessageHolders.OutcomingImageMessageViewHolder<Message>(itemView) {

    private val messageTime: TextView = itemView.findViewById<View>(R.id.messageTime) as TextView

    override fun onBind(message: Message) {
        super.onBind(message)

        messageTime.text = message.status + " " + time.text
    }

    //Override this method to have ability to pass custom data in ImageLoader for loading image(not avatar).
    override fun getPayloadForImageLoader(message: Message): Any? {
        //For example you can pass size of placeholder before loading
        return Pair(100, 100)
    }

}