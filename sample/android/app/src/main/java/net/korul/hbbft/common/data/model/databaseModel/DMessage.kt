package net.korul.hbbft.common.data.model.databaseModel

import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.structure.BaseModel
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.data.model.coreDataBase.AppDatabase
import java.util.*

@Table(name = "DMessage", database = AppDatabase::class)
class DMessage : BaseModel() {
    @PrimaryKey()
    @Column(name = "id")
    var id: Long = 0

    @Column(name = "idDialog")
    var idDialog: String = ""

    @Column(name = "userID")
    var userID: Long = 0

    var user: DUser = DUser()

    @Column(name = "text")
    var text: String? = ""

    @Column(name = "createdAt")
    var createdAt: Date? = Date()

    @Column(name = "image")
    var image: Message.Image? = null

    @Column(name = "voice")
    var voice: Message.Voice? = null

    @Column(name = "status")
    var status: String = "Sent"
}
