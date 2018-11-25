package net.korul.hbbft.common.data.fixtures

import net.korul.hbbft.common.data.fixtures.MessagesFixtures.Companion.getAllMessages
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.conversation.Conversations
import net.korul.hbbft.common.data.model.core.Getters.getAllDialog
import net.korul.hbbft.common.data.model.core.Getters.getNextDialogID
import net.korul.hbbft.common.data.model.core.Getters.getNextUserID


class DialogsFixtures private constructor() {
    init {
        throw AssertionError()
    }

    companion object {
        val dialogs: ArrayList<Dialog>
            get() {
                return ArrayList(getAllDialog())
            }

        fun setNewDialog(nameDialog: String, user: User, user2: User): Dialog {
            val nextID = getNextDialogID()

            user.id_ = getNextUserID()
            user.idDialog = nextID
            user.id = user.id_.toString()
            user.uid = "0"
            val duser = Conversations.getDUser(user)
            duser.insert()

            user2.id_ = getNextUserID()
            user2.id = user2.id_.toString()
            user2.idDialog = nextID
            val duser2 = Conversations.getDUser(user2)
            duser2.insert()

            val users: MutableList<User> = arrayListOf()
            users.add(user)
            users.add(user2)
            val dialog = Dialog(nextID, nameDialog, user.avatar, ArrayList(users), null, 0)

            val ddialog = Conversations.getDDialog(dialog)
            ddialog.insert()

            return dialog
        }

        fun deleteDialog(dialog: Dialog) {
            val ddialog = Conversations.getDDialog(dialog)

            val messages = getAllMessages(dialog)
            for (mes in messages) {
                val dmes = Conversations.getDMessage(mes)
                dmes?.delete()
            }

            for (duser in ddialog.users) {
                if(duser.idDialog != "")
                    duser.delete()
            }

            ddialog.lastMessage?.delete()


            ddialog.delete()
        }

    }
}
