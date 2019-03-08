package net.korul.hbbft.CommonFragments.tabSettings

import android.app.Application
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_settings_app_interface.*
import net.korul.hbbft.R


class DialogThemeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings_app_interface, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        theme_ok.setOnClickListener {
            if (healin_app_theme.isChecked) {
                val prefs = context!!.getSharedPreferences("HYRABADGER", Application.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.putString("Theme", "light")
                editor.apply()

                context!!.theme.applyStyle(R.style.App_Healin_Theme, true)
                activity!!.theme.applyStyle(R.style.App_Healin_Theme, true)
                activity!!.setTheme(R.style.App_Healin_Theme)
                context!!.setTheme(R.style.App_Healin_Theme)
            } else if (mib_app_theme.isChecked) {
                val prefs = context!!.getSharedPreferences("HYRABADGER", Application.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.putString("Theme", "night")
                editor.apply()

                context!!.theme.applyStyle(R.style.App_Black_Theme, true)
                activity!!.theme.applyStyle(R.style.App_Black_Theme, true)
                activity!!.setTheme(R.style.App_Black_Theme)
                context!!.setTheme(R.style.App_Black_Theme)
            }

            val prefs = context!!.getSharedPreferences("HYRABADGER", Application.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("Theme_NeedRestart", true)
            editor.apply()
            activity!!.recreate()
        }

        val prefs = context!!.getSharedPreferences("HYRABADGER", Application.MODE_PRIVATE)
        val theme = prefs!!.getString("Theme", "night")
        if (theme == "night") {
            mib_app_theme.isChecked = true
        } else if (theme == "light") {
            healin_app_theme.isChecked = true
        }


        action_back.setOnClickListener {
            (activity as AppCompatActivity).supportFragmentManager.popBackStack()
        }
    }


    companion object {
        fun newInstance() = DialogThemeFragment()
    }
}
