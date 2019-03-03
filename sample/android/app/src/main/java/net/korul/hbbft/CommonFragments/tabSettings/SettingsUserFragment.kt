package net.korul.hbbft.CommonFragments.tabSettings

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_settings_account.*
import net.korul.hbbft.DatabaseApplication.Companion.mCurUser
import net.korul.hbbft.R


class SettingsUserFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()
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
            (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }

        action_confirm.setOnClickListener {
            //TODO save

            (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }

        //TODO implement
        account_name.text = SpannableStringBuilder(mCurUser.name)
        account_nick.text = SpannableStringBuilder(mCurUser.nick)
        account_id.text = SpannableStringBuilder(mCurUser.uid)

        account_icon.setOnClickListener {
            //TODO implement
        }
    }

    companion object {
        fun newInstance() = SettingsUserFragment()
    }
}
