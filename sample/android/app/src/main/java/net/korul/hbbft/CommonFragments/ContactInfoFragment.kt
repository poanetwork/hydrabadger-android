package net.korul.hbbft.CommonFragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_contact_info.*
import net.korul.hbbft.R
import net.korul.hbbft.common.data.model.User


class ContactInfoFragment : Fragment() {

    lateinit var curUser: User

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
        //TODO image set
//                Bitmap image = BitmapFactory.decodeFile(curUser.getAvatar());
//                contact_icon.setImageBitmap(image);

        contact_nickname.isEnabled = false
        edit_nickname.setOnClickListener {
            contact_nickname.isEnabled = true
            contact_nickname.inputType = InputType.TYPE_CLASS_TEXT
            contact_nickname.isFocusable = true

            contact_nickname.requestFocus()
            val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(contact_nickname, InputMethodManager.SHOW_IMPLICIT)
        }

        action_chat.setOnClickListener {
            //TODO start chat
        }

        action_back.setOnClickListener {
            (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }

        contact_icon.setOnClickListener {
            ShowBigAvatarActivity.open(context!!, curUser.avatar)
        }

        account_generate_qr.setOnClickListener {
            ShowBigQRActivity.open(context!!, curUser.uid)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
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
