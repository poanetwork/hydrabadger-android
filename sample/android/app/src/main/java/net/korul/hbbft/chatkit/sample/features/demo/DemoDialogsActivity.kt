package net.korul.hbbft.chatkit.sample.features.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.squareup.picasso.Picasso
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import net.korul.hbbft.chatkit.sample.common.data.model.Dialog
import net.korul.hbbft.chatkit.sample.utils.AppUtils

/*
 * Created by troy379 on 05.04.17.
 */
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
