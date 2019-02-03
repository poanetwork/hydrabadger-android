package net.korul.hbbft.features

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import com.squareup.picasso.Picasso
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import net.korul.hbbft.R
import net.korul.hbbft.common.data.fixtures.DialogsFixtures.Companion.deleteDialog
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.utils.AppUtils

abstract class DemoDialogsActivity :
    AppCompatActivity(),
    DialogsListAdapter.OnDialogClickListener<Dialog>,
    DialogsListAdapter.OnDialogLongClickListener<Dialog>,
    DialogInterface.OnClickListener
{

    protected lateinit var imageLoader: ImageLoader
    protected var dialogsAdapter: DialogsListAdapter<Dialog>? = null

    protected var mDialog: Dialog? = null
    var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageLoader = ImageLoader { imageView, url, payload ->
            try {
                Picasso.with(this@DemoDialogsActivity).load(url).into(imageView)
            }
            catch (e: IllegalArgumentException) {
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        this.menu = menu
        menuInflater.inflate(R.menu.chat_actions_menu, menu)
        menu.findItem(R.id.action_delete).isVisible = false
        menu.findItem(R.id.action_copy).isVisible = false
        menu.findItem(R.id.action_1x).isVisible = false
//        menu.findItem(R.id.clear).isVisible = false
        menu.findItem(R.id.action_2x).isVisible = false
        menu.findItem(R.id.action_3x).isVisible = false
        menu.findItem(R.id.action_online).isVisible = false
        return true
    }

    override fun onDialogLongClick(dialog: Dialog) {
        AppUtils.showToast(
            this,
            dialog.dialogName,
            false
        )

        mDialog = dialog

        AlertDialog.Builder(this)
            .setItems(R.array.view_do_dialog, this)
            .show()
    }

    override fun onClick(dialogInterface: DialogInterface, i: Int) {
        when (i) {
            0 -> {
                dialogsAdapter?.deleteById(mDialog!!.id)
                dialogsAdapter?.notifyDataSetChanged()
                deleteDialog(mDialog!!)
            }
            1 -> mDialog = null
        }
    }
}
