package net.korul.hbbft.features.holder

import android.view.View
import android.widget.TextView
import com.stfalcon.chatkit.messages.MessageHolders
import com.stfalcon.chatkit.utils.DateFormatter
import net.korul.hbbft.R
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.utils.FormatUtils

/*
 * Created by troy379 on 05.04.17.
 */
class OutcomingVoiceMessageViewHolder(itemView: View, payload: Any) :
    MessageHolders.OutcomingTextMessageViewHolder<Message>(itemView, payload) {

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
