package net.korul.hbbft.Dialogs.holder.holders.dialogs

import android.view.View
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CoreHBBFT.CoreHBBFT
import net.korul.hbbft.R


class CustomDialogViewHolder(itemView: View) : DialogsListAdapter.DialogViewHolder<Dialog>(itemView) {

    private val onlineIndicator: View = itemView.findViewById(R.id.onlineIndicator)

    override fun onBind(dialog: Dialog) {
        super.onBind(dialog)

        if (dialog.users.size > 1) {
            onlineIndicator.visibility = View.GONE
        } else {
            var user: User? = null
            for (us in dialog.users) {
                if (us.uid != CoreHBBFT.uniqueID1) {
                    user = us
                    break
                }
            }
            if (user == null)
                user = dialog.users[0]

            val isOnline = user.isOnline
            onlineIndicator.visibility = View.VISIBLE
            if (isOnline) {
                onlineIndicator.setBackgroundResource(R.drawable.shape_bubble_online)
            } else {
                onlineIndicator.setBackgroundResource(R.drawable.shape_bubble_offline)
            }
        }
    }
}
