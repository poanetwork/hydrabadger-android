package net.korul.hbbft.common.data.model.core

import com.raizlabs.android.dbflow.sql.language.Select
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.conversation.Conversations
import net.korul.hbbft.common.data.model.conversation.Conversations.Companion.getDialog
import net.korul.hbbft.common.data.model.conversation.Conversations.Companion.getUser
import net.korul.hbbft.common.data.model.databaseModel.*
import java.util.*


object Getters {

    fun getAllDialog(): MutableList<Dialog> {
        val ddialogs = Select()
            .from(DDialog::class.java)
            .queryList()

        val dialogs: MutableList<Dialog> = arrayListOf()
        for (ddialog in ddialogs) {
            ddialog.lastMessage = Select()
                .from(DMessage::class.java)
                .where(DMessage_Table.id.eq(ddialog.lastMessageID))
                .querySingle()

            if (ddialog.lastMessage != null) {
                val duser = Select()
                    .from(DUser::class.java)
                    .where(DUser_Table.id.eq(ddialog.lastMessage?.userID))
                    .querySingle()

                ddialog.lastMessage?.user = duser!!
            }

            ddialog.users.clear()
            for (id in ddialog.usersIDs) {
                val us = Select()
                    .from(DUser::class.java)
                    .where(DUser_Table.id.eq(id))
                    .querySingle()

                if (us != null)
                    ddialog.users.add(
                        us
                    )
            }

            dialogs.add(getDialog(ddialog))
        }

        return dialogs
    }

    fun getUsers(id: String): MutableList<User?> {
        val users: MutableList<User?> = arrayListOf()

        val dusers = Select()
            .from(DUser::class.java)
            .where(DUser_Table.idDialog.eq(id))
            .queryList()

        for (duser in dusers) {
            users.add(getUser(duser))
        }

        return users
    }

    fun getUser(id: Long): User? {
        val user = Select()
            .from(DUser::class.java)
            .where(DUser_Table.id.eq(id))
            .querySingle()

        return Conversations.getUser(user!!)
    }

    fun getUserbyUID(uid: String, idDialog: String): User? {
        val user = Select()
            .from(DUser::class.java)
            .where(DUser_Table.uid.eq(uid))
            .and(DUser_Table.idDialog.eq(idDialog))
            .querySingle()

        return Conversations.getUser(user!!)
    }

    fun getDUser(id: Long): DUser {
        val user = Select()
            .from(DUser::class.java)
            .where(DUser_Table.id.eq(id))
            .querySingle()

        return user!!
    }

    fun getDialogByRoomName(roomName: String): Dialog {
        val ddialog = Select()
            .from(DDialog::class.java)
            .where(DDialog_Table.dialogName.eq(roomName))
            .querySingle()

        if (ddialog != null) {
            ddialog.lastMessage = Select()
                .from(DMessage::class.java)
                .where(DMessage_Table.id.eq(ddialog.lastMessageID))
                .querySingle()

            if (ddialog.lastMessage != null) {
                val duser = Select()
                    .from(DUser::class.java)
                    .where(DUser_Table.id.eq(ddialog.lastMessage?.userID))
                    .querySingle()

                ddialog.lastMessage?.user = duser!!
            }

            ddialog.users.clear()
            for (id in ddialog.usersIDs) {
                val us = Select()
                    .from(DUser::class.java)
                    .where(DUser_Table.id.eq(id))
                    .querySingle()

                if (us != null)
                    ddialog.users.add(
                        us
                    )
            }
        }

        return Conversations.getDialog(ddialog!!)
    }

    fun getDialog(id: String): Dialog {
        val ddialog = Select()
            .from(DDialog::class.java)
            .where(DDialog_Table.id.eq(id))
            .querySingle()

        if (ddialog != null) {
            ddialog.lastMessage = Select()
                .from(DMessage::class.java)
                .where(DMessage_Table.id.eq(ddialog.lastMessageID))
                .querySingle()

            if (ddialog.lastMessage != null) {
                val duser = Select()
                    .from(DUser::class.java)
                    .where(DUser_Table.id.eq(ddialog.lastMessage?.userID))
                    .querySingle()

                ddialog.lastMessage?.user = duser!!
            }

            ddialog.users.clear()
            for (id in ddialog.usersIDs) {
                val us = Select()
                    .from(DUser::class.java)
                    .where(DUser_Table.id.eq(id))
                    .querySingle()

                if (us != null)
                    ddialog.users.add(
                        us
                    )
            }
        }

        return Conversations.getDialog(ddialog!!)
    }

