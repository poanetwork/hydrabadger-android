package net.korul.hbbft.CommonFragments.tabSettings

import android.app.Application
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_settings_security_lock.*
import net.korul.hbbft.CommonFragments.WarningFragment
import net.korul.hbbft.R


class SettingsSecurityLockFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_security_lock, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        action_back.setOnClickListener {
            (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }

        val prefs = context!!.getSharedPreferences("HYRABADGER", Application.MODE_PRIVATE)
        val lockScreen = prefs!!.getBoolean("lockScreen", false)
        val PasswordLockScreen = prefs.getString("PasswordLockScreen", "")

        switch_lock.isChecked = lockScreen
        if (lockScreen) {
            if (PasswordLockScreen != null && PasswordLockScreen.isNotEmpty())
                pin_lock_view.visibility = View.VISIBLE
            else
                pin_lock_view.visibility = View.GONE
        } else {
            security_lock_set_code.visibility = View.GONE
            pin_lock_view.visibility = View.GONE
        }

        switch_lock.setOnClickListener {
            if (switch_lock.isChecked) {
                security_lock_set_code.visibility = View.VISIBLE

                if (PasswordLockScreen != null && PasswordLockScreen.isNotEmpty())
                    pin_lock_view.visibility = View.VISIBLE

                val editor = prefs.edit()
                editor.putBoolean("lockScreen", true)
                editor.apply()
            } else {
                security_lock_set_code.visibility = View.GONE
                pin_lock_view.visibility = View.GONE

                val editor = prefs.edit()
                editor.putBoolean("lockScreen", false)
                editor.apply()
            }
        }

        security_lock_set_code.setOnClickListener {
            val transaction = (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.view,
                WarningFragment.newInstance(), getString(R.string.tag_settings)
            )
            transaction.addToBackStack(getString(R.string.tag_settings))
            transaction.commit()
        }
    }

    companion object {
        fun newInstance() = SettingsSecurityLockFragment()
    }
}
