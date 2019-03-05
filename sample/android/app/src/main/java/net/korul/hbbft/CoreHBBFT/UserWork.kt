package net.korul.hbbft.CoreHBBFT

import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import net.korul.hbbft.CommonFragments.tabContacts.IAddToContacts
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.common.data.model.User
import net.korul.hbbft.common.data.model.conversation.Conversations
import net.korul.hbbft.common.data.model.core.Getters
import net.korul.hbbft.firebaseStorage.MyDownloadService
import java.io.File
import java.util.HashMap
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

object UserWork {

    fun initCurUser() {
        val prefs = CoreHBBFT.mApplicationContext.getSharedPreferences("cur_user", Application.MODE_PRIVATE)
        val strUser = prefs!!.getString("current_user","")
        if (strUser == null || strUser.isEmpty()) {
            val user = User (
                0,
                DatabaseApplication.mCoreHBBFT2X.uniqueID1,
                0.toString(),
                "",
                "name",
                "nick",
                "",
                true
            )

            DatabaseApplication.mCurUser = user
            Conversations.getDUser(user).insert()
        } else
            DatabaseApplication.mCurUser = Gson().fromJson(strUser, User::class.java)
    }

    fun saveCurUser(user: User) {
        val prefs = CoreHBBFT.mApplicationContext.getSharedPreferences("cur_user", Application.MODE_PRIVATE)
        val saveuser = Gson().toJson(user).toString()
        prefs.edit().putString("current_user", saveuser).apply()

        DatabaseApplication.mCurUser = user

        Getters.updateMetaUserbyUID(CoreHBBFT.uniqueID1, user)
        insertOrUpdateUserInFirebase(user)
    }

    fun saveCurUserSync(user: User) {
        val prefs = CoreHBBFT.mApplicationContext.getSharedPreferences("cur_user", Application.MODE_PRIVATE)
        val saveuser = Gson().toJson(user).toString()
        prefs.edit().putString("current_user", saveuser).apply()

        DatabaseApplication.mCurUser = user

        Getters.updateMetaUserbyUID(CoreHBBFT.uniqueID1, user)
        val latch = insertOrUpdateUserInFirebase(user)
        latch.await()
    }

