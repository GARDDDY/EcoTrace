package com.gy.ecotrace.db

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Exclude
import java.util.concurrent.TimeUnit
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.gy.ecotrace.Globals.Companion.applicationContext
import com.gy.ecotrace.Globals.Companion.getInstance
import com.gy.ecotrace.db.DatabaseMethods.UserDatabaseMethods.User
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.Goals
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.typeOf

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

        fun currentTimeId(): String {
            val currentUtcTime = LocalDateTime.now(ZoneOffset.UTC)
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            return currentUtcTime.format(formatter)
        }

        fun encodeKey(key: String): String {
            return try {
                URLEncoder.encode(key, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                key
            }
        }

        fun decodeKey(key: String): String {
            return try {
                URLDecoder.decode(key, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                key
            }
        }

        private fun getInCache(page: String, fields: String?): String? {

            return null
        }

        private fun saveInCache(page: String, fields: String?, data: String) {

        }

        fun requestGETAuth(pageGetName: String, fields: String?, callback: (String?) -> Unit) {
            val inCache = getInCache(pageGetName, fields)
            if (inCache != null) {
                callback(inCache)
                return
            }
            val authUser = FirebaseAuth.getInstance().currentUser
            authUser?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                if (tokenTask.isSuccessful) {
                    val oAuth2: String? = tokenTask.result?.token
                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val request = Request.Builder()
                        .url("${BuildConfig.SERVER_API_URI}$pageGetName?$fields&cid=${authUser.uid}&oauth=$oAuth2")
                        .build()

                    Log.d("final url", request.url.toString())

                    client.newCall(request).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            response.use {
                                if (!response.isSuccessful) {
                                    Log.d("response", "Not successful: ${response.code}")
                                    callback(null)
                                    return
                                }

                                val responseBody = response.body?.string() ?: ""

                                Log.d("response first", "$responseBody ${response.receivedResponseAtMillis - response.sentRequestAtMillis} ms")
                                if (responseBody.isEmpty() || responseBody == "null") {
                                    Log.d("response is null", responseBody)
                                    callback(null)
                                    return
                                }

                                try {
                                    val jsonObject = Gson().fromJson(responseBody, JsonObject::class.java)
                                    if (jsonObject.has("error")) {
                                        Log.d("response", "Error found: $responseBody")
                                        callback(null)
                                        return
                                    }

                                    Log.d("got req", responseBody)
                                    saveInCache(pageGetName, fields, responseBody)
                                    callback(responseBody)
                                } catch (e: Exception) {
                                    Log.d("response parse error", "Failed to parse response: ${e.message}")
                                    callback(null)
                                }
                            }
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            Log.d("bad request", "Request failed: ${e.message}")
                            callback(null)
                        }
                    })
                }
            }
        }

        fun requestGET(pageGetName: String, fields: String?, callback: (String?) -> Unit) {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("${BuildConfig.SERVER_API_URI}$pageGetName?$fields")
                .build()

            Log.d("final url", request.url.toString())

                    client.newCall(request).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            response.use {
                                if (!response.isSuccessful) {
                                    Log.d("response", "Not successful: ${response.code}")
                                    callback(null)
                                    return
                                }

                                val responseBody = response.body?.string() ?: ""

                                Log.d("response first", "$responseBody ${response.receivedResponseAtMillis - response.sentRequestAtMillis} ms")
                                if (responseBody.isEmpty() || responseBody == "null") {
                                    Log.d("response is null", responseBody)
                                    callback(null)
                                    return
                                }

                                try {
//                                    val jsonObject = Gson().fromJson(responseBody, JsonObject::class.java)
//                                    if (jsonObject.has("error")) {
//                                        Log.d("response", "Error found: $responseBody")
//                                        callback(null)
//                                        return
//                                    }

                                    Log.d("got req", responseBody)
                                    callback(responseBody)
                                } catch (e: Exception) {
                                    Log.d("response parse error", "Failed to parse response: ${e.message}")
                                    callback(null)
                                }
                            }
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            Log.d("bad request", "Request failed: ${e.message}")
                            callback(null)
                        }
                    })


        }

        fun requestPOST(pageGetName: String, fields: String?, callback: (String?) -> Unit) {
            val authUser = FirebaseAuth.getInstance().currentUser
            authUser?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                if (tokenTask.isSuccessful) {
                    val oAuth2: String? = tokenTask.result?.token
                    val jsonRequest = "some request"
                    val JSON = "application/json; charset=utf-8".toMediaType()

                    val client = OkHttpClient()
                    val body: RequestBody = jsonRequest.toRequestBody(JSON)
                    val request = Request.Builder()
                        .url("${BuildConfig.SERVER_API_URI}$pageGetName?$fields&oauth=$oAuth2&cuid=${authUser.uid}")
                        .post(body)
                        .build()

                    try {
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                throw IOException("Запрос к серверу не был успешен:" +
                                        " ${response.code} ${response.message}")
                            }
                            println(response.body!!.string())
                        }
                    } catch (e: IOException) {
                        Log.d("server", "Ошибка подключения: $e")
                    }
                }
            }
        }
    }

    class DataClasses {
        data class Country(
            var name: String = "Скрыто",
            var code: String? = null
        )

        data class Friendship(
            var userId: String = "0",
            var friend: Boolean = false,
            var sender: Boolean = false,
            var username: String = ""
        )

        data class FiltersFriendship(
            var userId: String = "0",
            var username: String = "error-username",
//            var friend: Boolean = false,
            var filters: String = ""
        )

        data class Event(
            @get:Exclude var eventId: String = "0",
            var eventName: String = "",
            var eventAbout: String? = null,
            var eventTags: String? = null,
            var eventStatus: Int = 0 /* 0 - Предстоит 1 - Проходит 2 - Закончилось*/,
            @get:Exclude @set:Exclude var eventCountMembers: Int = 0,
            var eventUsersToTheirRoles: HashMap<String, Int>? = null,
            var eventStart: String = "0;0",
            var eventCreatorId: String = "",
            @get:Exclude @set:Exclude var eventCreatorName: String = "",
            var filters: String = ""
        )

        data class Group(
            @get:Exclude @set:Exclude var groupId: String = "0",
            var groupName: String = "",
            var groupAbout: String? = null,
            var groupExperience: Int = 0,
            var groupRank: Int = 0,
            var groupCreatorId: String = "0",
            var groupTags: String? = null,
            var groupCountMembers: Int = 0,
            var groupType: Int = 0 /* 0 - открытая  1 - по заявкам  2 - закрытая*/,
            @get:Exclude @set:Exclude var groupUsersToTheirRoles: HashMap<String, HashMap<String, Boolean>>? = null
        )

        data class Comment(
            @get:Exclude @set:Exclude var commentId: String = "0",
            var commentCreatorId: String = "0",
            @get:Exclude @set:Exclude var commentCreatorName: String = "",
            var commentContentText: String? = null,
            var commentContentImageURI: String? = null
        )

        data class Post(
            @get:Exclude @set:Exclude var postId: String = "0",
            var postCreatorId: String = "0",
            @get:Exclude @set:Exclude var postCreatorName: String = "",
            var postContentText: String? = null,
            var postContentImageURI: String? = null,
            var postCommentsCount: Long = 0,
            var isHidden: Boolean = false // todo
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

        data class UserGroupAbilities(
            var manageUsers: Int = 4,
            var kickUsers: Boolean = false,
            var deletePosts: Boolean = false,
            var editGroup: Boolean = false
        )

        data class EcoTraceLinkResource(
            var resourceObject: String,
            var resourceIdInList: Int,
            var resourceName: String,
            var resourceDescription: String
        )

        data class Formula (
            var description: String = "",
            var formula: String = "",
            var useSpecify: Boolean = false,
            var valueType: String = "",
            var value: Float? = null
        )

        data class Values(
            var minValue: Int = 0,
            var maxValue: Int = 0,
            var valueType: String = "",
            var divideSpecifiersToMax: Boolean? = true
        )

        data class Specify(
            var name: String = "",
            var value: Int? = null,
        )

        data class EcoCalcQuestion(
            var qNoData: String = "",
            var qNoDataInTime: String = "",
            var qData: String = "",

            var useSpecify: Boolean = false,
            var dataValue: Int? = null,
            var specify: HashMap<String, Specify>? = null,
            var sliders: Values
        )

        data class Rating(
            var userId: String = "0",
            var experience: Int = 0,
            var username: String = ""
        )



        companion object {
            val EventRoles = arrayOf("Глава", "Помощник", "Исполняющий")
            val GroupRanks = arrayOf("Владелец", "Совладелец", "Следящий", "Участник")
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
        class User(
            var userId: String = "0",
            var username: String = "",
            var fullname: String? = null,
            var gender: Int = 0,
            var country: DataClasses.Country = DataClasses.Country(),
            var filters: String? = "",
            var aboutMe: String? = "",
            var experience: Int = 0,
//            var groups: HashMap<String, Boolean>? = null,
//            var events: HashMap<String, Boolean>? = null,
//            var friends: HashMap<String, DataClasses.Friendship>? = null
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

        class UserEvent(
            var eventInfo: DataClasses.Event = DataClasses.Event(),
            var isUserInEvent: Boolean = false
        )

        class UserGroup(
            var isUserInGroup: Boolean = false,
            var groupInfo: DataClasses.Group = DataClasses.Group(),
        )

        suspend fun getUserEmail(login: String, password: String): String? {
            return suspendCoroutine {
                Tech().requestGET("getUserEmail.php", "lgn=$login&pss=${Tech().hash256(password)}") {
                        response ->
                    it.resume(response)
                }
            }
//            val userIdSnapshot = FirebaseDatabase.getInstance().getReference("indexes/${Tech().encodeKey(login)}").get().await()
//            if (userIdSnapshot.exists()) {
//                val userId = userIdSnapshot.getValue(String::class.java)
//                val userPassword = FirebaseDatabase.getInstance().getReference("users/$userId/private/password").get().await().getValue(String::class.java)
//                val userEmail = FirebaseDatabase.getInstance().getReference("users/$userId/private/email").get().await().getValue(String::class.java)
//
//                return if (Tech().hash256(password) == userPassword)
//                    userEmail
//                else null
//            } else {
//                return null
//            }
        } // servs

        fun saveEcoCalc(questions:  HashMap<String, DatabaseMethods. DataClasses. EcoCalcQuestion>, formulas:  HashMap<String, DatabaseMethods. DataClasses. Formula>) {
            val formHash = hashMapOf<String, Float>()

            for (formula in formulas) {
                if (formula.value.value != null) {
                    formHash[formula.key] = formula.value.value!!
                }
            }

            Log.d("end", formHash.toString())
        }

    suspend fun getUsernameOnly(userId: String): String {
        FirebaseDatabase.getInstance().getReference().orderByKey().limitToFirst(4).limitToLast(2)
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
        }// servs
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
        }// servs

        suspend fun getUserInfo(userId: String): User? {
            return suspendCoroutine {
                Tech().requestGETAuth("getbaseprofile.php", "uid=$userId") {
                    response ->
                    if (response == null) {
                        it.resume(null)
                        return@requestGETAuth
                    }

                    it.resume(
                        Gson().fromJson(response, User::class.java)
                    )
                }
            }
        }

        suspend fun getUserGroups(userId: String, gGot: String?): HashMap<String, Boolean>? {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserGroups.php", "uid=$userId&block=$gGot") { response ->
                    if (response == null) {
                        it.resume(null)
                        return@requestGETAuth
                    }

                    val userGroups: HashMap<String, Boolean> = Gson().fromJson(response, object :
                        TypeToken<HashMap<String, Boolean>>() {}.type)

                    it.resume(userGroups)
                }
            }
        } // servs

        suspend fun getUserEvents(userId: String, eGot: String?): HashMap<String, Boolean>? {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserEvents.php", "uid=$userId&block=$eGot") { response ->
                    if (response == null) {
                        it.resume(null)
                        return@requestGETAuth
                    }

                    val userEvents: HashMap<String, Boolean> = Gson().fromJson(response, object :
                        TypeToken<HashMap<String, Boolean>>() {}.type)

                    it.resume(userEvents)
                }
            }
        } // servs

        suspend fun getUserFriends(userId: String, fGot: String?): MutableList<DataClasses.Friendship>? {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserFriends.php", "uid=$userId&block=$fGot") { response ->
                    if (response == null) {
                        it.resume(null)
                        return@requestGETAuth
                    }

                    val userFriends: HashMap<String, DataClasses.Friendship> = Gson().fromJson(response, object :
                        TypeToken<HashMap<String, DataClasses.Friendship>>() {}.type)

                    it.resume(userFriends.values.toMutableList())
                }
            }
        }

        fun removeFriends(userId: String) {
            Tech().requestPOST("removeFriend.php", "uid=$userId") {}
        }// servs
        fun addFriends(userId: String) {
            Tech().requestPOST("addFriend.php", "uid=$userId") {}
        }


        suspend fun isUserInGroup(userId: String, groupId: String): Boolean {
            val isJustAMember = FirebaseDatabase.getInstance().getReference("groups/$groupId/groupUsersToTheirRoles/role3/$userId").get().await().exists() ||
                    FirebaseDatabase.getInstance().getReference("groups/$groupId/groupUsersToTheirRoles/role2/$userId").get().await().exists() ||
                    FirebaseDatabase.getInstance().getReference("groups/$groupId/groupUsersToTheirRoles/role1/$userId").get().await().exists()
            val isCreator = FirebaseDatabase.getInstance().getReference("groups/$groupId/groupCreatorId").get().await().getValue(String::class.java) == userId
            return isJustAMember || isCreator
        }

        suspend fun joinGroup(userId: String, groupId: String): Boolean {
            return suspendCoroutine {
                FirebaseDatabase.getInstance().getReference("users/$userId/groups/$groupId").setValue(true)
                    .addOnSuccessListener { _ ->
                        val groupData = FirebaseDatabase.getInstance().getReference("groups/$groupId")
                        groupData.child("groupUsersToTheirRoles/role3/$userId").setValue(3) // todo roles
                        it.resume(true)
                    }.addOnFailureListener { _ ->
                        it.resume(false)
                    }
            }
        }// servs
        suspend fun leaveGroup(userId: String, groupId: String): Boolean {
            return suspendCoroutine {
                FirebaseDatabase.getInstance().getReference("users/$userId/groups/$groupId").removeValue()
                    .addOnSuccessListener { _ ->
                        val groupData = FirebaseDatabase.getInstance().getReference("groups/$groupId")
                        for (userRole in 1..3) {
                            groupData.child("groupUsersToTheirRoles/role$userRole/$userId")
                                .removeValue()
                        }
                        it.resume(true)
                    }.addOnFailureListener { _ ->
                        it.resume(false)
                    }
            }
        }// servs
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
                            val eventData = snapshot.getValue(DataClasses.Event::class.java)!!
                            eventData.eventId = eventId
                            it.resume(eventData)
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }

        fun observeEvent(eventId: String, callback: (DataClasses.Event) -> Unit) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("events/$eventId")
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val saved = DataClasses.Event()
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
                            val roles = snapshot.child("groupUsersToTheirRoles")
                            groupData.groupCountMembers = 1 +
                                    roles.child("role1").childrenCount.toInt() +
                                    roles.child("role2").childrenCount.toInt() +
                                    roles.child("role3").childrenCount.toInt()
                            it.resume(groupData)
                        } else {
                            it.resume(DataClasses.Group())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
        suspend fun createGroup(groupData: DataClasses.Group) {
            groupData.groupExperience = 0
            val groups = FirebaseDatabase.getInstance().getReference("groups")
            val unique = groups.push().key!!
            groups.child(unique).setValue(groupData)
            FirebaseDatabase.getInstance().getReference("users/${groupData.groupCreatorId}/groups/$unique").setValue(true)
        }// servs
        suspend fun isGroupNameAvailable(groupName: String): Boolean {
            return !FirebaseDatabase.getInstance().getReference("indexesGroups/$groupName").get().await().exists()
        }
        suspend fun getGroupMembers(groupId: String, lastGot: String?):  Pair<String, HashMap<String, HashMap<String, Boolean>>> {
            val foundUsers = hashMapOf<String, HashMap<String, Boolean>>()
            val db = FirebaseDatabase.getInstance().getReference("groups/$groupId/groupUsersToTheirRoles")
            var maxNeed = 10
            if (lastGot == null) {
                foundUsers["role1"] = db.child("role1").get().await().getValue(
                    object : GenericTypeIndicator<HashMap<String, Boolean>>() {}) ?: hashMapOf()
                foundUsers["role2"] = db.child("role2").get().await().getValue(
                    object : GenericTypeIndicator<HashMap<String, Boolean>>() {}) ?: hashMapOf()

                maxNeed = 28 - foundUsers["role1"]!!.size - foundUsers["role2"]!!.size
            }
            foundUsers["role3"] = hashMapOf()
            return suspendCoroutine {
                db.child("role3").orderByKey().startAfter(lastGot ?: "")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (member in snapshot.children) {
                                    foundUsers["role3"]!![member.key.toString()] = true
                                    if (foundUsers["role3"]!!.size == maxNeed) {
                                        it.resume(Pair(member.key.toString(), foundUsers))
                                        return
                                    }
                                }
                            }
                            it.resume(Pair("", foundUsers))
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }

        suspend fun userGroupRoleAtLeastModerator(userId: String, groupId: String): Int {
            Log.d("check role", userId)
            val isCeo = FirebaseDatabase.getInstance().getReference("groups/$groupId/groupUsersToTheirRoles/role1/$userId").get().await().exists()
            Log.d("ceo", "$userId $isCeo")
            if (isCeo) return 1
            val isModer = FirebaseDatabase.getInstance().getReference("groups/$groupId/groupUsersToTheirRoles/role2/$userId").get().await().exists()
            Log.d("moder", "$userId $isModer")
            return if (isModer) 2
            else 3
        }// servs

        fun observeGroup(groupId: String, callback: (DataClasses.Group) -> Unit) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("groups/$groupId")
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val saved = DataClasses.Group()
                        val roles = snapshot.child("groupUsersToTheirRoles")
                        saved.groupCountMembers = 1 +
                                roles.child("role1").childrenCount.toInt() +
                                roles.child("role2").childrenCount.toInt() +
                                roles.child("role3").childrenCount.toInt()
                        saved.groupUsersToTheirRoles = snapshot.child("groupUsersToTheirRoles").
                            getValue(object : GenericTypeIndicator<HashMap<String, HashMap<String, Boolean>>>() {})
                        saved.groupExperience = snapshot.child("groupExperience")
                            .getValue(Int::class.java)!!
                        saved.groupRank = snapshot.child("groupRank")
                            .getValue(Int::class.java)!!

                        callback(saved)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        fun setUserRoleInGroup(groupId: String, userId: String, roleFrom: Int, roleTo: Int) {
            FirebaseDatabase.getInstance().getReference("groups/$groupId/groupUsersToTheirRoles/role$roleTo/$userId").setValue(true)
            FirebaseDatabase.getInstance().getReference("groups/$groupId/groupUsersToTheirRoles/role$roleFrom/$userId").removeValue()
        }// servs
        fun kickUserFromGroup(groupId: String, userId: String, role: Int) {
            FirebaseDatabase.getInstance().getReference("groups/$groupId/groupUsersToTheirRoles/role$role/$userId").removeValue()
            FirebaseDatabase.getInstance().getReference("users/$userId/groups/$groupId").removeValue()
        }// servs

        suspend fun getPosts(groupId: String, lastEventId: Triple<Boolean, String?, Int>): Pair<MutableList<DataClasses.Post>, Triple<Boolean, String?, Int>> {
            val reference = FirebaseDatabase.getInstance().getReference("groups/$groupId/posts")
            val dbFirstRef = reference.get().await()
            if (dbFirstRef.childrenCount == 0L) {
                return Pair(mutableListOf(), Triple(false, null, 0))
            }
            val dbFirst = dbFirstRef.children.first().key.toString()
            return suspendCoroutine {
                val foundEvents = mutableListOf<DataClasses.Post>()
                var query = reference.orderByKey().limitToLast(2*lastEventId.third)
                if (lastEventId.first) {
                    if (lastEventId.second != null) {
                        query = reference.orderByKey().endBefore(lastEventId.second).limitToLast(2)
                    }
                } else {
                    it.resume(Pair(foundEvents, lastEventId))
                    return@suspendCoroutine
                }
                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d("tyuraagyiur", snapshot.children.map { it.key }.toString())
                        if (snapshot.exists()) {
                            val lastDBvalueKey = snapshot.children.first().key
                            Log.d("rgrgr", "${lastDBvalueKey.toString()} $dbFirst")
                            for (event in snapshot.children.reversed()) {
                                val currentDBvalueKey = event.key.toString()
                                val eventData = event.getValue(DataClasses.Post::class.java)!!
                                eventData.postId = currentDBvalueKey
                                eventData.postCommentsCount = event.child("postComments").childrenCount

                                Log.d("test", eventData.toString())

                                foundEvents.add(eventData)
                                Log.d("post", eventData.toString())

                                if (foundEvents.size == 2 || currentDBvalueKey == dbFirst) { // todo max value
                                    var nextDataPair = Triple(true, currentDBvalueKey, lastEventId.third+1)
                                    if (currentDBvalueKey == dbFirst) {
                                        nextDataPair = Triple(false, dbFirst, lastEventId.third+1)
                                    }
                                    it.resume(Pair(foundEvents, nextDataPair))
                                    return
                                }
                            }
                        }
                        it.resume(Pair(foundEvents, Triple(false, null, 0)))
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }// servs
        suspend fun getPostComments(groupId: String, postId: String, lastComment: Pair<String?, String?>): Pair<MutableList<DatabaseMethods.DataClasses.Comment>, Pair<String, String>> {
            return suspendCoroutine {
                val foundEvents = mutableListOf<DataClasses.Comment>()
                val reference = FirebaseDatabase.getInstance().getReference("groups/$groupId/posts/$postId/postComments")
                var query = reference.orderByKey()
                if (lastComment.second != null) {
                    query = query.startAfter(lastComment.second)
                }
//              else {
//                    it.resume(Pair(foundEvents, lastEventId))
//                    return@suspendCoroutine
//                }
                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val lastDBvalueKey = snapshot.children.last().key.toString()
                            for (event in snapshot.children) {
                                val currentDBvalueKey = event.key.toString()
                                Log.d("testcommentdb", event.toString())
                                val eventData = event.getValue(DataClasses.Comment::class.java)!!
                                eventData.commentId = currentDBvalueKey
                                foundEvents.add(eventData)
                                if (foundEvents.size == 10 || currentDBvalueKey == lastDBvalueKey) {
                                    var nextDataPair =
                                        Pair<String, String>(groupId, currentDBvalueKey)
                                    if (currentDBvalueKey == lastDBvalueKey) {
                                        nextDataPair = Pair("0", currentDBvalueKey)
                                    }
                                    it.resume(
                                        Pair(
                                            foundEvents,
                                            nextDataPair
                                        )
                                    )

                                    return
                                }


                            }
                        }
                        it.resume(Pair(foundEvents, Pair(groupId, "0")))
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }// servs

        fun observePosts(groupId: String, showedPosts: MutableList<String>, lastFound: String, callback: (Pair<Boolean, DataClasses.Post>) -> Unit) {
            val reference = FirebaseDatabase.getInstance().getReference("groups/$groupId/posts").orderByKey()
            reference.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d("observed", "Observing")
                    if (!showedPosts.contains(snapshot.key.toString()) &&
                        snapshot.key.toString().toLong() > lastFound.toLong()) {
                        val postData = snapshot.getValue(DataClasses.Post::class.java)!!
                        postData.postId = snapshot.key.toString()
//                        postData.postCreatorName = UserDatabaseMethods().getUsernameOnly(postData.postCreatorId)
                        Log.d("why added", showedPosts.toString())
                        callback(Pair(true, postData))
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    if (showedPosts.contains(snapshot.key.toString())) {
                        val postData = snapshot.getValue(DataClasses.Post::class.java)!!
                        postData.postId = snapshot.key.toString()
                        callback(Pair(false, postData))
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
        }// servs

        fun sendComment(groupId: String, postId: String, userId: String, textContent: String?, imageURI: String?) {
            val currentTime = Tech().currentTimeId()
            val comment = DataClasses.Comment(
                commentCreatorId = userId,
                commentContentText = textContent,
                commentContentImageURI = imageURI,
            )

            FirebaseDatabase.getInstance().getReference("groups/$groupId/posts/$postId/postComments/$currentTime")
                .setValue(comment)
        }// servs


        fun createPost(groupId: String, userId: String, textContent: String?, imageURI: String?) {
            val currentTimeId = Tech().currentTimeId()
            val post = DataClasses.Post(
                postCreatorId = userId,
                postCommentsCount = 0L,
                postContentText = textContent,
                postContentImageURI = imageURI,
                isHidden = false
            )
            FirebaseDatabase.getInstance().getReference("groups/$groupId/posts/$currentTimeId")
                .setValue(post)
        }// servs








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
//                for (comp in EventMore::class.memberProperties) {
//                    eventsChild.child("$unique/${comp.name}").setValue(comp.get(eventmore))
//                }
                FirebaseDatabase.getInstance().getReference("users")
                    .child("${event.eventUsersToTheirRoles!!.keys.first()}/events/$unique")
                    .setValue(true)

                it.resume(unique)
            }
        }// servs

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
                                        (eventData.eventAbout ?: "").lowercase()
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

        /*suspend*/ fun getImageLink(folder: String, imageId: String): String {
            return "https://firebasestorage.googleapis.com/v0/b/ecotrace-cf2be.appspot.com/o/$folder%2F$imageId.png?alt=media"
//            return suspendCoroutine {
//                FirebaseStorage.getInstance().getReference(folder).child("$imageId.png").downloadUrl
//                    .addOnSuccessListener { it1 ->
//                        it.resume(it1.toString())
//                    }.addOnFailureListener{ _ ->
//                        it.resume("")
//                    }
//            }
        }

        suspend fun uploadImage(folder: String, imageId: String, imageData: Bitmap): String {
            return suspendCoroutine {
                val currentTimeId = Tech().currentTimeId()
                val baos = ByteArrayOutputStream()
                imageData.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val imageBaos = baos.toByteArray()
                val storageRef = FirebaseStorage.getInstance().getReference(folder).child("${imageId}_$currentTimeId.png")
                val uploadTask = storageRef.putBytes(imageBaos)
                uploadTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        it.resume("${imageId}_$currentTimeId")
                    }
                }.addOnFailureListener { _ ->
                    it.resume("")
                }
            }
        }

        suspend fun getUserRating(userId: String): MutableList<DataClasses.Rating> {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserRating.php", "uid=$userId") { response ->

                    if (response == null) {
                        return@requestGETAuth
                    }

                    val data: MutableMap<String, DataClasses.Rating> =
                        Gson().fromJson(response, object :
                            TypeToken<HashMap<String, DataClasses.Rating>>() {}.type)

                    Log.d("test", data.toString())

                    it.resume(data.values.sortedBy { it.experience }.reversed().toMutableList())
                }
            }
        }

    }// servs

    class Account {

        private fun insertUserData(userData: UserDatabaseMethods.User, uid: String, callback: (Boolean) -> Unit) {
            FirebaseDatabase.getInstance().getReference("users/$uid").setValue(userData)
                .addOnCompleteListener {
                    callback(it.isSuccessful)
                }
        }

        private fun getToken(user: FirebaseUser?, isNewToken: Boolean = false, callback: (String?) -> Unit) {
            if (user != null) {
                user.getIdToken(isNewToken)
                    .addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val token: String? = tokenTask.result.token
                            callback(token)
                        } else {
                            Toast.makeText(Tech().context(), "Аутентификация не удалась!", Toast.LENGTH_LONG).show()
                            callback(null) // token error
                        }
                    }
            } else {
                Toast.makeText(Tech().context(), "Регистрация не удалась!", Toast.LENGTH_LONG).show()
                if (signOut()) {
                    callback(null)
                }
            }
        }

        fun createAccount(email: String, password: String, userData: UserDatabaseMethods.User, callback: (Boolean) -> Unit) {
            val auth = FirebaseAuth.getInstance()
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { signUp ->
                    if (signUp.isSuccessful) {
                        val user: FirebaseUser? = auth.currentUser
                        user?.updateProfile(UserProfileChangeRequest.Builder()
                            .setDisplayName(userData.fullname ?: userData.username)
                            .build())
                        getToken(user, true) { uid ->
                            if (uid == null) callback(false)
                            else insertUserData(userData, uid) { callback(it) }
                        }
                    } else {
                        Toast.makeText(Tech().context(), "Регистрация заблокирована!", Toast.LENGTH_LONG).show()
                        callback(false) // auth error
                    }
                }

        }

        fun loginIntoAccount(email: String, password: String, callback: (String?) -> Unit) {
            val auth = FirebaseAuth.getInstance()
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { signInTask ->
                    Log.d("login", "$email $password")
                    if (signInTask.isSuccessful) {
                        val user: FirebaseUser? = auth.currentUser
                        callback(user?.uid)
//                        getToken(user) { uid ->
//                            callback(uid)
//                        }
                    } else {
                        Toast.makeText(Tech().context(), "Вход заблокирован!", Toast.LENGTH_LONG).show()
                        callback(null) // auth error
                    }
                }
        }

        fun signOut(): Boolean {
            val auth = FirebaseAuth.getInstance()
            auth.signOut()

            Globals.getInstance().setString("CurrentlyLogged", "0")
            Globals.getInstance().setString("CurrentlyWatching", "0")

            return true
        }

        fun deleteAccount() {

        }
    }

    class ServerSide {

        fun deleteGroup(groupId: String, userId: String) {
            
        }

    }
}
