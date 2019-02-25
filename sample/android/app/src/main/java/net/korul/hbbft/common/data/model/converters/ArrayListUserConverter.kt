package net.korul.hbbft.common.data.model.converters

import com.google.gson.Gson
import com.raizlabs.android.dbflow.converter.TypeConverter
import net.korul.hbbft.common.data.model.databaseModel.usersIDsModelList
import com.raizlabs.android.dbflow.annotation.TypeConverter as TypeConverterAnnotation


@TypeConverterAnnotation
class ArrayListUserConverter : TypeConverter<String, usersIDsModelList>() {

    override fun getDBValue(model: usersIDsModelList): String? {
        return Gson().toJson(model)
    }

    override fun getModelValue(data: String?): usersIDsModelList {
        return Gson().fromJson(data, usersIDsModelList::class.java)
    }

}