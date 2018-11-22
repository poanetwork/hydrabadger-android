package net.korul.hbbft.common.data.model.converters

import com.raizlabs.android.dbflow.converter.TypeConverter
import net.korul.hbbft.common.data.model.Message
import com.raizlabs.android.dbflow.annotation.TypeConverter as TypeConverterAnnotation

@TypeConverterAnnotation
class VoiceConverter : TypeConverter<String, Message.Voice>() {

    companion object {
        private const val SEPARATE_SYMBOL = " ; "
    }

    override fun getDBValue(model: Message.Voice): String? {
        val str: String = model.url + SEPARATE_SYMBOL + model.duration.toString()
        return str
    }

    override fun getModelValue(data: String?): Message.Voice {
        val list = data?.split(SEPARATE_SYMBOL) ?: listOf()
        val date = Message.Voice(list[0], list[1].toInt())
        return date
    }

}