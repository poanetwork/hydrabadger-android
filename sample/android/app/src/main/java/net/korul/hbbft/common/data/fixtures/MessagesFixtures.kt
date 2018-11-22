package net.korul.hbbft.common.data.fixtures

import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.Message
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.core.Getters.getNextMessageID
import java.util.*


class MessagesFixtures private constructor() {
    init {
        throw AssertionError()
    }

    companion object {

//        val imageMessage: Message
//            get() {
//                val message = Message(FixturesData.randomId, user, null)
//                message.setImage(Message.Image(FixturesData.randomImage))
//                return message
//            }
//
//        val voiceMessage: Message
//            get() {
//                val message = Message(FixturesData.randomId, user, null)
//                message.voice = Message.Voice("http://example.com", FixturesData.rnd.nextInt(200) + 30)
//                return message
//            }


        fun getTextMessage(text: String, curDialog: Dialog, user: User): Message {
            return Message(getNextMessageID(curDialog.id), curDialog.id, user, text, Date())
        }

        // TODO podgruzka
        fun getMessages(startDate: Date?, curDialog: Dialog): ArrayList<Message> {
            val messages = ArrayList<Message>()
//            for (i in 0..9/*days count*/) {
//                val countPerDay = FixturesData.rnd.nextInt(5) + 1
//
//                for (j in 0 until countPerDay) {
//                    val message: Message = if (i % 2 == 0 && j % 3 == 0) {
//                        imageMessage
//                    } else {
//                        textMessage
//                    }
//
//                    val calendar = Calendar.getInstance()
//                    if (startDate != null) calendar.time = startDate
//                    calendar.add(Calendar.DAY_OF_MONTH, -(i * i + 1))
//
//                    message.setCreatedAt(calendar.time)
//                    messages.add(message)
//                }
//            }
            return messages
        }
    }
}
