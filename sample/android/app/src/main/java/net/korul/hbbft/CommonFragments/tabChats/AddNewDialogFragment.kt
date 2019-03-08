package net.korul.hbbft.CommonFragments.tabChats

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_create_group.*
import lib.folderpicker.FolderPicker
import net.korul.hbbft.CommonData.data.fixtures.DialogsFixtures.Companion.setNewExtDialog
import net.korul.hbbft.CommonData.utils.AppUtils
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.FirebaseStorageDU.MyUploadRoomService
import net.korul.hbbft.ImageWork.ImageUtil.circleShape
import net.korul.hbbft.R
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.concurrent.thread


class AddNewDialogFragment : Fragment() {
    lateinit var progress: ProgressDialog
    val handle = Handler()
    var mFileLocation: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()

        progress = ProgressDialog(context!!)
        progress.setTitle("Working")
        progress.setMessage("Wait while work...")
        progress.setCancelable(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_group, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_back.setOnClickListener {
            (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }

        group_icon.setOnClickListener {
            verifyStoragePermissionsAndPickFile(activity!!)
        }

        button_add_create_group.setOnClickListener {
            if (group_name.text.toString().isEmpty())
                group_name.error = getString(R.string.name_dialog_isempty)
            else {
                group_name.error = ""
                addDialog()
            }
        }

        action_create.setOnClickListener {
            if (group_name.text.toString().isEmpty())
                group_name.error = getString(R.string.name_dialog_isempty)
            else {
                group_name.error = ""
                addDialog()
            }
        }
    }

    fun dismissProgressBar() {
        handle.post {
            progress.dismiss()
        }
    }

    fun addDialog() {
        progress.show()

        val dialogName = group_name.text.toString()
        val dialogDescription = group_description.text.toString()

        val dialogUID = UUID.randomUUID().toString() + "_" + UUID.randomUUID().toString()
        val outputDir = activity!!.filesDir
        val localFile = File.createTempFile(dialogUID, "png", outputDir)

        try {
            group_icon.invalidate()
            val drawable = group_icon.drawable as BitmapDrawable
            val bitmap = drawable.bitmap

            val out = FileOutputStream(localFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setNewExtDialog(dialogUID, dialogName, dialogDescription, localFile.path, DatabaseApplication.mCurUser)

        val uploadUri = Uri.fromFile(localFile)
        context!!.startService(
            Intent(context, MyUploadRoomService::class.java)
                .putExtra(MyUploadRoomService.EXTRA_ROOM_FILE_URI, uploadUri)
                .putExtra(MyUploadRoomService.EXTRA_ROOM_ID, dialogUID)
                .setAction(MyUploadRoomService.ACTION_UPLOAD)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_PICKER_CODE && resultCode == Activity.RESULT_OK) {
            val folderLocation = data?.extras?.getString("data")
            if (folderLocation != null && folderLocation.isNotEmpty()) {
                if (resultCode == Activity.RESULT_OK && data.hasExtra("data")) {
                    thread {
                        val fileLocation = data.extras.getString("data")
                        if (fileLocation != null) {
                            val bitmap = BitmapFactory.decodeFile(fileLocation)
                            val resized = Bitmap.createScaledBitmap(bitmap, 250, 250, true)
                            val uImage = circleShape(resized)

                            handle.post {
                                group_icon.setImageBitmap(uImage)
                            }

                            mFileLocation = fileLocation
                        }
                    }
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

    override fun onDetach() {
        super.onDetach()
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    companion object {
        private val FILE_PICKER_CODE = 3
        private val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE =
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

        fun newInstance() = AddNewDialogFragment()
    }
}
