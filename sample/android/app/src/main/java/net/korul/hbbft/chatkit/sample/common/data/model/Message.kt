package net.korul.hbbft.chatkit.sample.common.data.model

import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.commons.models.MessageContentType
import java.util.*

/*
 * Created by troy379 on 04.04.17.
 */
class Message @JvmOverloads constructor(
    private val id: String,
    private val user: User,
    private var text: String?,
    private var createdAt: Date? = Date()
) : IMessage, MessageContentType.Image, /*this is for default image messages implementation*/
    MessageContentType /*and this one is for custom content type (in this case - voice message)*/ {
    private var image: Image? = null
    var voice: Voice? = null

    val status: String
        get() = "Sent"

    override fun getId(): String {
        return id
    }

    override fun getText(): String? {
        return text
    }

    override fun getCreatedAt(): Date? {
        return createdAt
    }

    override fun getUser(): User {
        return this.user
    }

    override fun getImageUrl(): String? {
        return if (image == null) null else image!!.url
    }

    fun setText(text: String) {
        this.text = text
    }

    fun setCreatedAt(createdAt: Date) {
        this.createdAt = createdAt
    }

    fun setImage(image: Image) {
        this.image = image
    }

    class Image(val url: String)

    class Voice(val url: String, val duration: Int)
}
