package net.korul.hbbft.CoreHBBFT

import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.data.model.User

interface CoreHBBFTListener {
    fun updateStateToOnline()

    fun reciveMessage(you: Boolean, uid: String, mes: String)
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