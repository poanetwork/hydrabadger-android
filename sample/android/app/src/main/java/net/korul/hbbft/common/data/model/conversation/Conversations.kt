package net.korul.hbbft.common.data.model.conversation

import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.databaseModel.DDialog
import net.korul.hbbft.common.data.model.databaseModel.DMessage
import net.korul.hbbft.common.data.model.databaseModel.DUser
import net.korul.hbbft.common.data.model.databaseModel.usersIDsModelList

class Conversations {
    companion object {
        fun getDialog(ddialog: DDialog): Dialog {
            val listUsers: MutableList<User> = arrayListOf()
            for (user in ddialog.users) {
                listUsers.add(getUser(user))
            }

            return Dialog(
                ddialog.id,
                ddialog.dialogName,
                ddialog.dialogPhoto,
                ArrayList(listUsers),
                getMessage(ddialog.lastMessage),
                ddialog.unreadCount
            )
        }

        fun getDDialog(dialog: Dialog): DDialog {
            val ddialog = DDialog()
            ddialog.id = dialog.id
            ddialog.dialogName = dialog.dialogName
            ddialog.dialogPhoto = dialog.dialogPhoto

            val listDUsers: MutableList<DUser> = arrayListOf()
            for (user in dialog.users) {
                listDUsers.add(getDUser(user))
            }
            ddialog.users = ArrayList(listDUsers)

            val listIDUsers: MutableList<Long> = arrayListOf()
            for (user in dialog.users) {
                listIDUsers.add(user.id_)
            }
            ddialog.usersIDs = usersIDsModelList(ArrayList(listIDUsers))

            ddialog.lastMessageID = dialog.lastMessage?.id_
            ddialog.lastMessage = getDMessage(dialog.lastMessage)

            ddialog.unreadCount = dialog.unreadCount

            return ddialog
        }

        fun getMessage(dmessage: DMessage?): Message? {
            return if (dmessage == null)
                null
            else {
                val mes = Message(
                    dmessage.id,
                    dmessage.id.toString(),
                    dmessage.idDialog,
                    getUser(dmessage.user),
                    dmessage.text,
                    dmessage.createdAt
                )
                mes.voice = dmessage.voice
                if (dmessage.image != null)
                    mes.setImage(dmessage.image!!)
                mes
            }
        }

        fun getDMessage(message: Message?): DMessage? {
            return if (message == null)
                null
            else {
                val dmessage = DMessage()
                dmessage.id = message.id_
                dmessage.idDialog = message.idDialog
                dmessage.userID = message.user.id_
                dmessage.user = getDUser(message.user)
                dmessage.text = message.text
                dmessage.createdAt = message.createdAt
                dmessage.image = message.getImage()
                dmessage.voice = message.voice
                dmessage.status = message.status
                dmessage
            }

        }

        fun getUser(duser: DUser): User {
            return User(
                duser.id,
                duser.uid,
                duser.id.toString(),
                duser.idDialog,
                duser.name,
                duser.nick,
                duser.avatar,
                duser.isOnline
            )
        }

        fun getDUser(user: User): DUser {
            val duser = DUser()
            duser.id = user.id_
            duser.uid = user.uid
            duser.idDialog = user.idDialog
            duser.name = user.name
            duser.nick = user.nick
            duser.avatar = user.avatar
            duser.isOnline = user.isOnline

            return duser
        }
    }
}
