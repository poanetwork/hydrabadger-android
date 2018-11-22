package net.korul.hbbft.common.data.model.core

import com.raizlabs.android.dbflow.sql.language.Select
import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.conversation.Conversations
import net.korul.hbbft.common.data.model.conversation.Conversations.Companion.getDialog
import net.korul.hbbft.common.data.model.conversation.Conversations.Companion.getUser
import net.korul.hbbft.common.data.model.databaseModel.*


object Getters {

    fun getAllDialog(): MutableList<Dialog> {
        val ddialogs = Select()
            .from(DDialog::class.java)
            .queryList()

        val dialogs: MutableList<Dialog> = arrayListOf()
        for (ddialog in ddialogs) {
            ddialog.lastMessage = Select()
                .from(DMessage::class.java)
                .where(DMessage_Table.id.eq(ddialog.lastMessageID) )
                .querySingle()!!

            ddialog.users.clear()
            for (id in ddialog.usersIDs) {
                ddialog.users.add(
                    Select()
                        .from(DUser::class.java)
                        .where(DUser_Table.id.eq(id) )
                        .querySingle()!!
                )
            }

            dialogs.add(getDialog(ddialog))
        }

        return dialogs
    }

    fun getUsers(id: String): MutableList<User> {
        val users: MutableList<User> = arrayListOf()

        val dusers = Select()
            .from(DUser::class.java)
            .where(DUser_Table.idDialog.eq(id) )
            .queryList()

        for(duser in dusers) {
            users.add(getUser(duser))
        }

        return users
    }

    fun getMessages(id: String): MutableList<Message> {
        val messages: MutableList<Message> = arrayListOf()

        val dmessages = Select()
            .from(DMessage::class.java)
            .where(DMessage_Table.idDialog.eq(id) )
            .queryList()

        for(dmessage in dmessages) {
            messages.add(Conversations.getMessage(dmessage))
        }

        return messages
    }

    fun getNextMessageID(id: String): Long {
        val list = Select()
            .from(DMessage::class.java)
            .where(DMessage_Table.idDialog.eq(id))
            .orderBy(DMessage_Table.id, false)
            .queryList()

        val ind = list.maxBy { it.id }!!.id

        return (ind + 1L)
    }

    fun getNextUserID(id: String): Long {
        val list = Select()
            .from(DUser::class.java)
            .where(DUser_Table.idDialog.eq(id))
            .orderBy(DUser_Table.id, false)
            .queryList()

        val ind = list.maxBy { it.id }!!.id

        return (ind + 1L)
    }
}
