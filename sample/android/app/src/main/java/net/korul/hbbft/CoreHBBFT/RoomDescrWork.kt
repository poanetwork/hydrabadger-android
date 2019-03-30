package net.korul.hbbft.CoreHBBFT

import android.content.Intent
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import net.korul.hbbft.CommonData.data.fixtures.DialogsFixtures.Companion.setNewExtDialog
import net.korul.hbbft.CommonData.data.model.Dialog
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.data.model.conversation.Conversations
import net.korul.hbbft.CommonData.data.model.core.Getters
import net.korul.hbbft.CommonData.data.model.core.Getters.getNextUserID
import net.korul.hbbft.CommonData.utils.AppUtils
import net.korul.hbbft.CoreHBBFT.RoomWork.getUIDsInRoomFromFirebase
import net.korul.hbbft.CoreHBBFT.RoomWork.registerInRoomInFirebase
import net.korul.hbbft.CoreHBBFT.UserWork.getUserFromLocalOrDownloadFromFirebase
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.FirebaseStorageDU.MyDownloadRoomService
import net.korul.hbbft.FirebaseStorageDU.MyGetLastModificationRoomService
import java.io.File
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread


object RoomDescrWork {

    fun updateAllRoomsFromFirebase() {
        for (roomID in Getters.getAllDialogsUids()) {
            getUpdateFromFirebase(roomID, object : IAddToRooms {
                override fun errorAddRoom() {
                    AppUtils.showToast(
                        CoreHBBFT.mApplicationContext,
                        "Error update $roomID", true
                    )
                }

                override fun dialog(dialog: Dialog) {
                    Conversations.getDDialog(dialog).update()
                }
            })
        }
    }

    fun unregisterInRoomDescFirebase(dialogId: String) {
        val queryRef = CoreHBBFT.mDatabase.child("RoomDescr").child(dialogId)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }

                Log.d(CoreHBBFT.TAG, "Success unregisterInRoomDescFirebase $dialogId")
            }
        })
    }

    fun registerInRoomDescFirebase(dialog: Dialog) {
        registerInRoomDescFirebase(dialog, object : IComplete {
            override fun complete() {
            }
        })
    }

    fun registerInRoomDescFirebase(dialog: Dialog, listener: IComplete) {
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

                listener.complete()

                Log.d(CoreHBBFT.TAG, "Success registerInRoomDescFirebase ${dialog.id}")
            }
        })
    }


    private fun getUpdateFromFirebase(dialogID: String, listener: IAddToRooms) {
        val queryRef = CoreHBBFT.mDatabase.child("RoomDescr").child(dialogID)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                listener.errorAddRoom()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val objectMap = snapshot.value as HashMap<String, Any>
                if (objectMap != null) {
                    thread {
                        for (obj in objectMap.values) {
                            val mapObj: Map<String, Any> = obj as Map<String, Any>

                            val roomDescr = RoomDescr()
                            roomDescr.id = mapObj["id"] as String
                            roomDescr.dialogName = mapObj["dialogName"] as String
                            roomDescr.dialogDescription = mapObj["dialogDescription"] as String

                            val outputDir = CoreHBBFT.mApplicationContext.filesDir
                            val localFile = File(outputDir.path + File.separator + dialogID + ".png")

                            val dialog = Getters.getDialogByRoomId(dialogID)
                            val saveDialog = Dialog(
                                dialogID,
                                roomDescr.dialogName!!,
                                roomDescr.dialogDescription!!,
                                localFile.path,
                                dialog.users,
                                dialog.lastMessage,
                                dialog.unreadCount
                            )

                            CoreHBBFT.mApplicationContext.startService(
                                Intent(CoreHBBFT.mApplicationContext, MyGetLastModificationRoomService::class.java)
                                    .putExtra(MyGetLastModificationRoomService.EXTRA_COMPARE_UID, dialogID)
                                    .setAction(MyGetLastModificationRoomService.ACTION_COMPARE)
                            )

                            listener.dialog(saveDialog)
                        }
                    }
                } else {
                    listener.errorAddRoom()
                }
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
                val objectMap = snapshot.value as HashMap<String, Any>?
                if (objectMap != null) {
                    thread {
                        for (obj in objectMap.values) {
                            val mapObj: Map<String, Any> = obj as Map<String, Any>

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

                            val user = DatabaseApplication.mCurUser
                            user.id_ = getNextUserID()
                            user.idDialog = roomDescr.id!!
                            user.id = user.id_.toString()
                            val duser = Conversations.getDUser(user)
                            duser.insert()

                            listOfUsers.add(user)

                            val outputDir = CoreHBBFT.mApplicationContext.filesDir
                            val localFile = File(outputDir.path + File.separator + roomDescr.id + ".png")

                            val dialog = setNewExtDialog(
                                roomDescr.id!!,
                                roomDescr.dialogName!!,
                                roomDescr.dialogDescription!!,
                                localFile.path,
                                listOfUsers
                            )

                            registerInRoomInFirebase(roomDescr.id!!)

                            // Kick off MyDownloadUserService to download the file
                            val intent = Intent(CoreHBBFT.mApplicationContext, MyDownloadRoomService::class.java)
                                .putExtra(MyDownloadRoomService.EXTRA_DOWNLOAD_DIALOGID, roomDescr.id!!)
                                .setAction(MyDownloadRoomService.ACTION_DOWNLOAD)
                            CoreHBBFT.mApplicationContext.startService(intent)

                            listener.dialog(dialog)
                        }
                    }
                } else {
                    listener.errorAddRoom()
                }
            }
        })
    }
}