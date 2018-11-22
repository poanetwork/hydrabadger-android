package net.korul.hbbft.features

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.squareup.picasso.Picasso
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.utils.AppUtils


abstract class DemoDialogsActivity : AppCompatActivity(), DialogsListAdapter.OnDialogClickListener<Dialog>,
    DialogsListAdapter.OnDialogLongClickListener<Dialog> {

    protected lateinit var imageLoader: ImageLoader
    protected var dialogsAdapter: DialogsListAdapter<Dialog>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageLoader = ImageLoader { imageView, url, payload ->
            Picasso.with(this@DemoDialogsActivity).load(url).into(imageView)
        }
    }

    override fun onDialogLongClick(dialog: Dialog) {
        AppUtils.showToast(
            this,
            dialog.dialogName,
            false
        )
    }
}
