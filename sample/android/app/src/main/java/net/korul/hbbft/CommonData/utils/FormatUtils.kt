package net.korul.hbbft.CommonData.utils

import java.text.SimpleDateFormat
import java.util.*

/*
 * Created by troy379 on 06.04.17.
 */
class FormatUtils private constructor() {
    init {
        throw AssertionError()
    }

    companion object {
        fun getDurationString(seconds: Int): String {
            val date = Date((seconds * 1000).toLong())
            val formatter = SimpleDateFormat(if (seconds >= 3600) "HH:mm:ss" else "mm:ss")
            formatter.timeZone = TimeZone.getTimeZone("GMT")
            return formatter.format(date)
        }
    }
}
