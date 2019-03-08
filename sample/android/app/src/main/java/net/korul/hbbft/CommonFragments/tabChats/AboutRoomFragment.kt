package net.korul.hbbft.CommonFragments.tabChats

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.fragment_about_dialog.*
import lib.folderpicker.FolderPicker
import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.data.model.conversation.Conversations
import net.korul.hbbft.CommonData.utils.AppUtils
import net.korul.hbbft.CommonFragments.ShowBigQRActivity
import net.korul.hbbft.CoreHBBFT.IComplete
import net.korul.hbbft.CoreHBBFT.RoomDescrWork.reregisterInRoomDescrFirebase
import net.korul.hbbft.Dialogs.DialogsFragment
import net.korul.hbbft.FirebaseStorageDU.MyUploadRoomService
import net.korul.hbbft.ImageWork.ImageUtil.circleShape
import net.korul.hbbft.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class AboutRoomFragment : Fragment() {
    lateinit var progress: ProgressDialog
    val handle = Handler()
    var clickOnAvatar: Boolean = false

    private var mFile: File? = null
    private var mCurDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()

        val bundle = arguments
        if (bundle != null && !bundle.isEmpty) {
            val extraDialog = bundle.getString("dialog")
            mCurDialog = Gson().fromJson(extraDialog, Dialog::class.java)
        }

        progress = ProgressDialog(context!!)
        progress.setTitle("Working")
        progress.setMessage("Wait while work...")
        progress.setCancelable(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_back.setOnClickListener {
            if (somethingChanged()) {
                val builder = AlertDialog.Builder(activity!!)
                builder.setMessage(R.string.save_user_settings)
                    .setPositiveButton(R.string.action_ok) { _, _ ->
                        saveRoom()
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                        (activity as AppCompatActivity).supportFragmentManager.popBackStack()
                    }
                builder.create()
                builder.show()
            } else {
                (activity as AppCompatActivity).supportFragmentManager.popBackStack()
            }
        }

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(mCurDialog!!.id, BarcodeFormat.QR_CODE, 400, 400)
            qr_code_view.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        qr_code_view.setOnClickListener {
            ShowBigQRActivity.open(context!!, mCurDialog!!.id)
        }

        action_confirm.setOnClickListener {
            if (somethingChanged())
                saveRoom()
            else {
                val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                transaction.replace(
                    R.id.view,
                    DialogsFragment.newInstance(), getString(R.string.tag_chats)
                )
                transaction.addToBackStack(getString(R.string.tag_chats))
                transaction.commit()
            }
        }

        dialog_name.text = SpannableStringBuilder(mCurDialog!!.dialogName)
        dialog_descr.text = SpannableStringBuilder(mCurDialog!!.dialogDescr)
        dialog_id.text = SpannableStringBuilder(mCurDialog!!.id)

        room_icon.setOnClickListener {
            clickOnAvatar = true
            verifyStoragePermissionsAndPickFile(activity!!)
        }

        val pathAvatar = mCurDialog!!.dialogPhoto
        if (pathAvatar != "") {
            val image = BitmapFactory.decodeFile(pathAvatar)
            room_icon.setImageBitmap(image)
        } else {
            room_icon.setImageResource(R.drawable.ic_nophoto)
        }

        save_qr_code.setOnClickListener {
            verifyStoragePermissionsAndSave(activity!!)
        }
    }

    fun verifyStoragePermissionsAndSave(activity: Activity) {
        // Check if we have write permission
        val permission =
            ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the dialog
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE2
            )
        } else {
            onSaveQRCode()
        }
    }

    fun onSaveQRCode() {
        val intent = Intent(context, FolderPicker::class.java)
        intent.putExtra("title", getString(R.string.select_dir_for_saving))
        startActivityForResult(intent, FOLDERPICKER_CODE)
    }

    fun somethingChanged(): Boolean {
        if (clickOnAvatar)
            return true
        if (dialog_name.text.toString() != mCurDialog!!.dialogName)
            return true
        if (dialog_descr.text.toString() != mCurDialog!!.dialogDescr)
            return true

        return false
    }

    fun setImageAvatar(file: File) {
        val pathAvatar = file.path
        if (pathAvatar != "") {
            val image = BitmapFactory.decodeFile(pathAvatar)
            room_icon.setImageBitmap(image)
        } else {
            room_icon.setImageResource(R.drawable.ic_nophoto)
        }
    }

    fun pickFile() {
        val intent = Intent(context!!, FolderPicker::class.java)

        //Optional
        intent.putExtra("title", "Select file to upload")
        intent.putExtra(
            "location",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
        )
        intent.putExtra("pickFiles", true)
        //Optional

        startActivityForResult(intent, FILE_PICKER_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FOLDERPICKER_CODE && resultCode == Activity.RESULT_OK) {
            val folderLocation = data?.extras?.getString("data")

            if (folderLocation != null && folderLocation.isNotEmpty()) {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(mCurDialog!!.id, BarcodeFormat.QR_CODE, 400, 400)
                try {
                    val filename = folderLocation + File.separator + mCurDialog!!.id
                    val file = File(filename)
                    if (file.exists())
                        file.delete()
                    val create = file.createNewFile()
                    if (!create) {
                        Toast.makeText(context!!, getString(R.string.qr_code_disk_saved_fail), Toast.LENGTH_LONG).show()
                    } else {
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                    Toast.makeText(context!!, getString(R.string.qr_code_disk_saved), Toast.LENGTH_LONG).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(context!!, getString(R.string.qr_code_disk_saved_fail), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context!!, getString(R.string.canceled), Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == REQUEST_EXTERNAL_STORAGE2 && resultCode == Activity.RESULT_OK) {
            onSaveQRCode()
        }

        if (requestCode == FILE_PICKER_CODE && resultCode == Activity.RESULT_OK) {
            val folderLocation = data?.extras?.getString("data")
            if (folderLocation != null && folderLocation.isNotEmpty()) {
                if (resultCode == Activity.RESULT_OK && data.hasExtra("data")) {
                    val fileLocation = data.extras.getString("data")

                    val outputDir = activity!!.filesDir
                    val localFile = File.createTempFile(mCurDialog!!.id, "png", outputDir)

                    val bitmap = BitmapFactory.decodeFile(fileLocation)
                    val resized = Bitmap.createScaledBitmap(bitmap, 250, 250, true)
                    val uImage = circleShape(resized)

                    val out = FileOutputStream(localFile)
                    uImage.compress(Bitmap.CompressFormat.PNG, 90, out) //100-best quality
                    out.close()

                    mFile = localFile

                    setImageAvatar(localFile)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    AppUtils.showToast(
                        activity!!,
                        getString(R.string.file_error), true
                    )
                }
            } else {
                AppUtils.showToast(
                    activity!!,
                    getString(R.string.canceled), true
                )
            }
        } else if (requestCode == REQUEST_EXTERNAL_STORAGE && resultCode == Activity.RESULT_OK) {
            pickFile()
        }
    }


    fun dismissProgressBar() {
        handle.post {
            progress.dismiss()
        }
    }

    fun saveRoom() {
        if (mFile != null) {
            handle.post {
                progress.show()
            }

            val dialog = Dialog(
                mCurDialog!!.id,
                dialog_name.text.toString(),
                dialog_descr.text.toString(),
                mFile!!.path,
                mCurDialog!!.users,
                mCurDialog!!.lastMessage,
                mCurDialog!!.unreadCount
            )

            Conversations.getDDialog(dialog).update()

            reregisterInRoomDescrFirebase(dialog, object : IComplete {
                override fun complete() {
                }
            })

            val uploadUri = Uri.fromFile(mFile)
            context!!.startService(
                Intent(context, MyUploadRoomService::class.java)
                    .putExtra(MyUploadRoomService.EXTRA_ROOM_FILE_URI, uploadUri)
                    .putExtra(MyUploadRoomService.EXTRA_ROOM_ID, dialog.id)
                    .setAction(MyUploadRoomService.ACTION_UPLOAD)
            )
        } else {
            handle.post {
                progress.show()
            }

            val dialog = Dialog(
                mCurDialog!!.id,
                dialog_name.text.toString(),
                dialog_descr.text.toString(),
                mCurDialog!!.dialogPhoto,
                mCurDialog!!.users,
                mCurDialog!!.lastMessage,
                mCurDialog!!.unreadCount
            )

            Conversations.getDDialog(dialog).update()

            reregisterInRoomDescrFirebase(dialog, object : IComplete {
                override fun complete() {
                    dismissProgressBar()
                    val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                    transaction.replace(
                        R.id.view,
                        DialogsFragment.newInstance(), getString(R.string.tag_chats)
                    )
                    transaction.addToBackStack(getString(R.string.tag_chats))
                    transaction.commit()
                }
            })
        }
    }

    fun verifyStoragePermissionsAndPickFile(activity: Activity) {
        // Check if we have write permission
        val permission =
            ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the dialog
            requestPermissions(
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        } else {
            pickFile()
        }
    }

    override fun onDetach() {
        super.onDetach()
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    companion object {
        private val FOLDERPICKER_CODE = 111

        private val FILE_PICKER_CODE = 3
        private val REQUEST_EXTERNAL_STORAGE = 1
        private val REQUEST_EXTERNAL_STORAGE2 = 5
        private val PERMISSIONS_STORAGE =
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )


        fun newInstance(dialog: Dialog): AboutRoomFragment {
            val f = AboutRoomFragment()
            val b = Bundle()
            b.putString("dialog", Gson().toJson(dialog))
            f.arguments = b

            return f
        }
    }
}
