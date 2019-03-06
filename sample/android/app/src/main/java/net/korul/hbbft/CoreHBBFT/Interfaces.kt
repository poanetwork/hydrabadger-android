package net.korul.hbbft.CoreHBBFT

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
