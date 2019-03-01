package net.korul.hbbft.features

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import com.squareup.picasso.Picasso
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import net.korul.hbbft.R
import net.korul.hbbft.common.data.fixtures.DialogsFixtures.Companion.deleteDialog
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.utils.AppUtils


abstract class DemoDialogsFragment :
    Fragment(),
    DialogsListAdapter.OnDialogClickListener<Dialog>,
    DialogsListAdapter.OnDialogLongClickListener<Dialog>,
    DialogInterface.OnClickListener {

    protected lateinit var imageLoader: ImageLoader
    protected var dialogsAdapter: DialogsListAdapter<Dialog>? = null

    private var mDialog: Dialog? = null
    var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.show()

        imageLoader = ImageLoader { imageView, url, payload ->
            try {
                Picasso.with(context).load(url).into(imageView)
            } catch (e: IllegalArgumentException) {
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater!!.inflate(R.menu.dialogs_actions_menu, menu)

        menu!!.findItem(R.id.action_online).isVisible = false
        menu.findItem(R.id.action_add).isVisible = true

        this.menu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onDialogLongClick(dialog: Dialog) {
        AppUtils.showToast(
            context!!,
            dialog.dialogName,
            false
        )

        mDialog = dialog

        AlertDialog.Builder(context!!)
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
