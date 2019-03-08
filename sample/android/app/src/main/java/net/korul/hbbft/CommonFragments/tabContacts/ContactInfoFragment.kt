package net.korul.hbbft.CommonFragments.tabContacts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.fragment_contact_info.*
import lib.folderpicker.FolderPicker
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonFragments.ShowBigAvatarActivity
import net.korul.hbbft.CommonFragments.ShowBigQRActivity
import net.korul.hbbft.CommonFragments.WarningFragment
import net.korul.hbbft.CoreHBBFT.UserWork
import net.korul.hbbft.CoreHBBFT.Users
import net.korul.hbbft.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ContactInfoFragment : Fragment() {

    lateinit var curUser: User
    var edited = false
    var lastNick = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()

        val bundle = arguments
        if (bundle != null && !bundle.isEmpty) {
            val extraUser = bundle.getString("dialog")
            curUser = Gson().fromJson(extraUser, User::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contact_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contact_info_name.text = SpannableStringBuilder(curUser.name)
        contact_info_uid.text = SpannableStringBuilder(curUser.uid)

        if (curUser.avatar != "") {
            val image = BitmapFactory.decodeFile(curUser.avatar)
            contact_icon.setImageBitmap(image)
        } else {
            contact_icon.setImageResource(R.drawable.ic_contact)
        }

        contact_nickname.text = SpannableStringBuilder(curUser.nick)
        contact_nickname.isEnabled = false

        edit_nickname.setOnClickListener {
            contact_nickname.isEnabled = true
            contact_nickname.inputType = InputType.TYPE_CLASS_TEXT
            contact_nickname.isFocusable = true

            contact_nickname.requestFocus()
            val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(contact_nickname, InputMethodManager.SHOW_IMPLICIT)

            edited = true
            lastNick = contact_nickname.text.toString()
        }

        action_chat.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                WarningFragment.newInstance(), getString(R.string.tag_contacts)
            )
            transaction.addToBackStack(getString(R.string.tag_contacts))
            transaction.commit()
        }

        action_back.setOnClickListener {
            if (edited && lastNick != contact_nickname.text.toString()) {
                val builder = AlertDialog.Builder(activity!!)
                builder.setMessage(R.string.save_user_settings)
                    .setPositiveButton(R.string.action_ok) { _, _ ->
                        saveUser()
                        val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                        transaction.replace(
                            R.id.view,
                            ContactsFragment.newInstance(), getString(R.string.tag_contacts)
                        )
                        transaction.addToBackStack(getString(R.string.tag_contacts))
                        transaction.commit()
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                        (activity as AppCompatActivity).supportFragmentManager.popBackStack()
                    }
                builder.create()
                builder.show()
            }
            else
                (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }

        contact_icon.setOnClickListener {
            ShowBigAvatarActivity.open(context!!, curUser.avatar)
        }

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(curUser.uid, BarcodeFormat.QR_CODE, 400, 400)
            qr_code_view.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        save_qr_code.setOnClickListener {
            verifyStoragePermissionsAndSave(activity!!)
        }

        qr_code_view.setOnClickListener {
            ShowBigQRActivity.open(context!!, curUser.uid)
        }

        account_generate_qr.setOnClickListener {
            ShowBigQRActivity.open(context!!, curUser.uid)
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
                REQUEST_EXTERNAL_STORAGE
            )
        } else {
            onSaveQRCode()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FOLDERPICKER_CODE && resultCode == Activity.RESULT_OK) {
            val folderLocation = data?.extras?.getString("data")

            if (folderLocation != null && folderLocation.isNotEmpty()) {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(curUser.uid, BarcodeFormat.QR_CODE, 400, 400)
                try {
                    val filename = folderLocation + File.separator + curUser.uid
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
        } else if (requestCode == REQUEST_EXTERNAL_STORAGE && resultCode == Activity.RESULT_OK) {
            onSaveQRCode()
        }
    }

    fun onSaveQRCode() {
        val intent = Intent(context, FolderPicker::class.java)
        intent.putExtra("title", getString(R.string.select_dir_for_saving))
        startActivityForResult(intent, FOLDERPICKER_CODE)
    }

    fun saveUser() {
        val user = Users()
        user.UID = curUser.uid
        user.name = curUser.name
        user.nick = contact_nickname.text.toString()
        user.isOnline = curUser.isOnline

        UserWork.updateMetaInAllLocalUserByUid(user)
    }

    companion object {
        private val FOLDERPICKER_CODE = 111
        private val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE =
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

        fun newInstance(user: User): ContactInfoFragment {
            val f = ContactInfoFragment()
            val b = Bundle()
            b.putString("dialog", Gson().toJson(user))
            f.arguments = b
            return f
        }
    }
}
