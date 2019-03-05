package net.korul.hbbft.CommonFragments.tabContacts

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.fragment_add_to_contact.*
import lib.folderpicker.FolderPicker
import net.korul.hbbft.CoreHBBFT.CoreHBBFT
import net.korul.hbbft.CoreHBBFT.UserWork.getUserFromLocalOrDownloadFromFirebase
import net.korul.hbbft.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

interface IAddToContacts {
    fun errorAddContact()
}


class AddToContactsFragment : Fragment() {

    private val FOLDERPICKER_CODE = 111

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_to_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_back.setOnClickListener {
            (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(CoreHBBFT.uniqueID1, BarcodeFormat.QR_CODE, 400, 400)
            my_qr_code_view.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        button_open_qr_scanner.setOnClickListener {
            val integrator = IntentIntegrator.forSupportFragment(this)
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(true)
            integrator.setOrientationLocked(false)
            integrator.initiateScan()
        }

        save_qr_code.setOnClickListener {
            verifyStoragePermissionsAndSaveQRCode(activity!!)
        }

        button_add_contact.setOnClickListener {
            if (contact_id_or_email.text.toString().isEmpty()) {
                contact_id_or_email.error = getString(R.string.contact_request_error_email_or_id)
            } else
                getUserFromLocalOrDownloadFromFirebase(contact_id_or_email.text.toString(), object :
                    IAddToContacts {
                    override fun errorAddContact() {
                        contact_id_or_email.error = getString(R.string.contact_request_error_email_or_id)
                    }
                })
        }
    }

    fun onSaveQRCode() {
        val intent = Intent(activity, FolderPicker::class.java)
        intent.putExtra("title", getString(R.string.select_dir_for_saving))
        startActivityForResult(intent, FOLDERPICKER_CODE)
    }


    fun verifyStoragePermissionsAndSaveQRCode(activity: Activity) {
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
            onSaveQRCode()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FOLDERPICKER_CODE && resultCode == Activity.RESULT_OK) {
            val folderLocation = data?.extras?.getString("data")

            if (folderLocation != null && folderLocation.isNotEmpty()) {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(CoreHBBFT.uniqueID1, BarcodeFormat.QR_CODE, 400, 400)
                try {
                    val filename = folderLocation + File.separator + getString(R.string.qr_code_file_name)
                    val file = File(filename)
                    if (file.exists())
                        file.delete()
                    val create = file.createNewFile()
                    if (!create) {
                        Toast.makeText(activity, getString(R.string.qr_code_disk_saved_fail), Toast.LENGTH_LONG).show()
                    } else {
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                    Toast.makeText(activity, getString(R.string.qr_code_disk_saved), Toast.LENGTH_LONG).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(activity, getString(R.string.qr_code_disk_saved_fail), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(activity, getString(R.string.canceled), Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == REQUEST_EXTERNAL_STORAGE && resultCode == Activity.RESULT_OK) {
            onSaveQRCode()
        } else {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents == null) {
                    Toast.makeText(activity, getString(R.string.canceled), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, "Scanned: " + result.contents, Toast.LENGTH_LONG).show()

                    getUserFromLocalOrDownloadFromFirebase(result.contents, object :
                        IAddToContacts {
                        override fun errorAddContact() {
                            contact_id_or_email.error = getString(R.string.contact_request_error_email_or_id)
                        }
                    })
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    companion object {
        fun newInstance() = AddToContactsFragment()

        private val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE =
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
    }
}
