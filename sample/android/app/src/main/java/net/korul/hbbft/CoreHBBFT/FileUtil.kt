package net.korul.hbbft.CoreHBBFT

import android.util.Log
import java.io.*

object FileUtil {
    fun WriteObjectToFile(serObj: String, path: String) {
        try {
            val fileout = File(CoreHBBFT.mApplicationContext.filesDir, path)
            if (!fileout.exists())
                fileout.createNewFile()
            val fileOut = FileOutputStream(fileout)
            val objectOut = ObjectOutputStream(fileOut)
            objectOut.writeObject(serObj)
            objectOut.close()
            Log.d(CoreHBBFT.TAG, "The Object  was succesfully written to a file")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun ReadObjectFromFile(path: String): String? {
        return try {
            val filein = File(CoreHBBFT.mApplicationContext.filesDir, path)
            val fileIn = FileInputStream(filein)
            val objectIn = ObjectInputStream(fileIn)

            val obj = objectIn.readObject()

            Log.d(CoreHBBFT.TAG, "The Object has been read from the file")
            objectIn.close()
            obj as String
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }


    fun WriteAnyObjectToFile(serObj: Any, path: String) {
        try {
            val fileout = File(CoreHBBFT.mApplicationContext.filesDir, path)
            if (!fileout.exists())
                fileout.createNewFile()
            val fileOut = FileOutputStream(fileout)
            val objectOut = ObjectOutputStream(fileOut)
            objectOut.writeObject(serObj)
            objectOut.close()
            Log.d(CoreHBBFT.TAG, "The Object  was succesfully written to a file")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun ReadAnyObjectFromFile(path: String): Any? {
        return try {
            val filein = File(CoreHBBFT.mApplicationContext.filesDir, path)
            val fileIn = FileInputStream(filein)
            val objectIn = ObjectInputStream(fileIn)

            val obj = objectIn.readObject()

            Log.d(CoreHBBFT.TAG, "The Object has been read from the file")
            objectIn.close()
            obj
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }
}
