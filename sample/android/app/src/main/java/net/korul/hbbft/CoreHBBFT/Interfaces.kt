package net.korul.hbbft.CoreHBBFT

import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.data.model.User
import java.util.*

interface CoreHBBFTListener {
    fun updateStateToOnline()

    fun updateStateToError()

    fun reciveMessage(you: Boolean, uid: String, mes: String, mesID: Long, data: Date)

    fun reciveMessageWithDate(you: Boolean, uid: String, mes: String, mesID: Long, data: Date)

    fun setOnlineUser(uid: String, online: Boolean)
}

data class Uids(
    var UID: String? = null,
    var isOnline: Boolean? = null
)

data class Users(
    var UID: String? = null,
    var isOnline: Boolean? = null,
    var name: String? = null,
    var nick: String? = null
)

data class RoomDescr(
    var id: String? = null,
    var dialogName: String? = null,
    var dialogDescription: String? = null
)

interface IAddToContacts {
    fun errorAddContact()
    fun user(user: User)
}

interface IAddToRooms {
    fun errorAddRoom()
    fun dialog(dialog: Dialog)
}

interface IComplete {
    fun complete()
}