package net.korul.hbbft.CoreHBBFT

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import net.korul.hbbft.CoreHBBFT.RoomDescrWork.unregisterInRoomDescFirebase
import java.util.*
import java.util.concurrent.CountDownLatch

object RoomWork {

    fun registerInFirebase(ListDialogsId: List<String>, uid: String) {
        for (dialogId in ListDialogsId) {
            val queryRef = CoreHBBFT.mDatabase.child("RoomsUsers").child(dialogId).orderByChild("uid").equalTo(uid)
            queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    for (postsnapshot in snapshot.children) {
                        postsnapshot.key
                        postsnapshot.ref.removeValue()
                    }

                    val uid = Uids()
                    uid.UID = CoreHBBFT.uniqueID1
                    uid.isOnline = false
                    val ref = snapshot.ref.push()
                    ref.setValue(uid)

                    Log.d(CoreHBBFT.TAG, "Success registerInFirebase ${CoreHBBFT.uniqueID1}")
                }
            })
        }
    }


    fun unregisterInRoomInFirebase(RoomId: String) {
        val queryRef =
            CoreHBBFT.mDatabase.child("RoomsUsers").child(RoomId).orderByChild("uid").equalTo(CoreHBBFT.uniqueID1)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    for (postsnapshot in snapshot.children) {
                        postsnapshot.key
                        postsnapshot.ref.removeValue()
                    }
                    Log.d(CoreHBBFT.TAG, "Success unregisterInRoomInFirebase ${CoreHBBFT.uniqueID1}")


                    val queryRef = CoreHBBFT.mDatabase.child("RoomsUsers").child(RoomId)
                    queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                        }

                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.children.count() == 0) {
                                unregisterInRoomDescFirebase(RoomId)
                            }
                        }
                    })


                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    fun registerInRoomInFirebase(RoomId: String) {
        val queryRef =
            CoreHBBFT.mDatabase.child("RoomsUsers").child(RoomId).orderByChild("uid").equalTo(CoreHBBFT.uniqueID1)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    for (postsnapshot in snapshot.children) {
                        postsnapshot.key
                        postsnapshot.ref.removeValue()
                    }
                    Log.d(CoreHBBFT.TAG, "Success unregisterInRoomInFirebase ${CoreHBBFT.uniqueID1}")

                    // Add dialog
                    val uid = Uids()
                    uid.UID = CoreHBBFT.uniqueID1
                    uid.isOnline = false
                    val ref = CoreHBBFT.mDatabase.child("RoomsUsers").child(RoomId)
                    ref.push().setValue(uid).addOnSuccessListener {
                        Log.d(CoreHBBFT.TAG, "Success registerInRoomInFirebase ${CoreHBBFT.uniqueID1}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    fun setOfflineModeInRoomInFirebase(RoomId: String) {
        val queryRef =
            CoreHBBFT.mDatabase.child("RoomsUsers").child(RoomId).orderByChild("uid").equalTo(CoreHBBFT.uniqueID1)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }

                val uid = Uids()
                uid.UID = CoreHBBFT.uniqueID1
                uid.isOnline = false
                snapshot.ref.push().setValue(uid)
                Log.d(CoreHBBFT.TAG, "Succes setOnlineModeInRoomInFirebase ${CoreHBBFT.uniqueID1}")
            }
        })
    }

    fun setOnlineModeInRoomInFirebase(RoomId: String) {
        val queryRef =
            CoreHBBFT.mDatabase.child("RoomsUsers").child(RoomId).orderByChild("uid").equalTo(CoreHBBFT.uniqueID1)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }

                val uid = Uids()
                uid.UID = CoreHBBFT.uniqueID1
                uid.isOnline = true
                val ref = snapshot.ref.push()
                ref.setValue(uid)

                val uid1 = Uids()
                uid1.UID = CoreHBBFT.uniqueID1
                uid1.isOnline = false
                ref.onDisconnect().setValue(uid1)

                Log.d(CoreHBBFT.TAG, "Success setOnlineModeInRoomInFirebase ${CoreHBBFT.uniqueID1}")
            }
        })
    }

    fun getUIDsInRoomFromFirebase(RoomId: String): MutableList<Uids> {
        val ref = CoreHBBFT.mDatabase.child("RoomsUsers").child(RoomId)

        val latch = CountDownLatch(1)
        val listObjectsOfUIds: MutableList<Uids> = arrayListOf()
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val objectMap = dataSnapshot.value as HashMap<String, Any>
                for (obj in objectMap.values) {
                    val mapObj: Map<String, Any> = obj as Map<String, Any>
                    try {
                        val uids = Uids()
                        uids.UID = mapObj["uid"] as String
                        uids.isOnline = mapObj["online"] as Boolean

                        listObjectsOfUIds.add(uids)
                    } catch (e: Exception) {
                        try {
                            val objj = mapObj.values.elementAt(0) as HashMap<String, Any>

                            val uids = Uids()
                            uids.UID = objj["uid"] as String
                            uids.isOnline = objj["online"] as Boolean

                            listObjectsOfUIds.add(uids)
                        } catch (e: Exception) {

                        }
                    }
                }
                Log.d(CoreHBBFT.TAG, "getUIDsInRoomFromFirebase success")
                latch.countDown()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                latch.countDown()
                // Getting Post failed, log a message
                Log.w(CoreHBBFT.TAG, "getUIDsInRoomFromFirebase: onCancelled", databaseError.toException())
            }
        }
        ref.addListenerForSingleValueEvent(postListener)
        latch.await()

        listObjectsOfUIds.distinctBy { it.UID }
        return listObjectsOfUIds
    }

    fun isSomeBodyOnlineInList(listOfUids: List<Uids>): Boolean {
        for (uids in listOfUids) {
            if (uids.isOnline!!)
                return true
        }
        return false
    }
}