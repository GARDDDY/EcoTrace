package com.gy.ecotrace.db

import android.content.Context
import android.provider.ContactsContract.Data
import android.text.BoringLayout
import android.util.Log
import com.google.common.reflect.TypeToken
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Exclude
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.getValue
import com.google.firebase.database.snapshots
import com.google.firebase.database.values
import com.google.gson.Gson
import com.gy.ecotrace.Globals
import com.yandex.mapkit.geometry.Point
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
import kotlin.reflect.full.memberProperties

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
            var role: Int = 0 // 0 - участник 1 - старейшна 2 - соруководитель 3 - глава // todo
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
            @get:Exclude var eventId: String = "error-eventId",
            var eventName: String = "-",
            var eventAbout: String = "",
            var eventTags: String = "",
            var eventStatus: Int = 0 /* 0 - Предстоит 1 - Проходит 2 - Закончилось*/,
            var eventCountMembers: Int = 0,
            var eventUsersToTheirRoles: HashMap<String, Int>? = null,
            var eventStart: String = "0;0",
            var eventCreatorId: String = "",
            @get:Exclude var eventCreatorName: String = "",
            var filters: String = ""
        )

        data class Group(
            @get:Exclude var groupId: String = "error-groupId",
            var groupName: String = "-",
            var groupAbout: String = "",
            var groupExperience: Int = 0,
            var groupRank: Int = 0,
            var groupTags: HashMap<String, String>? = null,
            var groupCountMembers: Int = 0,
            var groupType: Int = 0 /* 0 - открытая  1 - по заявкам  2 - закрытая*/,
            var groupUsersToTheirRoles: HashMap<String, UserActivity>? = null
        )

        data class ObjectRelation(
            var isWithGoal: Boolean = false,
            var relationValue: Int = -1
        )

        data class MapObject(
            var objectName: String = "",
            var objectType: Int = 0, // 0 - Circle  1 - Area  2 - Dot
            var objectRelation: ObjectRelation = ObjectRelation(),
            var objectCenter: Point = Point(),

            var fillColor: String = "#00000000",
            var strokeColor: String = "#00000000",

            var circleRadius: Float? = null,


        )



        companion object {
            val EventRoles = arrayOf("Глава", "Помощник", "Исполняющий")
            val UserRanks = arrayOf("Крутой", "Очень крутой")
            val UserFiltersSearchBy = arrayOf(
                Pair("Активный", "\"Активный\" пользователь всегдат\nготов присоединиться к новым мероприятиям"),
                Pair("Веселый", "\"Веселый\" пользователь"),
                Pair("Ветеран", "Пользователь, зарегистрировавшийся довольно давно (награда)"),
                Pair("В сети", "Пользователь часто бывает в сети"))
            val EventFiltersSearchBy = arrayOf(
                Pair("На улице", "Часть или все мероприятие проходит на улице"),
                Pair("В помещении", "Часть или все мероприятие проходит в помещении")
            )

            val filterColors = arrayOf(
                Pair("#00FA9A", "#1A3329"), // main, text
                Pair("#FF7F50", "#33201A"),
                Pair("#00CED1", "#1A3333"),
                Pair("#DA70D6", "#331A32"),
                Pair("", ""),
                Pair("", ""),
            )
        }
    }

    class UserDatabaseMethods {
        class UserInfo(
            var username: String = "-",
            var fullname: String = "-",
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
            var isUserInEvent: Boolean = false
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

        suspend fun getUsernameOnly(userId: String): String {
            return FirebaseDatabase.getInstance().getReference("users/$userId/username").get().await().getValue(String::class.java) ?: ""
        }

        suspend fun joinEvent(eventId: String, userId: String) {
            val userEvent = FirebaseDatabase.getInstance().getReference("users/$userId/events")
            userEvent.child(eventId).setValue(true)
            val event = FirebaseDatabase.getInstance().getReference("events/$eventId")
            val currentCount = event.child("eventCountMembers").get().await().getValue(Int::class.java)!!
            if (!event.child("eventUsersToTheirRoles").get().await()
                .getValue(object : GenericTypeIndicator<HashMap<String, Int>>() {})!!.contains(userId)) {
                    event.child("eventCountMembers").setValue(currentCount + 1)
                    event.child("eventUsersToTheirRoles/$userId")
                        .setValue(DataClasses.EventRoles.size - 1)
            }
        }
        suspend fun leaveEvent(eventId: String, userId: String) {
            val userEvent = FirebaseDatabase.getInstance().getReference("users/$userId/events")
            userEvent.child(eventId).removeValue()
            val event = FirebaseDatabase.getInstance().getReference("events/$eventId")
            val currentCount = event.child("eventCountMembers").get().await().getValue(Int::class.java)!!
            if (event.child("eventUsersToTheirRoles").get().await()
                .getValue(object : GenericTypeIndicator<HashMap<String, Int>>() {})!!.contains(userId)) {
                    event.child("eventCountMembers").setValue(currentCount - 1)
                    event.child("eventUsersToTheirRoles/$userId").removeValue()
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

        suspend fun getUserEvents(userId: String): MutableMap<String, Boolean> {
            return suspendCoroutine {
                val localEvents = Tech().getLocal("events", userId)
                var eventIds = mutableMapOf<String, Boolean>()
                if (localEvents != null) {
                    eventIds = Gson().fromJson(
                        localEvents,
                        object : TypeToken<MutableMap<String, Boolean>>() {}.type
                    )
                    it.resume(eventIds)
                    Tech().saveLocally(eventIds, "events", userId)
                } else {
                    FirebaseDatabase.getInstance().getReference("users/$userId/events")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (ev in snapshot.children) {
                                    eventIds[ev.key.toString()] =
                                        ev.getValue(Boolean::class.java)!!
                                }
                                it.resume(eventIds)
                                Tech().saveLocally(eventIds, "events", userId)
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }
        }

        suspend fun getUserFriends(userId: String): MutableList<DataClasses.Friendship> {
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
        class EventMore(
            var eventGoals: MutableMap<String, String> = mutableMapOf(),
            var eventTimes: MutableMap<String, String> = mutableMapOf(),
            var eventCoords: MutableMap<String, DataClasses.MapObject> = mutableMapOf()
        )


        suspend fun getEvent(eventId: String): DataClasses.Event {
            return suspendCoroutine {
                FirebaseDatabase.getInstance().getReference("events/$eventId")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            Log.d("AAtest", snapshot.toString())
                            val eventData = snapshot.getValue(DataClasses.Event::class.java)!!
                            Log.d("AAtest", eventData.toString())
                            eventData.eventId = eventId
                            it.resume(eventData)
                            Tech().saveLocally(eventData, "EVENT", eventId)
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }

        fun observeEvent(eventId: String, callback: (DataClasses.Event) -> Unit) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("events/$eventId")
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val saved = Gson().fromJson(
                        Tech().getLocal("EVENT", eventId),
                        DataClasses.Event::class.java
                    )
                    Log.d("xxx", saved.toString())
                    if (snapshot.exists()) {
                        val membersCount =
                            snapshot.child("eventCountMembers").getValue(Int::class.java)!!
                        val usersHashMap = snapshot.child("eventUsersToTheirRoles")
                            .getValue(object :
                                GenericTypeIndicator<HashMap<String, Int>>() {})!!
                        val eventStatus = snapshot.child("eventStatus").getValue(Int::class.java)!!
                        saved.eventCountMembers = membersCount
                        saved.eventUsersToTheirRoles = usersHashMap
                        saved.eventStatus = eventStatus
                        callback(saved)
                    } else {
                        callback(saved)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        suspend fun getEventMore(eventId: String): EventMore {
            return suspendCoroutine {
                FirebaseDatabase.getInstance().getReference("events/$eventId")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var snapshotEM = EventMore()
                            if (snapshot.exists()) {
                                snapshotEM = snapshot.getValue(EventMore::class.java)!!
                            }
                            it.resume(snapshotEM)
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }

        suspend fun getGroup(groupId: String): DataClasses.Group {
            return suspendCoroutine {
                val databaseReference =
                    FirebaseDatabase.getInstance().getReference("groups/$groupId")
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
                    val saved = Gson().fromJson(
                        Tech().getLocal("GROUP", groupId),
                        DataClasses.Group::class.java
                    )
                    if (snapshot.exists()) {
                        val membersCount =
                            snapshot.child("groupCountMembers").getValue(Int::class.java)!!
                        val usersHashMap = snapshot.child("groupUsersToTheirRoles")
                            .getValue(object :
                                GenericTypeIndicator<HashMap<String, DataClasses.UserActivity>>() {})!!
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

        suspend fun findUsersWithFilters(
            filters: MutableList<Int>,
            lastUserId: String?
        ): Pair<String?, MutableList<DataClasses.FiltersFriendship>> {
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
                        if (snapshot.children.last().key != lastUserId) {
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

        suspend fun createEvent(event: DataClasses.Event, eventmore: EventMore): String {
            return suspendCoroutine {
                val eventsChild = FirebaseDatabase.getInstance().getReference("events")
                val unique = eventsChild.push().key!!
                eventsChild.child(unique).setValue(event)
                for (comp in EventMore::class.memberProperties) {
                    eventsChild.child("$unique/${comp.name}").setValue(comp.get(eventmore))
                }
                FirebaseDatabase.getInstance().getReference("users")
                    .child("${event.eventUsersToTheirRoles!!.keys.first()}/events/$unique")
                    .setValue(true)

                it.resume(unique)
            }
        }

        suspend fun findEventsWithFilters(
            filters: MutableList<Int>,
            lastEventId: Pair<Boolean, String?>,
            string: String?,
        ): Pair<MutableList<DataClasses.Event>, Pair<Boolean, String?>> {
            return suspendCoroutine {
                val foundEvents = mutableListOf<DataClasses.Event>()
                val reference = FirebaseDatabase.getInstance().getReference("events")
                var query = reference.orderByKey()
                Log.d("lastEventIdData", lastEventId.toString())
                if (lastEventId.first) {
                    if (lastEventId.second != null) {
                        query = query.startAfter(lastEventId.second)
                    }
                } else {
                    it.resume(Pair(foundEvents, lastEventId))
                    return@suspendCoroutine
                }
                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val lastDBvalueKey = snapshot.children.last().key.toString()
                        Log.d("guyfgywe", snapshot.toString())
                        for (event in snapshot.children) {
                            val currentDBvalueKey = event.key.toString()
                                val eventData = event.getValue(DataClasses.Event::class.java)!!
                                eventData.eventId = currentDBvalueKey
                                var isOk = false
                                if (string.isNullOrEmpty()) {
                                    val eventFilters =
                                        event.child("filters").getValue(String::class.java)!!
                                            .split(",").map { it.toInt() - 1 }.sorted()
                                    val eventFiltersSame =
                                        eventFilters.intersect(filters.toSet()).toMutableList()
                                            .sorted()

                                    if (filters == eventFiltersSame) isOk = true
                                } else {
                                    if (eventData.eventName.lowercase()
                                            .startsWith(string.lowercase())
                                        ||
                                        eventData.eventAbout.lowercase()
                                            .contains(string.lowercase())
                                    ) isOk = true
                                }
                                if (isOk) {
                                    foundEvents.add(eventData)
                                    Log.d(
                                        "added event",
                                        "${currentDBvalueKey == lastDBvalueKey} $currentDBvalueKey $lastDBvalueKey"
                                    )
                                    if (foundEvents.size == 3 || currentDBvalueKey == lastDBvalueKey) {
                                        var nextDataPair =
                                            Pair<Boolean, String?>(true, currentDBvalueKey)
                                        if (currentDBvalueKey == lastDBvalueKey) {
                                            nextDataPair = Pair(false, null)
                                        }
                                        it.resume(Pair(foundEvents, nextDataPair))

                                        return
                                    }
                                }

                        }
                        it.resume(Pair(foundEvents, Pair(false, null)))
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }
}
