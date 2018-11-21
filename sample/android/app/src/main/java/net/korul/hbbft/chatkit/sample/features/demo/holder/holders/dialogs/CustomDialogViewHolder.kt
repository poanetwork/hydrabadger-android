package net.korul.hbbft.chatkit.sample.features.demo.holder.holders.dialogs

import android.view.View
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import net.korul.hbbft.R
import net.korul.hbbft.chatkit.sample.common.data.model.Dialog

/*
 * Created by Anton Bevza on 1/18/17.
 */
class CustomDialogViewHolder(itemView: View) : DialogsListAdapter.DialogViewHolder<Dialog>(itemView) {

    private val onlineIndicator: View

    init {
        onlineIndicator = itemView.findViewById(R.id.onlineIndicator)
    }

    override fun onBind(dialog: Dialog) {
        super.onBind(dialog)

        if (dialog.users.size > 1) {
            onlineIndicator.visibility = View.GONE
        } else {
            val isOnline = dialog.users[0].isOnline
            onlineIndicator.visibility = View.VISIBLE
            if (isOnline) {
                onlineIndicator.setBackgroundResource(R.drawable.shape_bubble_online)
            } else {
                onlineIndicator.setBackgroundResource(R.drawable.shape_bubble_offline)
            }
        }
    }
}
