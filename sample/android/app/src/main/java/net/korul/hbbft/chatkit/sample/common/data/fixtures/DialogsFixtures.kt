package net.korul.hbbft.chatkit.sample.common.data.fixtures

import net.korul.hbbft.chatkit.sample.common.data.model.Dialog
import net.korul.hbbft.chatkit.sample.common.data.model.Message
import net.korul.hbbft.chatkit.sample.common.data.model.User
import java.util.*

/*
 * Created by Anton Bevza on 07.09.16.
 */
class DialogsFixtures private constructor() : FixturesData() {
    init {
        throw AssertionError()
    }

    companion object {

        val dialogs: ArrayList<Dialog>
            get() {
                val chats = ArrayList<Dialog>()

                for (i in 0..19) {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_MONTH, -(i * i))
                    calendar.add(Calendar.MINUTE, -(i * i))

                    chats.add(getDialog(i, calendar.time))
                }

                return chats
            }

        private fun getDialog(i: Int, lastMessageCreatedAt: Date): Dialog {
            val users = users
            return Dialog(
                FixturesData.Companion.randomId,
                if (users.size > 1) FixturesData.Companion.groupChatTitles[users.size - 2] else users[0].name,
                if (users.size > 1) FixturesData.Companion.groupChatImages[users.size - 2] else FixturesData.Companion.randomAvatar,
                users,
                getMessage(lastMessageCreatedAt),
                if (i < 3) 3 - i else 0
            )
        }

        private val users: ArrayList<User>
            get() {
                val users = ArrayList<User>()
                val usersCount = 1 + FixturesData.Companion.rnd.nextInt(4)

                for (i in 0 until usersCount) {
                    users.add(user)
                }

                return users
            }

        private val user: User
            get() = User(
                FixturesData.Companion.randomId,
                FixturesData.Companion.randomName,
                FixturesData.Companion.randomAvatar,
                FixturesData.Companion.randomBoolean
            )

        private fun getMessage(date: Date): Message {
            return Message(
                FixturesData.Companion.randomId,
                user,
                FixturesData.Companion.randomMessage,
                date
            )
        }
    }
}
