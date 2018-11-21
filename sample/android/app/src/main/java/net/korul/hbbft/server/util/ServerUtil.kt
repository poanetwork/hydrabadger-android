package net.korul.hbbft.server.util

import org.json.JSONArray
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.nio.ByteBuffer

class ServerUtil {
    companion object {

        fun resetConnectOnServer(uniqueID: String, ipserver: String) {
            try {
//            62.176.10.54
                val soc = Socket(ipserver, 2999)
                val dout = DataOutputStream(soc.getOutputStream())
                val din = DataInputStream(soc.getInputStream())

                val magick = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xCA.toByte(), 0xFE.toByte())
                val bytesLengthString = ByteBuffer.allocate(4).putInt(uniqueID.count()).array()
                val original = uniqueID
                val utf8Bytes = original.toByteArray(charset("UTF8"))

                dout.write(magick)
                dout.write(bytesLengthString, 0, 4)
                dout.write(utf8Bytes, 0, utf8Bytes.count())
                dout.flush()

                dout.close()
                din.close()
                soc.close()
            } catch (e: Exception) {
                e.printStackTrace()

                throw e
            }
        }

        fun connectToStun(uniqueID: String, ipserver: String): ModelStun {
            try {
                val soc = Socket(ipserver, 3000)
                val dout = DataOutputStream(soc.getOutputStream())
                val din = DataInputStream(soc.getInputStream())

                val magick = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xCA.toByte(), 0xFE.toByte())
                val bytesLengthString = ByteBuffer.allocate(4).putInt(uniqueID.count()).array()
                val original = uniqueID
                val utf8Bytes = original.toByteArray(charset("UTF8"))

                dout.write(magick)
                dout.write(bytesLengthString, 0, 4)
                dout.write(utf8Bytes, 0, utf8Bytes.count())
                dout.flush()

                var bytesnum = din.available()
                while (bytesnum < 10) {
                    Thread.sleep(10)
                    bytesnum = din.available()
                }

                val modelAnswer = ModelStun()

                modelAnswer.myRecievePort = din.readUnsignedShort()
                modelAnswer.myPortForOther = din.readUnsignedShort()
                modelAnswer.myPort = din.readUnsignedShort()

                val sizeString = din.readInt()

                bytesnum = din.available()
                while (bytesnum < sizeString) {
                    Thread.sleep(10)
                    bytesnum = din.available()
                }
                val ip = din.readBytes()

                modelAnswer.myIP = String(ip, charset("UTF8"))

                dout.close()
                din.close()
                soc.close()

                return modelAnswer
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }

        fun requestGetClient(RoomName: String, myUid: String): MutableList<String> {
            // 1. Declare a URL Connection
            val arraysToSend: MutableList<String> = arrayListOf()
            try {
                //http://korul.esy.es/ServerSignal.php?action=GetClients&roomName=RoomName
                val url = URL("http://korul.esy.es/ServerSignal.php?action=GetClients&roomName=$RoomName")
                val conn = url.openConnection() as HttpURLConnection
                // 2. Open InputStream to connection
                conn.connect()
                val `in` = conn.inputStream
                // 3. Download and decode the string response using builder
                val stringBuilder = StringBuilder()
                val reader = BufferedReader(InputStreamReader(`in`))

                var line: String?
                do {
                    line = reader.readLine()
                    if (line == null)
                        break

                    stringBuilder.append(line)
                } while (true)

                val jsonArr = JSONArray(stringBuilder.toString())
                for (i in 0 until jsonArr.length()) {
                    val jsonObj = jsonArr.getJSONObject(i)

                    val tosend = jsonObj.getString("MyIpPort")
                    if (jsonObj.getString("Login") != myUid) {
                        println(tosend)
                        arraysToSend.add(tosend)
                    }
                }
                reader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return arraysToSend
        }


        fun requestDeleteClient(args: Array<String>) {
            if (args.isEmpty())
                return

            try {
                // 1. Declare a URL Connection
                //http://korul.esy.es/ServerSignal.php?action=DeleteClient&Login=author
                val url = URL("http://korul.esy.es/ServerSignal.php?action=DeleteClient&login=${args[0]}")
                val conn = url.openConnection() as HttpURLConnection
                // 2. Open InputStream to connection
                conn.connect()
                val `in` = conn.inputStream
                // 3. Download and decode the string response using builder
                val stringBuilder = StringBuilder()
                val reader = BufferedReader(InputStreamReader(`in`))
                reader.readLine()
                reader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun requestInsertClient(args: Array<String>) {
            if (args.size < 3)
                return

            try {
                // 1. Declare a URL Connection
                //http://korul.esy.es/ServerSignal.php?action=InsertClient&login=author&roomName=RoomName&myIpPort=ip;port1
                val url =
                    URL("http://korul.esy.es/ServerSignal.php?action=InsertClient&login=${args[0]}&roomName=${args[1]}&myIpPort=${args[2]}")
                val conn = url.openConnection() as HttpURLConnection
                // 2. Open InputStream to connection
                conn.connect()
                val `in` = conn.inputStream
                // 3. Download and decode the string response using builder
                val reader = BufferedReader(InputStreamReader(`in`))
                reader.readLine()
                reader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }
}