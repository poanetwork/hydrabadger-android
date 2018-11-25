package net.korul.hbbft.common.data.model

import com.stfalcon.chatkit.commons.models.IUser

/*
 * Created by troy379 on 04.04.17.
 */
class User(
    var id_: Long = 0,
    var uid: String,
    private var id: String,
    var idDialog: String,
    private val name: String,
    private val avatar: String,
    val isOnline: Boolean) :
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

    override fun getAvatar(): String {
        return avatar
    }
}