    fun setLastMessage(dialog: Dialog?) {
        val ddialog = Conversations.getDDialog(dialog!!)
        val mes = getMessagesLessDate(Date(), ddialog.id)
        if (mes.size > 0) {
            ddialog.lastMessage = Conversations.getDMessage(mes[0])
            ddialog.lastMessageID = mes[0]?.id_
        } else
            ddialog.lastMessage = null

        ddialog.update()
    }

    fun getMessages(id: String): MutableList<Message?> {
        val messages: MutableList<Message?> = arrayListOf()

        val dmessages = Select()
            .from(DMessage::class.java)
            .where(DMessage_Table.idDialog.eq(id))
            .orderBy(DMessage_Table.createdAt, false)
            .queryList()

        for (dmessage in dmessages) {
            dmessage.user = getDUser(dmessage.userID)
            messages.add(Conversations.getMessage(dmessage))
        }

        return messages
    }

    fun getMessagesLessDate(startDate: Date?, id: String): MutableList<Message?> {
        val messages: MutableList<Message?> = arrayListOf()

        val dmessages = Select()
            .from(DMessage::class.java)
            .where(DMessage_Table.idDialog.eq(id))
            .and(DMessage_Table.createdAt.lessThan(startDate!!))
            .orderBy(DMessage_Table.createdAt, false)
            .queryList()

        for (dmessage in dmessages) {
            dmessage.user = getDUser(dmessage.userID)
            messages.add(Conversations.getMessage(dmessage))
        }

        return messages
    }

    fun getNextDialogID(): String {
        return java.lang.Long.toString(UUID.randomUUID().leastSignificantBits)
    }

    fun getNextMessageID(): Long {
        val list = Select()
            .from(DMessage::class.java)
            .orderBy(DMessage_Table.id, false)
            .queryList()

        return if (list.isEmpty())
            0
        else {
            val ind = list.maxBy { it.id }!!.id
            (ind + 1L)
        }
    }

    fun getAllUsersDistinct(): Array<User> {
        val users: MutableList<User> = arrayListOf()

        val dusers = Select()
            .from(DUser::class.java)
            .queryList()

        val dus = dusers.filterNotNull()
        val duss = dus.distinctBy { it.id }
        for (duser in duss) {
            users.add(getUser(duser))
        }
        return users.toTypedArray()
    }

    fun getAllUsers(uid: String): Array<User> {
        val users: MutableList<User> = arrayListOf()

        val dusers = Select()
            .from(DUser::class.java)
            .where(DUser_Table.uid.eq(uid))
            .queryList()

        val dus = dusers.filterNotNull()
        for (duser in dus) {
            users.add(getUser(duser))
        }
        return users.toTypedArray()
    }

    fun getAllUsers(): Array<User> {
        val users: MutableList<User> = arrayListOf()

        val dusers = Select()
            .from(DUser::class.java)
            .queryList()

        val dus = dusers.filterNotNull()
        for (duser in dus) {
            users.add(getUser(duser))
        }
        return users.toTypedArray()
    }

    fun updateUserbyUID(uid: String, user: User) {
        val users = Select()
            .from(DUser::class.java)
            .where(DUser_Table.uid.eq(uid))
            .queryList()

        for (duser in users) {
            duser.avatar = user.avatar
            duser.name = user.name
            duser.nick = user.nick
            duser.isOnline = user.isOnline
            duser.update()
        }
    }

    fun removeUserByUid(user: User) {
        val dusers = Select()
            .from(DUser::class.java)
            .where(DUser_Table.uid.eq(user.uid))
            .queryList()

        val dus = dusers.filterNotNull()
        for (duser in dus) {
            duser.delete()
        }
    }

    fun getNextUserID(): Long {
        val list = Select()
            .from(DUser::class.java)
            .orderBy(DUser_Table.id, false)
            .queryList()

        return if (list.isEmpty())
            0
        else {
            val ind = list.maxBy { it.id }!!.id
            (ind + 1L)
        }
    }
}
