package net.korul.hbbft.chatkit.sample.features.demo.holder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.stfalcon.chatkit.dialogs.DialogsList
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import net.korul.hbbft.R
import net.korul.hbbft.chatkit.sample.common.data.fixtures.DialogsFixtures
import net.korul.hbbft.chatkit.sample.common.data.model.Dialog
import net.korul.hbbft.chatkit.sample.features.demo.DemoDialogsActivity
import net.korul.hbbft.chatkit.sample.features.demo.holder.holders.dialogs.CustomDialogViewHolder

class CustomHolderDialogsActivity : DemoDialogsActivity() {

    private var dialogsList: DialogsList? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_holder_dialogs)

        dialogsList = findViewById<View>(R.id.dialogsList) as DialogsList
        initAdapter()
    }

    override fun onDialogClick(dialog: Dialog) {
        CustomHolderMessagesActivity.open(this)
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

        dialogsList!!.setAdapter(super.dialogsAdapter)
    }

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, CustomHolderDialogsActivity::class.java))
        }
    }
}
