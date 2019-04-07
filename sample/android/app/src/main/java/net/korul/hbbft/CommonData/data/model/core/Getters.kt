package net.korul.hbbft.CommonData.data.model.core

import com.raizlabs.android.dbflow.sql.language.Select
import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.data.model.Message
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.data.model.conversation.Conversations
import net.korul.hbbft.CommonData.data.model.conversation.Conversations.Companion.getDialog
import net.korul.hbbft.CommonData.data.model.conversation.Conversations.Companion.getUser
import net.korul.hbbft.CommonData.data.model.databaseModel.*
import java.util.*


object Getters {

    fun getAllDialog(): MutableList<Dialog> {
        val ddialogs = Select()
            .from(DDialog::class.java)
            .queryList()

        val dialogs: MutableList<Dialog> = arrayListOf()
        for (ddialog in ddialogs) {
//            setLastMessage(Conversations.getDialog(ddialog))

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

    fun getAllDialogsName(): MutableList<String> {
        val ddialogs = Select()
            .from(DDialog::class.java)
            .queryList()

        val dialogs: MutableList<Dialog> = arrayListOf()
        for (ddialog in ddialogs)
            dialogs.add(getDialog(ddialog))

        val distDialogs = dialogs.distinctBy { it.dialogName }
        val listDiagNames: MutableList<String> = mutableListOf()

        for (diag in distDialogs) {
            listDiagNames.add(diag.dialogName)
        }

        return listDiagNames
    }

    fun getAllDialogsUids(): MutableList<String> {
        val ddialogs = Select()
            .from(DDialog::class.java)
            .queryList()

        val dialogs: MutableList<Dialog> = arrayListOf()
        for (ddialog in ddialogs)
            dialogs.add(getDialog(ddialog))

        val distDialogs = dialogs.distinctBy { it.id }
        val listDiagUids: MutableList<String> = mutableListOf()

        for (diag in distDialogs) {
            listDiagUids.add(diag.id)
        }

        return listDiagUids
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

    fun getUserbyUIDFromDialog(uid: String, idDialog: String): User? {
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

    fun getDialogByRoomId(roomId: String): Dialog {
        val ddialog = Select()
            .from(DDialog::class.java)
            .where(DDialog_Table.id.eq(roomId))
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
        val mes = getVisMessagesLessDate(Date(), ddialog.id)
        if (mes.size > 0) {
            ddialog.lastMessage = Conversations.getDMessage(mes[0])
            ddialog.lastMessageID = mes[0]?.id
        } else
            ddialog.lastMessage = null

        ddialog.update()
    }

    fun getVisMessages(id: String): MutableList<Message?> {
        val messages: MutableList<Message?> = arrayListOf()

        val dmessages = Select()
            .from(DMessage::class.java)
            .where(DMessage_Table.idDialog.eq(id))
            .and(DMessage_Table.isVisible.eq(true))
            .orderBy(DMessage_Table.createdAt, false)
            .queryList()

        for (dmessage in dmessages) {
            dmessage.user = getDUser(dmessage.userID)
            messages.add(Conversations.getMessage(dmessage))
        }

        return messages
    }

    fun getVisMessagesLessDate(startDate: Date?, id: String): MutableList<Message?> {
        val messages: MutableList<Message?> = arrayListOf()

        val dmessages = Select()
            .from(DMessage::class.java)
            .where(DMessage_Table.idDialog.eq(id))
            .and(DMessage_Table.isVisible.eq(true))
            .and(DMessage_Table.createdAt.lessThan(startDate!!))
            .orderBy(DMessage_Table.createdAt, false)
            .queryList()

        for (dmessage in dmessages) {
            dmessage.user = getDUser(dmessage.userID)
            messages.add(Conversations.getMessage(dmessage))
        }

        return messages
    }

    fun getAllMessagesWithId(idRoom: String, list: List<Long>): MutableList<Message> {
        val messages: MutableList<Message> = arrayListOf()

        for (id in list) {
            val dmessage = Select()
                .from(DMessage::class.java)
                .where(DMessage_Table.idDialog.eq(idRoom))
                .and(DMessage_Table.id.eq(id))
                .orderBy(DMessage_Table.createdAt, false)
                .querySingle()

            if (dmessage != null) {
                dmessage.user = getDUser(dmessage.userID)
                val mes = Conversations.getMessage(dmessage)
                if (mes != null) messages.add(mes)
            }
        }

        return messages
    }

    fun getAllMessagesLessGreaterDate(startDate: Date?, endDate: Date?, id: String): MutableList<Message?> {
        val messages: MutableList<Message?> = arrayListOf()

        val dmessages = Select()
            .from(DMessage::class.java)
            .where(DMessage_Table.idDialog.eq(id))
            .and(DMessage_Table.createdAt.lessThanOrEq(endDate!!))
            .and(DMessage_Table.createdAt.greaterThanOrEq(startDate!!))
            .orderBy(DMessage_Table.createdAt, false)
            .queryList()

        for (dmessage in dmessages) {
            dmessage.user = getDUser(dmessage.userID)
            messages.add(Conversations.getMessage(dmessage))
        }

        return messages
    }

    fun getNextMessageID(): Long {
        return UUID.randomUUID().leastSignificantBits
    }

    fun getAllLocalUsersDistinct(): Array<User> {
        val users: MutableList<User> = arrayListOf()

        val dusers = Select()
            .from(DUser::class.java)
            .where(DUser_Table.isVisible.eq(true))
            .queryList()

        val dus = dusers.filterNotNull()
        val duss = dus.distinctBy { it.uid }
        for (duser in duss) {
            users.add(getUser(duser))
        }
        return users.toTypedArray()
    }

    fun getAllLocalOfflineUsers(uid: String, online: Boolean) {
        val dusers = Select()
            .from(DUser::class.java)
            .where(DUser_Table.uid.eq(uid))
            .and(DUser_Table.isOnline.eq(!online))
            .queryList()

        val dus = dusers.filterNotNull()
        for (duser in dus) {
            duser.isOnline = online
            duser.update()
        }
    }

    fun getAllLocalUsers(uid: String): Array<User> {
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

    fun getAllLocalUsers(): Array<User> {
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

    fun updateMetaUserbyUID(uid: String, user: User) {
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

    fun setInvisUserByUid(user: User) {
        val dusers = Select()
            .from(DUser::class.java)
            .where(DUser_Table.uid.eq(user.uid))
            .queryList()

        val dus = dusers.filterNotNull()
        for (duser in dus) {
            duser.isVisible = false
            duser.update()
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
