package net.korul.hbbft.common.data.model.conversation

import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.databaseModel.DDialog
import net.korul.hbbft.common.data.model.databaseModel.DMessage
import net.korul.hbbft.common.data.model.databaseModel.DUser

class Conversations
{
    companion object {
        fun getDialog(ddialog: DDialog): Dialog {
            val listUsers: MutableList<User> = arrayListOf()
            for (user in ddialog.users) {
                listUsers.add(getUser(user))
            }

            return Dialog(ddialog.id, ddialog.dialogName, ddialog.dialogPhoto, ArrayList(listUsers), getMessage(ddialog.lastMessage), ddialog.unreadCount)
        }

        fun getMessage(dmessage: DMessage): Message {
            return Message(dmessage.id, dmessage.idDialog, getUser(dmessage.user), dmessage.text, dmessage.createdAt)
        }

        fun getUser(duser: DUser): User {
            return User(duser.id, duser.idDialog, duser.name, duser.avatar, duser.isOnline)
        }
    }
}
