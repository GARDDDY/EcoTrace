package com.gy.ecotrace.db

import android.content.Context
import android.text.BoringLayout
import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.getValue
import com.google.firebase.database.snapshots
import com.google.firebase.database.values
import com.google.gson.Gson
import com.gy.ecotrace.Globals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.floor

object DatabaseMethods {

    class Tech {
        fun context(): Context {
            return Globals.applicationContext()
        }

        fun hash256(text: String): String {
            val bytes = text.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }

        fun saveLocally(data: Any, dType: String, objId: String) {
            try {
                val directory = context().filesDir
                val file = File(directory, "${objId}_$dType.json")
                val jsonString = Gson().toJson(data)
                FileOutputStream(file).use { out ->
                    out.write(jsonString.toByteArray())
                }
            } catch (e: IOException) {
                Log.e(dType, "Error saving")
                e.printStackTrace()
            }
        }

        fun getLocal(dType: String, objId: String): String? {
            val file = File(Tech().context().filesDir, "${objId}_$dType.json")
            if (file.exists()) {
                return file.readText()
            }
            return null
        }
    }

    class DataClasses {
        data class Country(
            var name: String = "error-country_name",
            var code: String = "error-country_code"
        )

        data class UserActivity(
            var username: String = "error-username",
            var role: Int = 0 // 0 - участник 1 - старейшна 2 - соруководитель 3 - глава
        )

        data class Friendship(
            var userId: String = "error-userId",
            var username: String = "error-username",
            var friend: Boolean = false,
            var sender: Boolean = false
        )

        data class FiltersFriendship(
            var userId: String = "error-userId",
            var username: String = "error-username",
//            var friend: Boolean = false,
            var filters: String = ""
        )

        data class Event(
            var eventId: String = "error-eventId",
            var eventName: String = "error-event_name",
            var eventAbout: String = "",
            var eventTags: HashMap<String, String>? = null,
            var eventStatus: Int = 0 /* 0 - Предстоит 1 - Проходит 2 - Закончилось*/,
            var eventCountMembers: Int = 0,
            var eventUsersToTheirNames: HashMap<String, UserActivity>? = null
        )


        data class Group(
            var groupId: String = "error-groupId",
            var groupName: String = "error-group_name",
            var groupAbout: String = "",
            var groupExperience: Int = 0,
            var groupRank: Int = 0,
            var groupTags: HashMap<String, String>? = null,
            var groupCountMembers: Int = 0,
            var groupType: Int = 0 /* 0 - открытая  1 - по заявкам  2 - закрытая*/,
            var groupUsersToTheirRoles: HashMap<String, UserActivity>? = null
        )



        companion object {
            val EventRoles = arrayOf("Глава", "Помощник", "Исполняющий")
            val UserRanks = arrayOf("Крутой", "Очень крутой")
        }
    }

    class UserDatabaseMethods {
        class UserInfo(
            var username: String = "error-username",
            var fullname: String = "error-fullname",
            var gender: Int = 0,
            var country: DataClasses.Country = DataClasses.Country(),
            var aboutMe: String = "",
            var experience: Int = 0
        )

        class UserPrivate(
            var email: String = "error-email",
            var password: String = "error-password"
        )

        class UserRules(
            var nameSeen: Int = 0 /* Все */,
            var countrySeen: Int = 0 /* Все */,
            var friendFrom: Int = 0 /* Все */
        )

        class UserActivity(
            var eventInfo: DataClasses.Event = DataClasses.Event(),
            var eventUserRole: Int = 0
        )

        class UserGroups(
            var isUserInGroup: Boolean = false,
            var groupInfo: DataClasses.Group = DataClasses.Group(),
        )


