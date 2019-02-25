package net.korul.hbbft.common.data.fixtures

import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.conversation.Conversations
import net.korul.hbbft.common.data.model.core.Getters
import net.korul.hbbft.common.data.model.core.Getters.getNextMessageID
import java.security.SecureRandom
import java.util.*
import kotlin.collections.ArrayList


class MessagesFixtures private constructor() {
    init {
        throw AssertionError()
    }

    companion object {

        var rnd = SecureRandom()

        fun getImageMessage(curDialog: Dialog, user: User): Message {
            val id = getNextMessageID()
            val message = Message(id, id.toString(), curDialog.id, user, null, Date())
            message.setImage(Message.Image("https://habrastorage.org/getpro/habr/post_images/e4b/067/b17/e4b067b17a3e414083f7420351db272b.jpg"))

            val dmes = Conversations.getDMessage(message)
            dmes?.insert()

            curDialog.setLastMessage(message)
            val ddialog = Conversations.getDDialog(curDialog)
            ddialog.update()

            return message
        }

        fun getVoiceMessage(curDialog: Dialog, user: User): Message {
            val id = getNextMessageID()
            val message = Message(id, id.toString(), curDialog.id, user, null, Date())
            message.voice = Message.Voice("http://example.com", rnd.nextInt(200) + 30)

            val dmes = Conversations.getDMessage(message)
            dmes?.insert()

            curDialog.setLastMessage(message)
            val ddialog = Conversations.getDDialog(curDialog)
            ddialog.update()

            return message
        }


        // set new message
        fun setNewMessage(text: String, curDialog: Dialog, user: User): Message {
            user.idDialog = curDialog.id
            val id = getNextMessageID()
            val mes = Message(id, id.toString(), curDialog.id, user, text, Date())

            val dmes = Conversations.getDMessage(mes)
            dmes?.insert()

            curDialog.setLastMessage(mes)
            curDialog.lastMessage?.user = user
            val ddialog = Conversations.getDDialog(curDialog)
            ddialog.update()

            return mes
        }

        fun deleteMeseges(messages: ArrayList<Message>) {
            for (message in messages) {
                val dmes = Conversations.getDMessage(message)
                dmes?.delete()
            }
        }

        fun getAllMessages(curDialog: Dialog): ArrayList<Message?> {
            val messages = Getters.getMessages(curDialog.id)

            return ArrayList(messages)
        }

        fun getMessages(startDate: Date?, curDialog: Dialog): ArrayList<Message?> {
            val startDate_ = Date()
            if (startDate != null) startDate_.time = startDate.time

            val messages = Getters.getMessagesLessDate(startDate_, curDialog.id)

            return ArrayList(messages)
        }
    }
}
