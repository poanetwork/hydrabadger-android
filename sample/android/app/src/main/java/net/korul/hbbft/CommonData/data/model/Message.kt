package net.korul.hbbft.CommonData.data.model

import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.commons.models.MessageContentType
import java.util.*


class Message(
    var id_: Long = 0,
    private var id: String,
    var idDialog: String,
    var isVisible: Boolean,
    private var user: User,
    private var text: String?,
    private var createdAt: Date? = Date()
) : IMessage,
    MessageContentType.Image, /*this is for default image messages implementation*/
    MessageContentType /*and this one is for custom content type (in this case - voice message)*/ {
    private var image: Image? = null
    var voice: Voice? = null

    val status: String
        get() = "Sent"

    fun getImage(): Image? {
        return image
    }

    override fun getId(): String {
        return id
    }

    fun setId(id: String) {
        this.id = id
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

    fun setUser(user: User) {
        this.user = user
    }

    fun setCreatedAt(createdAt: Date) {
        this.createdAt = createdAt
    }

    fun setImage(image: Image) {
        this.image = image
    }

    override fun equals(other: Any?): Boolean {
        return try { this.createdAt == (other as Message).createdAt && this.idDialog == other.idDialog && this.text == other.text }
        catch (e: Exception) {
            false
        }
    }


    class Image(val url: String)

    class Voice(val url: String, val duration: Int)
}
