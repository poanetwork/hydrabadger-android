package net.korul.hbbft.common.data.model.converters

import com.raizlabs.android.dbflow.converter.TypeConverter
import java.util.*
import com.raizlabs.android.dbflow.annotation.TypeConverter as TypeConverterAnnotation

@TypeConverterAnnotation
class DateConverter : TypeConverter<Long, Date>() {

    override fun getDBValue(model: Date): Long? {
        return model.time
    }

    override fun getModelValue(data: Long?): Date {
        val date = Date()
        date.time = data!!
        return date
    }

}