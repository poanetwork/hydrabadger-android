package net.korul.hbbft.CommonFragments.tabChats

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
import kotlinx.android.synthetic.main.fragment_add_room.*
import lib.folderpicker.FolderPicker
import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.utils.AppUtils
import net.korul.hbbft.CoreHBBFT.CoreHBBFT
import net.korul.hbbft.CoreHBBFT.IAddToRooms
import net.korul.hbbft.CoreHBBFT.RoomDescrWork.getDialogFromFirebase
import net.korul.hbbft.Dialogs.DialogsFragment
import net.korul.hbbft.ImageWork.ImageUtil.blurRenderScript
import net.korul.hbbft.R


class AddDialogExistFragment : Fragment() {

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_room, container, false)
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

        dialog_id.hint = CoreHBBFT.uniqueID1 + "_" + CoreHBBFT.uniqueID2
        dialog_id.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()

                if (str.count() != 73)
                    button_add_dialog.visibility = View.GONE
                else {
                    if (str.filter { it == '-' }.count() != 8 && str.filter { it == '_' }.count() != 1)
                        button_add_dialog.visibility = View.GONE
                    else
                        button_add_dialog.visibility = View.VISIBLE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })


        button_add_dialog.setOnClickListener {
            if (dialog_id.text.toString().isEmpty()) {
                dialog_id_layout.error = getString(R.string.contact_request_error_id)
            } else {
                getDialogFromFirebase(dialog_id.text.toString(), object : IAddToRooms {
                    override fun dialog(dialog: Dialog) {
                        val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                        transaction.replace(
                            R.id.view,
                            DialogsFragment.newInstance(), getString(R.string.tag_chats)
                        )
                        transaction.addToBackStack(getString(R.string.tag_chats))
                        transaction.commit()
                    }

                    override fun errorAddRoom() {
                        dialog_id_layout.error = getString(R.string.contact_request_error_id)
                    }
                })
            }
        }

        button_add_dialog_qr.setOnClickListener {
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

                getDialogFromFirebase(text, object : IAddToRooms {
                    override fun dialog(dialog: Dialog) {
                        handler.post {
                            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                            transaction.replace(
                                R.id.view,
                                DialogsFragment.newInstance(), getString(R.string.tag_chats)
                            )
                            transaction.addToBackStack(getString(R.string.tag_chats))

                            transaction.commit()
                        }
                    }

                    override fun errorAddRoom() {
                        handler.post {
                            dialog_id_layout.error = getString(R.string.contact_request_error_id)
                        }
                    }
                })
            } else {
                handler.post {
                    dialog_id_layout.error = getString(R.string.contact_request_error_id)
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

                val generatedQRCode = bitmap
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
                    val barcodeEncoder = BarcodeEncoder()
                    val bitmap = barcodeEncoder.encodeBitmap(result.text, BarcodeFormat.QR_CODE, 400, 400)
                    my_qr_code_view.setImageBitmap(bitmap)
                    button_add_dialog_qr.visibility = View.VISIBLE

                    AppUtils.showToast(
                        activity!!,
                        getString(R.string.qr_code_file_selected), true
                    )
                } else {
                    AppUtils.showToast(
                        activity!!,
                        getString(R.string.qr_code_file_selected_wrongly), true
                    )

                    try {
                        val barcodeEncoder = BarcodeEncoder()
                        val bitmap = barcodeEncoder.encodeBitmap(CoreHBBFT.uniqueID1, BarcodeFormat.QR_CODE, 400, 400)
                        val blurred = blurRenderScript(context!!, bitmap, 25)
                        my_qr_code_view.setImageBitmap(blurred)
                        button_add_dialog_qr.visibility = View.GONE
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

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
                    button_add_dialog_qr.visibility = View.VISIBLE
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    companion object {
        fun newInstance() = AddDialogExistFragment()

        private val FILEPICKER_CODE = 111
        private val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE =
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
    }
}
