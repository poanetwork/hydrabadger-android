package net.korul.hbbft.Dialogs.holder

import android.view.View
import android.widget.TextView
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.utils.DateFormatter
import net.korul.hbbft.CommonData.data.model.Message
import net.korul.hbbft.CommonData.utils.FormatUtils
import net.korul.hbbft.R


class IncomingVoiceMessageViewHolder(itemView: View) :
    MessageHolders.IncomingTextMessageViewHolder<Message>(itemView) {

    private val tvDuration: TextView = itemView.findViewById<View>(R.id.duration) as TextView
    private val tvTime: TextView = itemView.findViewById<View>(R.id.time) as TextView

    override fun onBind(message: Message) {
        super.onBind(message)
        tvDuration.text = FormatUtils.getDurationString(
            message.voice!!.duration
        )
        tvTime.text = DateFormatter.format(message.createdAt, DateFormatter.Template.TIME)
    }
}
