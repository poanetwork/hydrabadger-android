package net.korul.hbbft.common.data.model.converters

import com.raizlabs.android.dbflow.converter.TypeConverter
import net.korul.hbbft.common.data.model.Message
import com.raizlabs.android.dbflow.annotation.TypeConverter as TypeConverterAnnotation

@TypeConverterAnnotation
class ImageConverter : TypeConverter<String, Message.Image>() {

    override fun getDBValue(model: Message.Image): String? {
        return model.url
    }

    override fun getModelValue(data: String?): Message.Image {
        val date = Message.Image(data!!)
        return date
    }

}