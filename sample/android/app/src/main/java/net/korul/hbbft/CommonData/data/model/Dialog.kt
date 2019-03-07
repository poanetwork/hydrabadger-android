package net.korul.hbbft.CommonData.data.model

import com.stfalcon.chatkit.commons.models.IDialog
import java.util.*

/*
 * Created by troy379 on 04.04.17.
 */
class Dialog(
    private val id: String,
    private val dialogName: String,
    private val dialogDescr: String,
    private val dialogPhoto: String,
    private val users: ArrayList<User>, private var lastMessage: Message?, private var unreadCount: Int
) : IDialog<Message> {

    override fun getId(): String {
        return id
    }

    override fun getDialogPhoto(): String {
        return dialogPhoto
    }

    override fun getDialogName(): String {
        return dialogName
    }

    override fun getUsers(): ArrayList<User> {
        return users
    }

    override fun getLastMessage(): Message? {
        return lastMessage
    }

    override fun setLastMessage(lastMessage: Message) {
        this.lastMessage = lastMessage
    }

    override fun getUnreadCount(): Int {
        return unreadCount
    }

    override fun getDialogDescr(): String {
        return dialogDescr
    }

    fun setUnreadCount(unreadCount: Int) {
        this.unreadCount = unreadCount
    }
}
