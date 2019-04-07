package net.korul.hbbft.CommonData.data.model.coreDataBase

import com.raizlabs.android.dbflow.annotation.Database

@Database(name = AppDatabase.NAME, version = AppDatabase.VERSION, generatedClassSeparator = "_")
object AppDatabase {
    const val NAME: String = "hbbft"
    const val VERSION: Int = 8
}
