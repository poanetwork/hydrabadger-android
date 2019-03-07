package net.korul.hbbft.CoreHBBFT

import android.content.Intent
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import net.korul.hbbft.CommonData.data.fixtures.DialogsFixtures.Companion.setNewExtDialog
import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CoreHBBFT.RoomWork.getUIDsInRoomFromFirebase
import net.korul.hbbft.CoreHBBFT.UserWork.getUserFromLocalOrDownloadFromFirebase
import net.korul.hbbft.FirebaseStorageDU.MyDownloadRoomService
import java.io.File
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread


object RoomDescrWork {

    fun reregisterInRoomDescrFirebase(dialog: Dialog) {
        val queryRef = CoreHBBFT.mDatabase.child("RoomDescr").child(dialog.id).orderByChild("id").equalTo(dialog.id)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }

                val roomDescr = RoomDescr()
                roomDescr.id = dialog.id
                roomDescr.dialogName = dialog.dialogName
                roomDescr.dialogDescription = dialog.dialogDescr
                val ref = snapshot.ref.push()
                ref.setValue(roomDescr)

                Log.d(CoreHBBFT.TAG, "Succes reregisterInRoomDescrFirebase ${dialog.id}")
            }
        })
    }

    fun getDialogFromFirebase(dialogID: String, listener: IAddToRooms) {
        val queryRef = CoreHBBFT.mDatabase.child("RoomDescr").child(dialogID)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                listener.errorAddRoom()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val objectMap = snapshot.value as HashMap<String, Any>
                for (obj in objectMap.values) {
                    val mapObj: Map<String, Any> = obj as Map<String, Any>

                    thread {
                        val roomDescr = RoomDescr()
                        roomDescr.id = mapObj["id"] as String
                        roomDescr.dialogName = mapObj["dialogName"] as String
                        roomDescr.dialogDescription = mapObj["dialogDescription"] as String

                        val listOfUsersUIDS = getUIDsInRoomFromFirebase(roomDescr.id!!)

                        val listOfUsers: MutableList<User> = mutableListOf()
                        for (uid in listOfUsersUIDS) {
                            val semaphor = Semaphore(0)
                            getUserFromLocalOrDownloadFromFirebase(uid.UID!!, object :
                                IAddToContacts {
                                override fun user(user: User) {
                                    listOfUsers.add(user)
                                    semaphor.release()
                                }

                                override fun errorAddContact() {
                                    listener.errorAddRoom()
                                }
                            })
                            semaphor.acquire()
                        }
                        val outputDir = CoreHBBFT.mApplicationContext.filesDir
                        val localFile = File.createTempFile(roomDescr.id, "png", outputDir)
                        setNewExtDialog(
                            roomDescr.id!!,
                            roomDescr.dialogName!!,
                            roomDescr.dialogDescription!!,
                            localFile.path,
                            listOfUsers
                        )

                        // Kick off MyDownloadUserService to download the file
                        val intent = Intent(CoreHBBFT.mApplicationContext, MyDownloadRoomService::class.java)
                            .putExtra(MyDownloadRoomService.EXTRA_DOWNLOAD_DIALOGID, roomDescr.id!!)
                            .setAction(MyDownloadRoomService.ACTION_DOWNLOAD)
                        CoreHBBFT.mApplicationContext.startService(intent)
                    }

                }
            }
        })
    }
}