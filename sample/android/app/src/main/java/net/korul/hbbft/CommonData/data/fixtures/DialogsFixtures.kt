package net.korul.hbbft.CommonData.data.fixtures

import net.korul.hbbft.CommonData.data.fixtures.MessagesFixtures.Companion.getAllMessages
import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.data.model.conversation.Conversations
import net.korul.hbbft.CommonData.data.model.core.Getters.getAllDialog
import net.korul.hbbft.CommonData.data.model.core.Getters.getNextUserID
import net.korul.hbbft.CoreHBBFT.RoomDescrWork.registerInRoomDescFirebase
import net.korul.hbbft.CoreHBBFT.RoomWork.registerInRoomInFirebase
import net.korul.hbbft.CoreHBBFT.RoomWork.unregisterInRoomInFirebase


class DialogsFixtures private constructor() {
    init {
        throw AssertionError()
    }

    companion object {
        val dialogs: ArrayList<Dialog>
            get() {
                return ArrayList(getAllDialog())
            }

        fun setNewExtDialog(
            dialogUID: String,
            nameDialog: String,
            nameDescr: String,
            avatarPath: String,
            user: User
        ): Dialog {
            user.id_ = getNextUserID()
            user.idDialog = dialogUID
            user.id = user.id_.toString()
            val duser = Conversations.getDUser(user)
            duser.insert()

            val users: MutableList<User> = arrayListOf()
            users.add(user)

            val dialog = Dialog(dialogUID, nameDialog, nameDescr, avatarPath, ArrayList(users), null, 0)
            val ddialog = Conversations.getDDialog(dialog)
            ddialog.insert()

            registerInRoomInFirebase(dialogUID)
            registerInRoomDescFirebase(dialog)

            return dialog
        }

        fun setNewExtDialog(
            dialogUID: String,
            nameDialog: String,
            nameDescr: String,
            avatarPath: String,
            users: MutableList<User>
        ): Dialog {
            val users_: MutableList<User> = arrayListOf()
            for (user in users) {
                user.id_ = getNextUserID()
                user.idDialog = dialogUID
                user.id = user.id_.toString()
                val duser = Conversations.getDUser(user)
                duser.insert()

                users_.add(user)
            }

            val dialog = Dialog(dialogUID, nameDialog, nameDescr, avatarPath, ArrayList(users_), null, 0)
            val ddialog = Conversations.getDDialog(dialog)
            ddialog.insert()

            registerInRoomInFirebase(dialogUID)
            registerInRoomDescFirebase(dialog)

            return dialog
        }


        fun deleteDialog(dialog: Dialog) {
            val ddialog = Conversations.getDDialog(dialog)

            unregisterInRoomInFirebase(dialog.id)

            val messages = getAllMessages(dialog)
            for (mes in messages) {
                val dmes = Conversations.getDMessage(mes)
                dmes?.delete()
            }

            for (duser in ddialog.users) {
                if (duser.idDialog != "") {
                    //TODO delete?
                    duser.isVisible = false
                    duser.update()
                }
            }

            ddialog.lastMessage?.delete()

            ddialog.delete()
        }

    }
}
