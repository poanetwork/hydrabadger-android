package net.korul.hbbft.CommonFragments.tabSettings

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.fragment_settings_account.*
import lib.folderpicker.FolderPicker
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.utils.AppUtils
import net.korul.hbbft.CoreHBBFT.UserWork.saveCurUser
import net.korul.hbbft.CoreHBBFT.UserWork.updateAvatarInAllLocalUserByUid
import net.korul.hbbft.DatabaseApplication.Companion.mCurUser
import net.korul.hbbft.FirebaseStorageDU.MyUploadUserService
import net.korul.hbbft.ImageWork.ImageUtil.circleShape
import net.korul.hbbft.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread


class SettingsUserFragment : Fragment() {

    lateinit var progress: ProgressDialog
    val handle = Handler()
    var clickOnAvatar: Boolean = false

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
            if (somethingChanged()) {
                val builder = AlertDialog.Builder(activity!!)
                builder.setMessage(R.string.save_user_settings)
                    .setPositiveButton(R.string.action_ok) { _, _ ->
                        saveUser()
                        val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                        transaction.replace(
                            R.id.view,
                            SettingsFragment.newInstance(), getString(R.string.tag_settings)
                        )
                        transaction.addToBackStack(getString(R.string.tag_settings))
                        transaction.commit()
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
            val bitmap = barcodeEncoder.encodeBitmap(mCurUser.uid, BarcodeFormat.QR_CODE, 400, 400)
            qr_code_view.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        qr_code_view.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                SettingsQRFragment.newInstance(mCurUser.uid), getString(R.string.tag_settings)
            )
            transaction.addToBackStack(getString(R.string.tag_settings))
            transaction.commit()
        }

        action_confirm.setOnClickListener {
            if (somethingChanged())
                saveUser()
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.replace(
                R.id.view,
                SettingsFragment.newInstance(), getString(R.string.tag_settings)
            )
            transaction.addToBackStack(getString(R.string.tag_settings))
            transaction.commit()
        }

        account_name.text = SpannableStringBuilder(mCurUser.name)
        account_nick.text = SpannableStringBuilder(mCurUser.nick)
        account_id.text = SpannableStringBuilder(mCurUser.uid)

        account_icon.setOnClickListener {
            clickOnAvatar = true
            verifyStoragePermissionsAndPickFile(activity!!)
        }

        val pathAvatar = mCurUser.avatar
        if (pathAvatar != "") {
            val image = BitmapFactory.decodeFile(pathAvatar)
            account_icon.setImageBitmap(image)
        } else {
            account_icon.setImageResource(R.drawable.ic_contact)
        }

        save_qr_code.setOnClickListener {
            verifyStoragePermissionsAndSave(activity!!)
        }

        export_qr_code.setOnClickListener {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(mCurUser.uid, BarcodeFormat.QR_CODE, 400, 400)
            try {
                val outputDir = context!!.externalCacheDir
                val localFile = File.createTempFile(mCurUser.uid, "__.png", outputDir)
                if (localFile.exists())
                    localFile.delete()
                val create = localFile.createNewFile()
                if (!create) {
                    Toast.makeText(context!!, getString(R.string.qr_code_disk_saved_fail), Toast.LENGTH_LONG).show()
                } else {
                    FileOutputStream(localFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    sendFileByEmail(localFile.path)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context!!, getString(R.string.qr_code_disk_saved_fail), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendFileByEmail(filePath: String) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    val m = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
                    m.invoke(null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val file = File(filePath)

            val intent = Intent(Intent.ACTION_SEND)
            val extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString())
            val mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (extension.equals("", ignoreCase = true) || mimetype == null) {
                // if there is no extension or there is no definite mimetype, still try to open the file
                intent.type = "application/*"
            } else {
                intent.type = mimetype
            }
            intent.putExtra(
                android.content.Intent.EXTRA_SUBJECT,
                activity!!.getString(R.string.export_chooser_title)
            )
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.path))

            startActivity(Intent.createChooser(intent, activity!!.getString(R.string.export_chooser_title)))
        } catch (e: Exception) {
            e.printStackTrace()
            Crashlytics.logException(e)
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
        if (account_name.text.toString() != mCurUser.name)
            return true
        if (account_nick.text.toString() != mCurUser.nick)
            return true

        return false
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
        if (requestCode == FOLDERPICKER_CODE && resultCode == Activity.RESULT_OK) {
            val folderLocation = data?.extras?.getString("data")

            if (folderLocation != null && folderLocation.isNotEmpty()) {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(mCurUser.uid, BarcodeFormat.QR_CODE, 400, 400)
                try {
                    val filename = folderLocation + File.separator + mCurUser.uid
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
                    progress.show()
                    thread {
                        val fileLocation = data.extras.getString("data")

                        val outputDir = activity!!.filesDir
                        val localFile = File(outputDir.path + File.separator + mCurUser.uid + ".png")
                        if (localFile.exists())
                            localFile.delete()

                        val bitmap = BitmapFactory.decodeFile(fileLocation)
                        val resized = Bitmap.createScaledBitmap(bitmap, 250, 250, true)
                        val uImage = circleShape(resized)

                        val out = FileOutputStream(localFile)
                        uImage.compress(Bitmap.CompressFormat.PNG, 90, out) //100-best quality
                        out.close()

                        updateAvatarInAllLocalUserByUid(mCurUser.uid, localFile)

                        val uploadUri = Uri.fromFile(localFile)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context!!.startForegroundService(
                                Intent(context, MyUploadUserService::class.java)
                                    .putExtra(MyUploadUserService.EXTRA_FILE_URI, uploadUri)
                                    .putExtra(MyUploadUserService.EXTRA_USER_ID, mCurUser.uid)
                                    .setAction(MyUploadUserService.ACTION_UPLOAD)
                            )
                        } else {
                            context!!.startService(
                                Intent(context, MyUploadUserService::class.java)
                                    .putExtra(MyUploadUserService.EXTRA_FILE_URI, uploadUri)
                                    .putExtra(MyUploadUserService.EXTRA_USER_ID, mCurUser.uid)
                                    .setAction(MyUploadUserService.ACTION_UPLOAD)
                            )
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
            // We don't have permission so prompt the dialog
            requestPermissions(
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        } else {
            pickFile()
        }
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


        fun newInstance() = SettingsUserFragment()
    }
}
