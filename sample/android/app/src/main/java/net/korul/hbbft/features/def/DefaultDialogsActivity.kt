package net.korul.hbbft.features.def

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.stfalcon.chatkit.dialogs.DialogsList
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import com.stfalcon.chatkit.utils.DateFormatter
import kotlinx.android.synthetic.main.activity_default_dialogs.*
import net.korul.hbbft.R
import net.korul.hbbft.common.data.fixtures.DialogsFixtures
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.features.DemoDialogsActivity
import net.korul.hbbft.features.holder.holders.dialogs.CustomDialogViewHolder
import java.util.*

class DefaultDialogsActivity : DemoDialogsActivity(), DateFormatter.Formatter {

    private var dialogsList: DialogsList? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_dialogs)

        dialogsList = findViewById<View>(R.id.dialogsList) as DialogsList

        addDialog.setOnClickListener {
            onAddDialog()
        }
    }

    override fun onResume() {
        super.onResume()

        super.dialogsAdapter?.clear()
        initAdapter()
    }

    override fun onDialogClick(dialog: Dialog) {
        DefaultMessagesActivity.open(this, dialog, dialog.users[0])
    }

    override fun format(date: Date): String {
        return when {
            DateFormatter.isToday(date) -> DateFormatter.format(date, DateFormatter.Template.TIME)
            DateFormatter.isYesterday(date) -> getString(R.string.date_header_yesterday)
            DateFormatter.isCurrentYear(date) -> DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH)
            else -> DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH_YEAR)
        }
    }

    private fun initAdapter() {
        super.dialogsAdapter = DialogsListAdapter(
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

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, DefaultDialogsActivity::class.java))
        }
    }


    fun onAddDialog() {
        CreateNewDialog.open(this)
    }

    //for example
    fun onNewMessage(dialogId: String, message: Message) {
        val isUpdated = dialogsAdapter!!.updateDialogWithMessage(dialogId, message)
        if (!isUpdated) {
            //Dialog with this ID doesn't exist, so you can create new Dialog or update all dialogs list
        }
    }

    //for example
    fun onNewDialog(dialog: Dialog) {
        dialogsAdapter!!.addItem(dialog)
    }
}
