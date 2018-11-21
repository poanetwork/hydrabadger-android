package net.korul.hbbft.chatkit.sample.features.demo.holder

import android.view.View
import android.widget.TextView
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.utils.DateFormatter
import net.korul.hbbft.R
import net.korul.hbbft.chatkit.sample.common.data.model.Message
import net.korul.hbbft.chatkit.sample.utils.FormatUtils

/*
 * Created by troy379 on 05.04.17.
 */
class IncomingVoiceMessageViewHolder(itemView: View, payload: Any) :
    MessageHolders.IncomingTextMessageViewHolder<Message>(itemView, payload) {

    private val tvDuration: TextView
    private val tvTime: TextView

    init {
        tvDuration = itemView.findViewById<View>(R.id.duration) as TextView
        tvTime = itemView.findViewById<View>(R.id.time) as TextView
    }

    override fun onBind(message: Message) {
        super.onBind(message)
        tvDuration.text = FormatUtils.getDurationString(
            message.voice!!.duration
        )
        tvTime.text = DateFormatter.format(message.createdAt, DateFormatter.Template.TIME)
    }
}
