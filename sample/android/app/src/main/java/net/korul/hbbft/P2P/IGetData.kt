package ru.hintsolutions.myapplication2
import java.nio.ByteBuffer

interface IGetData {
    fun dataReceived(receiveFrom: String, bytes: ByteArray)
}