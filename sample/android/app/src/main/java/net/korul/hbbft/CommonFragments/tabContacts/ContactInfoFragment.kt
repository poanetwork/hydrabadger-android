package net.korul.hbbft.CommonFragments.tabContacts

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_contact_info.*
import net.korul.hbbft.CommonFragments.ShowBigAvatarActivity
import net.korul.hbbft.CommonFragments.ShowBigQRActivity
import net.korul.hbbft.CommonFragments.WarningFragment
import net.korul.hbbft.CoreHBBFT.UserWork
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.R
import net.korul.hbbft.common.data.model.User


class ContactInfoFragment : Fragment() {

    lateinit var curUser: User
    var edited = false
    var lastNick = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()

        val bundle = arguments
        if (bundle != null && !bundle.isEmpty) {
            val extraUser = bundle.getString("user")
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

            if(edited && lastNick != contact_nickname.text.toString()) {
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
            else
                (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }

        contact_icon.setOnClickListener {
            ShowBigAvatarActivity.open(context!!, curUser.avatar)
        }

        account_generate_qr.setOnClickListener {
            ShowBigQRActivity.open(context!!, curUser.uid)
        }
    }

    fun saveUser() {
        val user = User(
            DatabaseApplication.mCurUser.id_,
            DatabaseApplication.mCurUser.uid,
            DatabaseApplication.mCurUser.id,
            DatabaseApplication.mCurUser.idDialog,
            DatabaseApplication.mCurUser.name,
            contact_nickname.text.toString(),
            DatabaseApplication.mCurUser.avatar,
            DatabaseApplication.mCurUser.isOnline
        )

        UserWork.saveCurUser(user)
    }
    companion object {
        fun newInstance(user: User): ContactInfoFragment {
            val f = ContactInfoFragment()
            val b = Bundle()
            b.putString("user", Gson().toJson(user))
            f.arguments = b
            return f
        }
    }
}
