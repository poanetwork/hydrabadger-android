package net.korul.hbbft.CommonData.data.model.converters

import com.raizlabs.android.dbflow.converter.TypeConverter
import net.korul.hbbft.CommonData.data.model.Message
import com.raizlabs.android.dbflow.annotation.TypeConverter as TypeConverterAnnotation

@TypeConverterAnnotation
class ImageConverter : TypeConverter<String, Message.Image?>() {

    override fun getDBValue(model: Message.Image?): String? {
        return model?.url
    }

    override fun getModelValue(data: String?): Message.Image? {
        return if (data != null) {
            val date = Message.Image(data)
            date
        } else {
            null
        }
    }

}