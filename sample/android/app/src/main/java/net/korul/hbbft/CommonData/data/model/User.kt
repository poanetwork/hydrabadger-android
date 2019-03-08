package net.korul.hbbft.CommonData.data.model

import com.stfalcon.chatkit.commons.models.IUser


class User(
    var id_: Long = 0,
    var uid: String,
    private var id: String,
    var idDialog: String,
    private val name: String,
    private val nick: String,
    private val avatar: String,
    val isOnline: Boolean,
    val isVisible: Boolean = true
) :
    IUser {

    override fun getId(): String {
        return id
    }

    fun setId(ids: String) {
        id = ids
    }

    override fun getName(): String {
        return name
    }

    override fun getNick(): String {
        return nick
    }

    override fun getAvatar(): String {
        return avatar
    }
}
