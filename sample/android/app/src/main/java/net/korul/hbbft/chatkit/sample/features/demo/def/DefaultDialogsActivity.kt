package net.korul.hbbft.chatkit.sample.features.demo.def

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.stfalcon.chatkit.dialogs.DialogsList
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import com.stfalcon.chatkit.utils.DateFormatter
import net.korul.hbbft.R
import net.korul.hbbft.chatkit.sample.common.data.fixtures.DialogsFixtures
import net.korul.hbbft.chatkit.sample.common.data.model.Dialog
import net.korul.hbbft.chatkit.sample.common.data.model.Message
import net.korul.hbbft.chatkit.sample.features.demo.DemoDialogsActivity
import net.korul.hbbft.chatkit.sample.features.demo.holder.holders.dialogs.CustomDialogViewHolder
import java.util.*

class DefaultDialogsActivity : DemoDialogsActivity(), DateFormatter.Formatter {

    private var dialogsList: DialogsList? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_dialogs)

        dialogsList = findViewById<View>(R.id.dialogsList) as DialogsList
        initAdapter()
    }

    override fun onDialogClick(dialog: Dialog) {
        DefaultMessagesActivity.open(this)
    }

    override fun format(date: Date): String {
        return if (DateFormatter.isToday(date)) {
            DateFormatter.format(date, DateFormatter.Template.TIME)
        } else if (DateFormatter.isYesterday(date)) {
            getString(R.string.date_header_yesterday)
        } else if (DateFormatter.isCurrentYear(date)) {
            DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH)
        } else {
            DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH_YEAR)
        }
    }

    private fun initAdapter() {
        super.dialogsAdapter = DialogsListAdapter<Dialog>(
            R.layout.item_custom_dialog_view_holder,
            CustomDialogViewHolder::class.java,
            super.imageLoader
        )

        super.dialogsAdapter!!.setItems(DialogsFixtures.dialogs)

        super.dialogsAdapter!!.setOnDialogClickListener(this)
        super.dialogsAdapter!!.setOnDialogLongClickListener(this)
        super.dialogsAdapter!!.setDatesFormatter(this)

        dialogsList!!.setAdapter(super.dialogsAdapter)
    }

    //for example
    private fun onNewMessage(dialogId: String, message: Message) {
        val isUpdated = dialogsAdapter!!.updateDialogWithMessage(dialogId, message)
        if (!isUpdated) {
            //Dialog with this ID doesn't exist, so you can create new Dialog or update all dialogs list
        }
    }

    //for example
    private fun onNewDialog(dialog: Dialog) {
        dialogsAdapter!!.addItem(dialog)
    }

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, DefaultDialogsActivity::class.java))
        }
    }
}