        suspend fun getMaximums(userId: String): MutableList<Int> {
            return suspendCoroutine {
                val db = FirebaseDatabase.getInstance().getReference("users/$userId")
                val maxList = mutableListOf<Int>()
                db.addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        maxList.add(snapshot.child("activities").childrenCount.toInt())
                        maxList.add(snapshot.child("friends").childrenCount.toInt())
                        maxList.add(snapshot.child("groups").childrenCount.toInt())

                        it.resume(maxList)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }


//        private fun getLocalInfo(userId: String): UserInfo? {
//            val file = File(Tech().context().filesDir, "${userId}_info.json")
//            if (file.exists()) {
//                val reader = BufferedReader(FileReader(file))
//                val stringBuilder = StringBuilder()
//                var line: String?
//                while (reader.readLine().also { line = it } != null) {
//                    stringBuilder.append(line)
//                }
//                reader.close()
//                return Gson().fromJson(stringBuilder.toString(), UserInfo::class.java)
//            }
//            return null
//        }
        suspend fun getUserInfo(userId: String): UserInfo {
//            val localInfo = getLocalInfo(userId)
//            if (localInfo != null) {
//                Log.d("UserInfoClassGET", "Got Info Local!")
//                return localInfo
//            }
            return suspendCoroutine {
                FirebaseDatabase.getInstance().getReference("users").child(userId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val data = snapshot.getValue(UserInfo::class.java)!!
                            Log.d("UserInfoClassGET", "Got Info Network!")
                            it.resume(data)
                            Tech().saveLocally(data, "info", userId)
                        } else {
                            it.resume(UserInfo())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }

        fun observeUserInfo(userId: String, callback: (UserInfo?) -> Unit) {
            val database: DatabaseReference =
                FirebaseDatabase.getInstance().getReference("users").child(userId)
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val data = snapshot.getValue(UserInfo::class.java)
                        callback(data)
                    } else {
                        callback(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }


        // private


        // rules


        private fun getLocalEvents(userId: String): MutableList<UserActivity>? {
            val file = File(Tech().context().filesDir, "${userId}_events.json")
            if (file.exists()) {
                val reader = BufferedReader(FileReader(file))
                val stringBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                reader.close()
                return Gson().fromJson(
                    stringBuilder.toString(),
                    object : TypeToken<MutableList<UserActivity>>() {}.type
                )
            }
            return null
        }

        suspend fun getUserEvents(userId: String): MutableList<UserActivity> {
            val localEvents = getLocalEvents(userId)
            if (localEvents != null) {
                Log.d("UserEventClassGET", "Got Info Local!")
                return localEvents
            }
            return suspendCoroutine {
                val database: DatabaseReference =
                    FirebaseDatabase.getInstance().getReference("users/$userId/activities")
                database.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val allActs = mutableListOf<UserActivity>()
                        val scope = CoroutineScope(Dispatchers.IO)
                        val tasks = snapshot.children.map { act ->
                            scope.async {
                                val currentAct = UserActivity()
                                currentAct.eventUserRole =
                                    act.child("eventUserRole").getValue(Int::class.java)!!
                                val events: DatabaseReference =
                                    FirebaseDatabase.getInstance()
                                        .getReference("events/${act.key.toString()}")
                                val eventSnapshot = events.get().await()
                                val eventData =
                                    eventSnapshot.getValue(DataClasses.Event::class.java)
                                currentAct.eventInfo = eventData!!
                                currentAct.eventInfo.eventId = act.key.toString()
                                currentAct
                            }
                        }
                        scope.launch {
                            val results = tasks.awaitAll()
                            allActs.addAll(results)
                            it.resume(allActs)
                            Tech().saveLocally(allActs, "events", userId)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }


//        private fun getLocalFriends(userId: String): MutableList<Pair<String, String>>? {
//            // получить userId, потом локально проверить userinfo(userid)
//            val file = File(Tech().context().filesDir, "${userId}_friends.json")
//            if (file.exists()) {
//                val reader = BufferedReader(FileReader(file))
//                val stringBuilder = StringBuilder()
//                var line: String?
//                while (reader.readLine().also { line = it } != null) {
//                    stringBuilder.append(line)
//                }
//                reader.close()
//                return Gson().fromJson(
//                    stringBuilder.toString(),
//                    object : TypeToken<MutableList<Pair<String, String>>>() {}.type
//                )
//            }
//            return null
//        }

        suspend fun getUserFriends(userId: String): MutableList<DataClasses.Friendship> {
//            val localFriends = getLocalFriends(userId)
//            if (localFriends != null) {
//                Log.d("UserFriendsClassGET", "Got Info Local!")
//                return localFriends
//            }
            return suspendCoroutine {
                val database: DatabaseReference =
                    FirebaseDatabase.getInstance().getReference("users/$userId/friends")
                database.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val friendsList = mutableListOf<DataClasses.Friendship>()
                        for (friendId in snapshot.children) {
                            val friendClass = friendId.getValue(DataClasses.Friendship::class.java)!!
                            friendClass.userId = friendId.key.toString()
                            friendsList.add(friendClass)
                        }
                        it.resume(friendsList)
                        Tech().saveLocally(friendsList, "friends", userId)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }

        fun removeFriends(userId1: String, userId2: String) {
            val db = FirebaseDatabase.getInstance().getReference("users")
            db.child("$userId1/friends/$userId2").removeValue()
            db.child("$userId2/friends/$userId1").removeValue()
        }
        fun addFriends(userId1: String, userId2: String) {
            val username2 = Gson().fromJson(Tech().getLocal("info", userId2), UserInfo::class.java).username
            val username1 = Gson().fromJson(Tech().getLocal("info", userId1), UserInfo::class.java).username
            val db = FirebaseDatabase.getInstance().getReference("users")
            db.child("$userId1/friends/$userId2").setValue(
                DataClasses.Friendship(userId2, username2, false, true)
            )
            db.child("$userId2/friends/$userId1").setValue(
                DataClasses.Friendship(userId2, username1, false, false)
            )
        }

        suspend fun getUserGroups(userId: String): MutableMap<String, Boolean> {
            return suspendCoroutine {
                val localGroups = Tech().getLocal("groups", userId)
                var groupIds = mutableMapOf<String, Boolean>()

                if (localGroups != null) {
                    groupIds = Gson().fromJson(
                        localGroups,
                        object : TypeToken<MutableMap<String, Boolean>>() {}.type
                    )
                    it.resume(groupIds)
                    Tech().saveLocally(groupIds, "groups", userId)
                } else {
                    FirebaseDatabase.getInstance().getReference("users/$userId/groups")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (gr in snapshot.children) {
                                    groupIds[gr.key.toString()] =
                                        gr.getValue(Boolean::class.java)!!
                                }
                                it.resume(groupIds)
                                Tech().saveLocally(groupIds, "groups", userId)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                    }
                }
            }
        }


    class ApplicationDatabaseMethods {
        suspend fun getEvent(eventId: String): DataClasses.Event {
            return suspendCoroutine {
                FirebaseDatabase.getInstance().getReference("events/$eventId")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val eventData = snapshot.getValue(DataClasses.Event::class.java)!!
                        it.resume(eventData)
                        Tech().saveLocally(eventData, "event", eventId)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }

        suspend fun getGroup(groupId: String): DataClasses.Group {
            return suspendCoroutine {
                val databaseReference = FirebaseDatabase.getInstance().getReference("groups/$groupId")
                databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val groupData = snapshot.getValue(DataClasses.Group::class.java)!!
                            groupData.groupId = groupId
                            it.resume(groupData)
                            Tech().saveLocally(groupData, "GROUP", groupId)
                        } else {
                            it.resume(DataClasses.Group())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
        fun observeGroup(groupId: String, callback: (DataClasses.Group) -> Unit) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("groups/$groupId")
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val saved = Gson().fromJson(Tech().getLocal("GROUP", groupId), DataClasses.Group::class.java)
                    if (snapshot.exists()) {
                        val membersCount = snapshot.child("groupCountMembers").getValue(Int::class.java)!!
                        val usersHashMap = snapshot.child("groupUsersToTheirRoles").getValue(object : GenericTypeIndicator<HashMap<String, DataClasses.UserActivity>>() {})!!
                        saved.groupCountMembers = membersCount
                        saved.groupUsersToTheirRoles = usersHashMap
                        callback(saved)
                    } else {
                        callback(saved)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        suspend fun findUsersWithFilters(filters: MutableList<Int>, lastUserId: String?): Pair<String?, MutableList<DataClasses.FiltersFriendship>> {
            return suspendCoroutine {
                val foundUsers = mutableListOf<DataClasses.FiltersFriendship>()
                val reference = FirebaseDatabase.getInstance().getReference("users")
                val query = if (lastUserId != null) {
                    reference.orderByKey().startAt(lastUserId)
                } else {
                    reference.orderByKey()
                }
                query.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.children.last().key != lastUserId){
                                for (user in snapshot.children) {
                                    if (user.key == lastUserId) continue
                                    val userFilters =
                                        user.child("filters").getValue(String::class.java)!!
                                            .split(",").map { it.toInt() - 1 }
                                    val userFiltersSame =
                                        userFilters.intersect(filters.toSet()).toMutableList()

                                    if (filters == userFiltersSame) {//if (filters.size - userFiltersSame.size <= floor(filters.size / 2.0)) {
                                        val userClass =
                                            user.getValue(DataClasses.FiltersFriendship::class.java)!!
                                        userClass.userId = user.key.toString()
                                        foundUsers.add(userClass)
                                    }

                                    if (foundUsers.size == 4) {
                                        it.resume(Pair(user.key.toString(), foundUsers))
                                        return
                                    }

                                }
                            }
                            it.resume(Pair(snapshot.children.last().key.toString(), foundUsers))
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }

    }
}
