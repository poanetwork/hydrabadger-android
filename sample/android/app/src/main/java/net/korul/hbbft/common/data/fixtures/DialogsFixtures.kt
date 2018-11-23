package net.korul.hbbft.common.data.fixtures

import android.database.sqlite.SQLiteConstraintException
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

        fun setNewDialog(nameDialog: String, user: User): Dialog {
            val nextID = getNextDialogID()
            user.id_ = getNextUserID()
            user.id = nextID

            val duser = Conversations.getDUser(user)
            try {
                duser.insert()
            }
            catch (e: SQLiteConstraintException) {
                duser.update()
            }

            val users: MutableList<User> = arrayListOf()
            users.add(user)
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
