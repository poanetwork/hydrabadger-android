package net.korul.hbbft.P2P
import java.nio.ByteBuffer

interface IGetData {
    fun dataReceived(bytes: ByteArray)
}