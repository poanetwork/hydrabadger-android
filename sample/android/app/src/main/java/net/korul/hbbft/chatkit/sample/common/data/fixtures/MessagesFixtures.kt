package net.korul.hbbft.chatkit.sample.common.data.fixtures

import net.korul.hbbft.chatkit.sample.common.data.model.Message
import net.korul.hbbft.chatkit.sample.common.data.model.User
import java.util.*

/*
 * Created by troy379 on 12.12.16.
 */
class MessagesFixtures private constructor() : FixturesData() {
    init {
        throw AssertionError()
    }

    companion object {

        val imageMessage: Message
            get() {
                val message = Message(FixturesData.randomId, user, null)
                message.setImage(Message.Image(FixturesData.randomImage))
                return message
            }

        val voiceMessage: Message
            get() {
                val message = Message(FixturesData.randomId, user, null)
                message.voice = Message.Voice("http://example.com", FixturesData.rnd.nextInt(200) + 30)
                return message
            }

        val textMessage: Message
            get() = getTextMessage(FixturesData.randomMessage)

        fun getTextMessage(text: String): Message {
            return Message(FixturesData.randomId, user, text)
        }

        fun getMessages(startDate: Date?): ArrayList<Message> {
            val messages = ArrayList<Message>()
            for (i in 0..9/*days count*/) {
                val countPerDay = FixturesData.rnd.nextInt(5) + 1

                for (j in 0 until countPerDay) {
                    val message: Message
                    if (i % 2 == 0 && j % 3 == 0) {
                        message = imageMessage
                    } else {
                        message = textMessage
                    }

                    val calendar = Calendar.getInstance()
                    if (startDate != null) calendar.time = startDate
                    calendar.add(Calendar.DAY_OF_MONTH, -(i * i + 1))

                    message.setCreatedAt(calendar.time)
                    messages.add(message)
                }
            }
            return messages
        }

        private val user: User
            get() {
                val even = FixturesData.rnd.nextBoolean()
                return User(
                    if (even) "0" else "1",
                    if (even) FixturesData.names[0] else FixturesData.names[1],
                    if (even) FixturesData.avatars[0] else FixturesData.avatars[1],
                    true
                )
            }
    }
}
