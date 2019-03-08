package net.korul.hbbft.CommonFragments.tabContacts

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.fragment_add_to_contact.*
import lib.folderpicker.FolderPicker
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.utils.AppUtils
import net.korul.hbbft.CoreHBBFT.CoreHBBFT
import net.korul.hbbft.CoreHBBFT.IAddToContacts
import net.korul.hbbft.CoreHBBFT.UserWork.getUserFromLocalOrDownloadFromFirebase
import net.korul.hbbft.ImageWork.ImageUtil.blurRenderScript
import net.korul.hbbft.R


class AddToContactsFragment : Fragment() {

    private val FILEPICKER_CODE = 111
    private val handler = Handler()

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
            val blurred = blurRenderScript(context!!, bitmap, 25)
            my_qr_code_view.setImageBitmap(blurred)
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

        button_open_qr_gallary.setOnClickListener {
            verifyStoragePermissionsAndGetQRCode(activity!!)
        }

        contact_id.hint = CoreHBBFT.uniqueID1
        contact_id.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()

                if (str.count() != 36)
                    button_add_contact.visibility = View.GONE
                else {
                    if (str.filter { it == '-' }.count() != 4)
                        button_add_contact.visibility = View.GONE
                    else
                        button_add_contact.visibility = View.VISIBLE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })


        button_add_contact.setOnClickListener {
            if (contact_id.text.toString().isEmpty()) {
                contact_id_layout.error = getString(R.string.contact_request_error_id)
            } else
                getUserFromLocalOrDownloadFromFirebase(contact_id.text.toString(), object :
                    IAddToContacts {
                    override fun user(user: User) {
                        val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                        transaction.replace(
                            R.id.view,
                            ContactsFragment.newInstance(), getString(R.string.tag_contacts)
                        )
                        transaction.addToBackStack(getString(R.string.tag_contacts))
                        transaction.commit()
                    }

                    override fun errorAddContact() {
                        contact_id_layout.error = getString(R.string.contact_request_error_id)
                    }
                })
        }

        button_add_contact_qr.setOnClickListener {
            my_qr_code_view.invalidate()
            val drawable = my_qr_code_view.drawable as BitmapDrawable
            val generatedQRCode = drawable.bitmap


            val width = generatedQRCode.width
            val height = generatedQRCode.height
            val pixels = IntArray(width * height)
            generatedQRCode.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val reader = MultiFormatReader()
            var result: Result? = null
            try {
                result = reader.decode(binaryBitmap)
            } catch (e: NotFoundException) {
                e.printStackTrace()
            } catch (e: ChecksumException) {
                e.printStackTrace()
            } catch (e: FormatException) {
                e.printStackTrace()
            }

            if (result != null) {
                val text = result.text

                getUserFromLocalOrDownloadFromFirebase(text, object :
                    IAddToContacts {
                    override fun user(user: User) {
                        val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                        transaction.replace(
                            R.id.view,
                            ContactsFragment.newInstance(), getString(R.string.tag_contacts)
                        )
                        transaction.addToBackStack(getString(R.string.tag_contacts))
                        transaction.commit()
                    }

                    override fun errorAddContact() {
                        handler.post {
                            contact_id_layout.error = getString(R.string.contact_request_error_id)
                        }
                    }
                })
            } else {
                handler.post {
                    contact_id_layout.error = getString(R.string.contact_request_error_id)
                }
            }
        }
    }

    fun onGetQRCode() {
        val intent = Intent(activity, FolderPicker::class.java)
        intent.putExtra("title", getString(R.string.select_qr_image))
        intent.putExtra("pickFiles", true)
        startActivityForResult(intent, FILEPICKER_CODE)
    }


    fun verifyStoragePermissionsAndGetQRCode(activity: Activity) {
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
            onGetQRCode()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILEPICKER_CODE && resultCode == Activity.RESULT_OK) {
            val folderLocation = data?.extras?.getString("data")

            if (folderLocation != null && folderLocation.isNotEmpty()) {

                val fileLocation = data.extras.getString("data")
                val bitmap = BitmapFactory.decodeFile(fileLocation)

                my_qr_code_view.setImageBitmap(bitmap)
                button_add_contact_qr.visibility = View.VISIBLE

                AppUtils.showToast(
                    activity!!,
                    getString(R.string.qr_code_file_selected), true
                )
            } else {
                AppUtils.showToast(
                    activity!!,
                    getString(R.string.canceled), true
                )
            }
        } else if (requestCode == REQUEST_EXTERNAL_STORAGE && resultCode == Activity.RESULT_OK) {
            onGetQRCode()
        } else {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents == null) {
                    AppUtils.showToast(
                        activity!!,
                        getString(R.string.canceled), true
                    )
                } else {
                    AppUtils.showToast(
                        activity!!,
                        "Scanned: " + result.contents, false
                    )

                    try {
                        val barcodeEncoder = BarcodeEncoder()
                        val bitmap = barcodeEncoder.encodeBitmap(result.contents, BarcodeFormat.QR_CODE, 400, 400)
                        my_qr_code_view.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    button_add_contact_qr.visibility = View.VISIBLE
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
