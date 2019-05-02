package net.korul.hbbft.CoreHBBFT

import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.raizlabs.android.dbflow.config.FlowManager
import net.korul.hbbft.CommonData.data.model.User
import net.korul.hbbft.CommonData.data.model.conversation.Conversations
import net.korul.hbbft.CommonData.data.model.core.Getters
import net.korul.hbbft.CommonData.data.model.core.Getters.getAllLocalOfflineUsers
import net.korul.hbbft.CommonData.data.model.coreDataBase.AppDatabase
import net.korul.hbbft.CoreHBBFT.FileUtil.ReadObjectFromFile
import net.korul.hbbft.CoreHBBFT.FileUtil.WriteObjectToFile
import net.korul.hbbft.DatabaseApplication
import net.korul.hbbft.FirebaseStorageDU.MyDownloadUserService
import net.korul.hbbft.FirebaseStorageDU.MyGetLastModificationUserService
import java.io.File
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread

object UserWork {

    fun initCurUser() {
        val strUser = ReadObjectFromFile("cur_user.out")
        if (strUser == null || strUser.isEmpty()) {
            val user = User(
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
            try {
                Conversations.getDUser(user).insert()
            } catch (e: Exception) {
                FlowManager.getDatabase(AppDatabase::class.java)
                    .reset(DatabaseApplication.mCoreHBBFT2X.mApplicationContext)
                Conversations.getDUser(user).insert()
            }
        } else
            DatabaseApplication.mCurUser = Gson().fromJson(strUser, User::class.java)
    }

    fun saveCurUser(user: User) {
        val saveuser = Gson().toJson(user).toString()
        WriteObjectToFile(saveuser, "cur_user.out")

        DatabaseApplication.mCurUser = user

        Getters.updateMetaUserbyUID(CoreHBBFT.uniqueID1, user)
        insertOrUpdateUserInFirebase(user)
    }

    fun saveCurUserSync(user: User) {
        val saveuser = Gson().toJson(user).toString()
        WriteObjectToFile(saveuser, "cur_user.out")

        DatabaseApplication.mCurUser = user

        Getters.updateMetaUserbyUID(CoreHBBFT.uniqueID1, user)

        Log.d(CoreHBBFT.TAG, "saveCurUserSync")
        val semaphore = insertOrUpdateUserInFirebase(user)
        semaphore.acquire()

        Log.d(CoreHBBFT.TAG, "saveCurUserSync: insertOrUpdateUserInFirebase")
    }

    fun insertOrUpdateUserInFirebase(user: User): Semaphore {
        val semaphore = Semaphore(1)
        val queryRef = CoreHBBFT.mDatabase.child("Users").child(user.uid).orderByChild("uid").equalTo(user.uid)
        queryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                semaphore.release()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                for (postsnapshot in snapshot.children) {
                    postsnapshot.key
                    postsnapshot.ref.removeValue()
                }

                val userToSave = Users(
                    user.uid,
                    user.isOnline,
                    user.name,
                    user.nick
                )

                val ref = snapshot.ref.push()
                ref.setValue(userToSave)
                Log.d(CoreHBBFT.TAG, "Success insertOrUpdateUserInFirebase ${CoreHBBFT.uniqueID1}")

                if(user.uid == CoreHBBFT.uniqueID1) {
                    val userToSave = Users(
                        user.uid,
                        false,
                        user.name,
                        user.nick
                    )
                    ref.onDisconnect().setValue(userToSave)
                }

                semaphore.release()
            }
        })
        return semaphore
    }

    private fun getUsersFromFirebase(uid: String): MutableList<Users> {
        val ref = CoreHBBFT.mDatabase.child("Users").child(uid)

        val semaphore = Semaphore(0)
        val listObjectsOfUsers: MutableList<Users> = arrayListOf()
        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val objectMap = dataSnapshot.value as HashMap<String, Any>?
                if (objectMap != null) {
                    for (obj in objectMap.values) {
                        val mapObj: Map<String, Any> = obj as Map<String, Any>

                        val user = Users()
                        user.UID = mapObj["uid"] as String
                        user.isOnline = mapObj["online"] as Boolean
                        user.name = mapObj["name"] as String
                        user.nick = mapObj["nick"] as String

                        listObjectsOfUsers.add(user)
                    }
                    semaphore.release()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                semaphore.release()
                // Getting Post failed, log a message
                Log.w(CoreHBBFT.TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        ref.addListenerForSingleValueEvent(postListener)
        semaphore.acquire()

        listObjectsOfUsers.distinctBy { it.UID }
        return listObjectsOfUsers
    }

    private fun AddUserToLocalFromFirebaseWithAvatar(
        uid: String,
        dialogId: String,
        listObjectsOfUsers: MutableList<Users>,
        listener: IAddToContacts
    ) {
        if (listObjectsOfUsers.isEmpty()) {
            listener.errorAddContact()
        } else {
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

                // Kick off MyDownloadUserService to download the file
                val intent = Intent(CoreHBBFT.mApplicationContext, MyDownloadUserService::class.java)
                    .putExtra(MyDownloadUserService.EXTRA_DOWNLOAD_USERID, user.UID)
                    .setAction(MyDownloadUserService.ACTION_DOWNLOAD)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CoreHBBFT.mApplicationContext.startForegroundService(intent)
                } else {
                    CoreHBBFT.mApplicationContext.startService(intent)
                }
//                CoreHBBFT.mApplicationContext.startService(intent)
            }
        }

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
        listener.user(user)
    }

    fun getUserFromLocalOrDownloadFromFirebase(uid: String, listener: IAddToContacts) {
        getUserFromLocalOrDownloadFromFirebase(uid, "", object : IAddToContacts {
            override fun user(user: User) {
                listener.user(user)
            }

            override fun errorAddContact() {
                Toast.makeText(CoreHBBFT.mApplicationContext, "ERROR Adding Contact", Toast.LENGTH_LONG).show()
            }
        })
    }

    fun getUserFromLocalOrDownloadFromFirebase(uid: String, dialogId: String, listener: IAddToContacts) {
        val localUser = getAnyLocalUserByUid(uid)
        if (localUser != null) {
            val id = Getters.getNextUserID()
            val user = User(
                id,
                uid,
                id.toString(),
                dialogId,
                localUser.name,
                localUser.nick,
                localUser.avatar,
                localUser.isOnline
            )

            listener.user(user)
        } else {
            thread {
                val listOfUSer = getUsersFromFirebase(uid)
                AddUserToLocalFromFirebaseWithAvatar(uid, dialogId, listOfUSer, object : IAddToContacts {
                    override fun user(user: User) {
                        listener.user(user)
                    }

                    override fun errorAddContact() {
                        listener.errorAddContact()
                    }
                })
            }
        }
    }

    fun updateOnlineInAllLocalUserByUid(uid: String, online: Boolean) {
        for (user in Getters.getAllLocalUsers(uid)) {
            if (user.uid == uid) {
                val us = User(
                    user.id_,
                    user.uid,
                    user.id,
                    user.idDialog,
                    user.name,
                    user.nick,
                    user.avatar,
                    online
                )

                Conversations.getDUser(us).update()
            }
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

    fun updateMetaInAllLocalUserByUidWithoutNick(userMeta: Users) {
        for (user in Getters.getAllLocalUsers(userMeta.UID!!)) {
            if (user.uid == userMeta.UID) {
                val us = User(
                    user.id_,
                    user.uid,
                    user.id,
                    user.idDialog,
                    userMeta.name!!,
                    user.nick,
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

    fun setUnOnlineUserWithUID(uid: String, online: Boolean) {
        getAllLocalOfflineUsers(uid, online)
    }


    fun updateAllUsersFromFirebase() {
        Log.d(CoreHBBFT.TAG, "updateAllUsersFromFirebase")
        for (user in Getters.getAllLocalUsersDistinct().filter { it.uid != CoreHBBFT.uniqueID1 }) {
            val listObjectsOfUsers: MutableList<Users> = getUsersFromFirebase(user.uid)
            if (listObjectsOfUsers.isEmpty())
                Toast.makeText(
                    CoreHBBFT.mApplicationContext,
                    "ERROR Adding Contact with uid ${user.uid}",
                    Toast.LENGTH_LONG
                ).show()
            else {
                for (us in listObjectsOfUsers) {

                    updateMetaInAllLocalUserByUidWithoutNick(us)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        CoreHBBFT.mApplicationContext.startForegroundService(
                            Intent(CoreHBBFT.mApplicationContext, MyGetLastModificationUserService::class.java)
                                .putExtra(MyGetLastModificationUserService.EXTRA_COMPARE_UID, us.UID)
                                .setAction(MyGetLastModificationUserService.ACTION_COMPARE)
                        )
                    } else {
                        CoreHBBFT.mApplicationContext.startService(
                            Intent(CoreHBBFT.mApplicationContext, MyGetLastModificationUserService::class.java)
                            .putExtra(MyGetLastModificationUserService.EXTRA_COMPARE_UID, us.UID)
                            .setAction(MyGetLastModificationUserService.ACTION_COMPARE)
                        )
                    }

//                    CoreHBBFT.mApplicationContext.startService(
//                        Intent(CoreHBBFT.mApplicationContext, MyGetLastModificationUserService::class.java)
//                            .putExtra(MyGetLastModificationUserService.EXTRA_COMPARE_UID, us.UID)
//                            .setAction(MyGetLastModificationUserService.ACTION_COMPARE)
//                    )
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