    fun insertOrUpdateUserInFirebase(user: User): CountDownLatch {
        val latch = CountDownLatch(1)
        val queryRef = CoreHBBFT.mDatabase.child("Users").child(user.uid).orderByChild("uid").equalTo(user.uid)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                latch.countDown()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val userToSave = Users(
                    user.uid,
                    user.isOnline,
                    user.name,
                    user.nick
                )

                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }
                snapshot.ref.push().setValue(userToSave)
                Log.d(CoreHBBFT.TAG, "Succes insertOrUpdateUserInFirebase ${CoreHBBFT.uniqueID1}")
                latch.countDown()
            }
        })
        return latch
    }

    private fun getUsersFromFirebase(uid: String): MutableList<Users> {
        val ref = CoreHBBFT.mDatabase.child("Users").child(uid)

        val latch = CountDownLatch(1)
        val listObjectsOfUsers: MutableList<Users> = arrayListOf()
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val objectMap = dataSnapshot.value as HashMap<String, Any>?
                if(objectMap != null) {
                    for (obj in objectMap.values) {
                        val mapObj: Map<String, Any> = obj as Map<String, Any>

                        val user = Users()
                        user.UID = mapObj["uid"] as String
                        user.isOnline = mapObj["online"] as Boolean
                        user.name = mapObj["name"] as String
                        user.nick = mapObj["nick"] as String

                        listObjectsOfUsers.add(user)
                    }
                    try {
                        latch.countDown()
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(CoreHBBFT.TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        ref.addValueEventListener(postListener)
        latch.await()

        listObjectsOfUsers.distinctBy { it.UID }
        return listObjectsOfUsers
    }

    fun AddUserToLocalFromFirebaseWithAvatar(uid: String, listener: IAddToContacts) {
        val latch = CountDownLatch(1)
        thread {
            val listObjectsOfUsers: MutableList<Users> = getUsersFromFirebase(uid)
            if (listObjectsOfUsers.isEmpty()) {
                listener.errorAddContact()
            }
            else {
                for (user in listObjectsOfUsers) {
                    val id = Getters.getNextUserID()
                    val users = User(
                        id,
                        uid,
                        id.toString(),
                        "",
                        user.name!!,
                        user.nick!!,
                        "",
                        user.isOnline!!
                    )
                    Conversations.getDUser(users).insert()

                    // Kick off MyDownloadService to download the file
                    val intent = Intent(CoreHBBFT.mApplicationContext, MyDownloadService::class.java)
                        .putExtra(MyDownloadService.EXTRA_DOWNLOAD_USERID, user.UID)
                        .setAction(MyDownloadService.ACTION_DOWNLOAD)
                    CoreHBBFT.mApplicationContext.startService(intent)
                }
            }
            latch.countDown()
        }
        latch.await()
    }

    fun getUserFromLocalOrDownloadFromFirebase(uid: String): User {
        return getUserFromLocalOrDownloadFromFirebase(uid, "", object : IAddToContacts {
            override fun errorAddContact() {
                Toast.makeText(CoreHBBFT.mApplicationContext, "ERROR Adding Contact", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun getUserFromLocalOrDownloadFromFirebase(uid: String, listener: IAddToContacts): User {
        return getUserFromLocalOrDownloadFromFirebase(uid, "", object : IAddToContacts {
            override fun errorAddContact() {
                listener.errorAddContact()
            }
        })
    }

    fun getUserFromLocalOrDownloadFromFirebase(uid: String, idDialog: String): User {
        return getUserFromLocalOrDownloadFromFirebase(uid, idDialog, object : IAddToContacts {
            override fun errorAddContact() {
                Toast.makeText(CoreHBBFT.mApplicationContext, "ERROR Adding Contact", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun getUserFromLocalOrDownloadFromFirebase(uid: String, dialogId: String, listener: IAddToContacts): User {
        val LocalUser = getAnyLocalUserByUid(uid)
        if (LocalUser != null) {
            val id = Getters.getNextUserID()
            val user = User(
                id,
                uid,
                id.toString(),
                dialogId,
                LocalUser.name,
                LocalUser.nick,
                LocalUser.avatar,
                LocalUser.isOnline
            )

            return user
        } else {
            AddUserToLocalFromFirebaseWithAvatar(uid, object : IAddToContacts {
                override fun errorAddContact() {
                    listener.errorAddContact()
                }
            })
            val User = getAnyLocalUserByUid(uid)
            val id = Getters.getNextUserID()
            val user = User(
                id,
                uid,
                id.toString(),
                dialogId,
                User!!.name,
                User.nick,
                User.avatar,
                User.isOnline
            )

            return user
        }
    }

    fun updateAvatarInAllLocalUserByUid(uid: String, avatarFile: File) {
        for (user in Getters.getAllLocalUsers(uid)) {
            if (user.uid == uid) {
                val us = User(
                    user.id_,
                    user.uid,
                    user.id,
                    user.idDialog,
                    user.name,
                    user.nick,
                    avatarFile.path,
                    user.isOnline
                )

                Conversations.getDUser(us).update()
            }
        }
    }

    fun updateMetaInAllLocalUserByUid(userMeta: Users) {
        for (user in Getters.getAllLocalUsers(userMeta.UID!!)) {
            if (user.uid == userMeta.UID) {
                val us = User(
                    user.id_,
                    user.uid,
                    user.id,
                    user.idDialog,
                    userMeta.name!!,
                    userMeta.nick!!,
                    user.avatar,
                    userMeta.isOnline!!
                )

                Conversations.getDUser(us).update()
            }
        }
    }

    fun getAnyLocalUserByUid(uid: String): User? {
        for (user in Getters.getAllLocalUsersDistinct()) {
            if (user.uid == uid)
                return user
        }
        return null
    }

    fun updateAllUsersFromFirebase() {
        for (user in Getters.getAllLocalUsersDistinct()) {
            val listObjectsOfUsers: MutableList<Users> = getUsersFromFirebase(user.uid)
            if (listObjectsOfUsers.isEmpty())
                Toast.makeText(CoreHBBFT.mApplicationContext, "ERROR Adding Contact with uid ${user.uid}", Toast.LENGTH_LONG).show()
            else {
                for (us in listObjectsOfUsers) {

                    updateMetaInAllLocalUserByUid(us)

                    // Kick off MyDownloadService to download the file
                    val intent = Intent(CoreHBBFT.mApplicationContext, MyDownloadService::class.java)
                        .putExtra(MyDownloadService.EXTRA_DOWNLOAD_USERID, us.UID)
                        .setAction(MyDownloadService.ACTION_DOWNLOAD)
                    CoreHBBFT.mApplicationContext.startService(intent)
                }
            }
        }
    }


    fun unregisterUserInFirebase(uid: String) {
        val queryRef = CoreHBBFT.mDatabase.child("Users").child(uid).orderByChild("uid").equalTo(uid)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }
                Log.d(CoreHBBFT.TAG, "Succes unregisterUserInFirebase ${CoreHBBFT.uniqueID1}")
            }
        })
    }

}
