package net.korul.hbbft.CoreHBBFT

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.*
import java.util.concurrent.CountDownLatch

object RoomWork {

    fun reregisterInFirebase(ListDialogsName: List<String>, uid: String) {
        for (dialogName in ListDialogsName) {
            val queryRef = CoreHBBFT.mDatabase.child("Rooms").child(dialogName).orderByChild("uid").equalTo(uid)
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

                    Log.d(CoreHBBFT.TAG, "Succes reregisterInFirebase ${CoreHBBFT.uniqueID1}")
                }
            })
        }
    }


    fun unregisterInRoomInFirebase(RoomName: String) {
        val queryRef = CoreHBBFT.mDatabase.child("Rooms").child(RoomName).orderByChild("uid").equalTo(CoreHBBFT.uniqueID1)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    for (postsnapshot in snapshot.children) {
                        postsnapshot.key
                        postsnapshot.ref.removeValue()
                    }
                    Log.d(CoreHBBFT.TAG, "Succes unregisterInRoomInFirebase ${CoreHBBFT.uniqueID1}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    fun registerInRoomInFirebase(RoomName: String) {
        val queryRef = CoreHBBFT.mDatabase.child("Rooms").child(RoomName).orderByChild("uid").equalTo(CoreHBBFT.uniqueID1)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    for (postsnapshot in snapshot.children) {
                        postsnapshot.key
                        postsnapshot.ref.removeValue()
                    }
                    Log.d(CoreHBBFT.TAG, "Succes unregisterInRoomInFirebase ${CoreHBBFT.uniqueID1}")

                    // Add user
                    val uid = Uids()
                    uid.UID = CoreHBBFT.uniqueID1
                    uid.isOnline = false
                    val ref = CoreHBBFT.mDatabase.child("Rooms").child(RoomName)
                    ref.push().setValue(uid).addOnSuccessListener {
                        Log.d(CoreHBBFT.TAG, "Succes registerInRoomInFirebase ${CoreHBBFT.uniqueID1}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    fun setOfflineModeInRoomInFirebase(RoomName: String) {
        val queryRef = CoreHBBFT.mDatabase.child("Rooms").child(RoomName).orderByChild("uid").equalTo(CoreHBBFT.uniqueID1)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val uid = Uids()
                uid.UID = CoreHBBFT.uniqueID1
                uid.isOnline = false
                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }
                snapshot.ref.push().setValue(uid)
                Log.d(CoreHBBFT.TAG, "Succes setOnlineModeInRoomInFirebase ${CoreHBBFT.uniqueID1}")
            }
        })
    }

    fun setOnlineModeInRoomInFirebase(RoomName: String) {
        val queryRef = CoreHBBFT.mDatabase.child("Rooms").child(RoomName).orderByChild("uid").equalTo(CoreHBBFT.uniqueID1)
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

                Log.d(CoreHBBFT.TAG, "Succes setOnlineModeInRoomInFirebase ${CoreHBBFT.uniqueID1}")
            }
        })
    }

    fun getUIDsInRoomFromFirebase(RoomName: String): MutableList<Uids> {
        val ref = CoreHBBFT.mDatabase.child("Rooms").child(RoomName)

        val latch = CountDownLatch(1)
        val listObjectsOfUIds: MutableList<Uids> = arrayListOf()
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val objectMap = dataSnapshot.value as HashMap<String, Any>
                for (obj in objectMap.values) {
                    val mapObj: Map<String, Any> = obj as Map<String, Any>
                    val uids = Uids()
                    uids.UID = mapObj["uid"] as String
                    uids.isOnline = mapObj["online"] as Boolean

                    listObjectsOfUIds.add(uids)
                }
                try {
                    latch.countDown()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(CoreHBBFT.TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        ref.addValueEventListener(postListener)
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