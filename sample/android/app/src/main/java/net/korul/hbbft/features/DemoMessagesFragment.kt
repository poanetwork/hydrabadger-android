package net.korul.hbbft.features

import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.squareup.picasso.Picasso
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.messages.MessagesListAdapter
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.R
import net.korul.hbbft.common.data.fixtures.MessagesFixtures
import net.korul.hbbft.common.data.fixtures.MessagesFixtures.Companion.deleteMeseges
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.core.Getters
import net.korul.hbbft.common.data.model.core.Getters.setLastMessage
import net.korul.hbbft.common.utils.AppUtils
import java.text.SimpleDateFormat
import java.util.*


//val handler = Handler()

abstract class DemoMessagesFragment : Fragment(),
    MessagesListAdapter.SelectionListener,
    MessagesListAdapter.OnLoadMoreListener {

    protected lateinit var imageLoader: ImageLoader
    protected var messagesAdapter: MessagesListAdapter<Message>? = null

    var menu: Menu? = null
    var selectionCount: Int = 0
    private var lastLoadedDate: Date? = null

    var mCurDialog: Dialog? = null
    var mCurUser: User? = null

    private val handler = Handler()

    lateinit var progress: ProgressDialog

    private val messageStringFormatter: MessagesListAdapter.Formatter<Message>
        get() = MessagesListAdapter.Formatter { message ->
            val createdAt = SimpleDateFormat("MMM d, EEE 'at' h:mm a", Locale.getDefault())
                .format(message.createdAt)

            var text = message.text
            if (text == null) text = "[attachment]"

            String.format(
                Locale.getDefault(), "%s: %s (%s)",
                message.user.name, text, createdAt
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        imageLoader = ImageLoader { imageView, url, payload ->
            try {
                Picasso.with(context).load(url).into(imageView)
            } catch (e: IllegalArgumentException) {
            }
        }

        progress = ProgressDialog(context!!)
        progress.setTitle("Connecting")
        progress.setMessage("Wait while connecting...")
        progress.setCancelable(false)
    }

    override fun onStart() {
        super.onStart()
        if (mCurDialog!!.lastMessage != null) {
            messagesAdapter!!.addToStart(mCurDialog!!.lastMessage, true)
        } else {
            loadMessages()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater!!.inflate(R.menu.chat_actions_menu, menu)

        if (isNeedVilibleMenuHbbft()) {
            menu!!.findItem(R.id.action_online).icon =
                DatabaseApplication.instance.resources.getDrawable(R.mipmap.ic_online_round)
            hideMenuHbbft()
        }

        this.menu = menu
        onSelectionChanged(0)
        super.onCreateOptionsMenu(menu, inflater)
    }

    fun hideMenuHbbft() {
        menu?.findItem(R.id.action_startALL)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                val sellmes = messagesAdapter!!.selectedMessages
                deleteMeseges(sellmes)
                setLastMessage(mCurDialog)
                mCurDialog = Getters.getDialog(mCurDialog!!.id)
                messagesAdapter!!.deleteSelectedMessages()
                messagesAdapter!!.notifyDataSetChanged()
            }
            R.id.action_copy -> {
                messagesAdapter!!.copySelectedMessagesText(context!!, messageStringFormatter, true)
                AppUtils.showToast(context!!, R.string.copied_message, true)
            }
            R.id.action_online -> {
//                DatabaseApplication.mCoreHBBFT2X.setOfflineModeInRoomInFirebase(DatabaseApplication.mCoreHBBFT2X.mRoomName)
            }
            R.id.action_startALL -> {
                Handler().post {
                    progress.show()
                }
                DatabaseApplication.mCoreHBBFT2X.subscribeSession()
                DatabaseApplication.mCoreHBBFT2X.afterSubscribeSession()

                if (DatabaseApplication.mCoreHBBFT2X.mShowError) {
                    val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        AlertDialog.Builder(context!!, android.R.style.Theme_Material_Dialog_Alert)
                    } else {
                        AlertDialog.Builder(context!!)
                    }
                    builder.setTitle("Error ")
                        .setMessage("Dll Error")
                        .setPositiveButton(android.R.string.yes) { dialog, _ ->
                            dialog.cancel()
                        }
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
                }

                DatabaseApplication.mCoreHBBFT2X.startAllNode(mCurDialog!!.dialogName)
            }
        }
        return false
    }


    override fun onLoadMore(page: Int, totalItemsCount: Int) {
        Log.i("TAG", "onLoadMore: $page $totalItemsCount")
        if (totalItemsCount < TOTAL_MESSAGES_COUNT) {
            loadMessages()
        }
    }

    override fun onSelectionChanged(count: Int) {
        this.selectionCount = count
        menu?.findItem(R.id.action_delete)?.isVisible = count > 0
        menu?.findItem(R.id.action_copy)?.isVisible = count > 0
        menu?.findItem(R.id.action_startALL)?.isVisible = count <= 0 && !isNeedVilibleMenuHbbft()
    }


    fun isNeedVilibleMenuHbbft(): Boolean {
        return (DatabaseApplication.mCoreHBBFT2X.mUpdateStateToOnline && mCurDialog!!.dialogName == DatabaseApplication.mCoreHBBFT2X.mRoomName)
    }

    private fun loadMessages() {
        handler.postDelayed({
            try {
                lastLoadedDate = if (messagesAdapter!!.allMessages.size > 0) {
                    val min = messagesAdapter!!.allMessages.minBy { it.createdAt!!.time }
                    min!!.createdAt
                } else {
                    Date()
                }

                val messages = MessagesFixtures.getMessages(lastLoadedDate, mCurDialog!!)
                messages.filterNotNull()
                if (messages.isNotEmpty()) {
                    lastLoadedDate = messages[messages.size - 1]?.createdAt
                    messagesAdapter!!.addToEnd(messages, false)
                }
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }, 500)
    }

    companion object {
        private val TOTAL_MESSAGES_COUNT = 100
    }
}
