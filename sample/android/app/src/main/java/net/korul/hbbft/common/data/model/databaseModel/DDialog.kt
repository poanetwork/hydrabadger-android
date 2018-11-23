package net.korul.hbbft.common.data.model.databaseModel

import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.structure.BaseModel
import net.korul.hbbft.common.data.model.coreDataBase.AppDatabase


@Table(name = "DDialog", database = AppDatabase::class)
class DDialog: BaseModel()
{
    @PrimaryKey()
    @Column(name = "id")
    var id: String = ""

    @Column(name = "dialogName")
    var dialogName: String = ""

    @Column(name = "dialogPhoto")
    var dialogPhoto: String = ""

    @Column(name = "usersIDs")
    var usersIDs: usersIDsModelList = usersIDsModelList()

    var users: ArrayList<DUser> = arrayListOf()

    @Column(name = "lastMessageID")
    var lastMessageID: Long? = null

    var lastMessage: DMessage? = null

    @Column(name = "unreadCount")
    var unreadCount: Int = 0
}

