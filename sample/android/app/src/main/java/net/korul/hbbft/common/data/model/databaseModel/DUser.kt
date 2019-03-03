package net.korul.hbbft.common.data.model.databaseModel

import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.structure.BaseModel
import net.korul.hbbft.common.data.model.coreDataBase.AppDatabase

@Table(name = "DUser", database = AppDatabase::class)
class DUser : BaseModel() {
    @PrimaryKey()
    @Column(name = "id")
    var id: Long = 0

    @Column(name = "uid")
    var uid: String = ""

    @Column(name = "idDialog")
    var idDialog: String = ""

    @Column(name = "name")
    var name: String = ""

    @Column(name = "nick")
    var nick: String = ""

    @Column(name = "avatar")
    var avatar: String = ""

    @Column(name = "isOnline")
    var isOnline: Boolean = false
}