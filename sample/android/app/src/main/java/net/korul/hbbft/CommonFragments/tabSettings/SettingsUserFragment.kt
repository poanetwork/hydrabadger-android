package net.korul.hbbft.CommonFragments.tabSettings

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
import kotlinx.android.synthetic.main.fragment_settings_account.*
import lib.folderpicker.FolderPicker
import net.korul.hbbft.CoreHBBFT.UserWork.saveCurUser
import net.korul.hbbft.CoreHBBFT.UserWork.updateAvatarInAllLocalUserByUid
import net.korul.hbbft.DatabaseApplication.Companion.mCurUser
import net.korul.hbbft.R
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.firebaseStorage.MyUploadService
import net.korul.hbbft.imageWork.ImageUtil.circleShape
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread


class SettingsUserFragment : Fragment() {

    lateinit var progress: ProgressDialog
    val handle = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()

        progress = ProgressDialog(context!!)
        progress.setTitle("Working")
        progress.setMessage("Wait while work...")
        progress.setCancelable(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        account_generate_qr.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                SettingsQRFragment.newInstance(mCurUser.uid), getString(R.string.tag_settings)
            )
            transaction.addToBackStack(getString(R.string.tag_settings))
            transaction.commit()
        }

        action_back.setOnClickListener {
            val builder = AlertDialog.Builder(activity!!)
            builder.setMessage(R.string.save_user_settings)
                .setPositiveButton(R.string.action_ok) { _, _ ->
                    saveUser()
                    (activity as AppCompatActivity).supportFragmentManager.popBackStack()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    (activity as AppCompatActivity).supportFragmentManager.popBackStack()
                }
            builder.create()
            builder.show()
        }

        action_confirm.setOnClickListener {
            saveUser()
            (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }

        account_name.text = SpannableStringBuilder(mCurUser.name)
        account_nick.text = SpannableStringBuilder(mCurUser.nick)
        account_id.text = SpannableStringBuilder(mCurUser.uid)

        account_icon.setOnClickListener {
            verifyStoragePermissionsAndPickFile(activity!!)
        }


        val pathAvatar = mCurUser.avatar
        if (pathAvatar != "") {
            val image = BitmapFactory.decodeFile(pathAvatar)
            account_icon.setImageBitmap(image)
        } else {
            account_icon.setImageResource(R.drawable.ic_contact)
        }
    }

    fun setImageAvatar(file: File) {
        val pathAvatar = file.path
        if (pathAvatar != "") {
            val image = BitmapFactory.decodeFile(pathAvatar)
            account_icon.setImageBitmap(image)
        } else {
            account_icon.setImageResource(R.drawable.ic_contact)
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
        if (requestCode == FILE_PICKER_CODE && resultCode == Activity.RESULT_OK) {
            val folderLocation = data?.extras?.getString("data")
            if (folderLocation != null && folderLocation.isNotEmpty()) {
                if (resultCode == Activity.RESULT_OK && data.hasExtra("data")) {
                    progress.show()
                    thread {
                        val fileLocation = data.extras.getString("data")

                        val outputDir = activity!!.filesDir
                        val localFile = File.createTempFile(mCurUser.uid, "png", outputDir)

                        val bitmap = BitmapFactory.decodeFile(fileLocation)
                        val resized = Bitmap.createScaledBitmap(bitmap, 250, 250, true)
                        val uImage = circleShape(resized)

                        val out = FileOutputStream(localFile)
                        uImage.compress(Bitmap.CompressFormat.PNG, 90, out) //100-best quality
                        out.close()

                        updateAvatarInAllLocalUserByUid(mCurUser.uid, localFile)

                        val uploadUri = Uri.fromFile(localFile)
                        context!!.startService(
                            Intent(context, MyUploadService::class.java)
                                .putExtra(MyUploadService.EXTRA_FILE_URI, uploadUri)
                                .putExtra(MyUploadService.EXTRA_USER_ID, mCurUser.uid)
                                .setAction(MyUploadService.ACTION_UPLOAD)
                        )
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(context!!, getString(R.string.file_error), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, getString(R.string.canceled), Toast.LENGTH_LONG).show()
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

    fun saveUser() {
        val user = User(
            mCurUser.id_,
            mCurUser.uid,
            mCurUser.id,
            mCurUser.idDialog,
            account_name.text.toString(),
            account_nick.text.toString(),
            mCurUser.avatar,
            mCurUser.isOnline
        )

        saveCurUser(user)
    }

    fun verifyStoragePermissionsAndPickFile(activity: Activity) {
        // Check if we have write permission
        val permission =
            ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            requestPermissions(
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
        else {
            pickFile()
        }
    }

    companion object {
        private val FILE_PICKER_CODE = 3
        private val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE =
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )


        fun newInstance() = SettingsUserFragment()
    }
}
