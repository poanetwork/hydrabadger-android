package net.korul.hbbft.CommonFragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.fragment_contact_info_qr.*
import lib.folderpicker.FolderPicker
import net.korul.hbbft.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ShowBigQRActivity : AppCompatActivity() {

    private val FOLDERPICKER_CODE = 111

    lateinit var qr: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_contact_info_qr)

        qr = intent.getStringExtra("qr")
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(qr, BarcodeFormat.QR_CODE, 400, 400)
            qr_code_view.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }


        save_qr_code.setOnClickListener {
            verifyStoragePermissions(this)
            onSaveQRCode()
        }
    }

    fun onSaveQRCode() {
        val intent = Intent(this, FolderPicker::class.java)
        intent.putExtra("title", getString(R.string.select_dir_for_saving))
        startActivityForResult(intent, FOLDERPICKER_CODE)
    }


    fun verifyStoragePermissions(activity: Activity) {
        // Check if we have write permission
        val permission =
            ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FOLDERPICKER_CODE && resultCode == Activity.RESULT_OK) {
            val folderLocation = data?.extras?.getString("data")

            if (folderLocation != null && folderLocation.isNotEmpty()) {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(qr, BarcodeFormat.QR_CODE, 400, 400)
                try {
                    val filename = folderLocation + File.separator + qr
                    val file = File(filename)
                    if (file.exists())
                        file.delete()
                    val create = file.createNewFile()
                    if (!create) {
                        Toast.makeText(this, getString(R.string.qr_code_disk_saved_fail), Toast.LENGTH_LONG).show()
                    } else {
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                    Toast.makeText(this, getString(R.string.qr_code_disk_saved), Toast.LENGTH_LONG).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, getString(R.string.qr_code_disk_saved_fail), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.canceled), Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == REQUEST_EXTERNAL_STORAGE && resultCode == Activity.RESULT_OK) {
            onSaveQRCode()
        }
    }

    companion object {
        private val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE =
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

        fun open(context: Context, qr: String) {
            val intent = Intent(context, ShowBigQRActivity::class.java)
            intent.putExtra("qr", qr)
            context.startActivity(intent)
        }
    }

}