package net.korul.hbbft.CommonFragments.tabSettings.RingtoneUtil

import android.content.DialogInterface
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import net.korul.hbbft.CommonFragments.tabSettings.RingtoneUtil.RingtonePickerDialog.OnRingtoneSelectedListener
import net.korul.hbbft.R
import java.util.*


/**
 *
 *
 * An alternative to the system's ringtone picker dialog. The differences are:
 * (1) this dialog matches the current theme,
 * (2) the selected ringtone URI is delivered via the [ OnRingtoneSelectedListener][OnRingtoneSelectedListener] callback.
 *
 *
 * TODO: If a ringtone was playing and the configuration changes, the ringtone is destroyed.
 * Restore the playing ringtone (seamlessly, without the stutter that comes from restarting).
 * Setting setRetainInstance(true) in onCreate() made our app crash (error said attempted to
 * access closed Cursor).
 * We might need to play the ringtone from a Service instead, so we won't have to worry about
 * the ringtone being destroyed on rotation.
 */
class RingtonePickerDialog : BaseAlertDialogFragment() {

    private var mRingtoneManager: RingtoneManager? = null
    private var mOnRingtoneSelectedListener: OnRingtoneSelectedListener? = null
    private var mRingtoneUri: Uri? = null
    private var mRingtone: RingtoneLoop? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null)
            mRingtoneUri = savedInstanceState.getParcelable(KEY_RINGTONE_URI)
        mRingtoneManager = RingtoneManager(activity)
        mRingtoneManager!!.setType(RingtoneManager.TYPE_ALL)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun createFrom(builder: AlertDialog.Builder): AlertDialog {
        // TODO: We set the READ_EXTERNAL_STORAGE permission. Verify that this includes the user's
        // custom ringtone files.
        val cursor = mRingtoneManager!!.cursor
        val checkedItem = mRingtoneManager!!.getRingtonePosition(mRingtoneUri)
        val labelColumn = cursor.getColumnName(RingtoneManager.TITLE_COLUMN_INDEX)

        builder.setTitle(R.string.ringtones)
            .setSingleChoiceItems(cursor, checkedItem, labelColumn) { dialog, which ->
                if (mRingtone != null) {
                    destroyLocalPlayer()
                }
                // Here, 'which' param refers to the position of the item clicked.
                mRingtoneUri = mRingtoneManager!!.getRingtoneUri(which)
                mRingtone = RingtoneLoop(Objects.requireNonNull<FragmentActivity>(activity), mRingtoneUri)
                mRingtone!!.play()
            }
        return super.createFrom(builder)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        destroyLocalPlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_RINGTONE_URI, mRingtoneUri)
    }

    override fun onOk() {
        if (mOnRingtoneSelectedListener != null) {
            // Here, 'which' param refers to the position of the item clicked.
            mOnRingtoneSelectedListener!!.onRingtoneSelected(mRingtoneUri)
        }
        dismiss()
    }

    fun setOnRingtoneSelectedListener(onRingtoneSelectedListener: OnRingtoneSelectedListener) {
        mOnRingtoneSelectedListener = onRingtoneSelectedListener
    }

    private fun destroyLocalPlayer() {
        if (mRingtone != null) {
            mRingtone!!.stop()
            mRingtone = null
        }
    }

    interface OnRingtoneSelectedListener {
        fun onRingtoneSelected(ringtoneUri: Uri?)
    }

    companion object {
        private const val TAG = "HYDRA:RingtonePickerDialog"
        private const val KEY_RINGTONE_URI = "key_ringtone_uri"

        /**
         * @param ringtoneUri the URI of the ringtone to isVisible as initially selected
         */
        fun newInstance(l: OnRingtoneSelectedListener, ringtoneUri: Uri): RingtonePickerDialog {
            val dialog = RingtonePickerDialog()
            dialog.mOnRingtoneSelectedListener = l
            dialog.mRingtoneUri = ringtoneUri
            return dialog
        }
    }
}