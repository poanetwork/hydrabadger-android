package net.korul.hbbft.CommonData.utils

import android.content.Context
import android.support.annotation.StringRes
import android.widget.Toast

/*
 * Created by troy379 on 04.04.17.
 */
object AppUtils {

    fun showToast(context: Context, @StringRes text: Int, isLong: Boolean) {
        showToast(context, context.getString(text), isLong)
    }

    fun showToast(context: Context, text: String, isLong: Boolean) {
        Toast.makeText(context, text, if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}