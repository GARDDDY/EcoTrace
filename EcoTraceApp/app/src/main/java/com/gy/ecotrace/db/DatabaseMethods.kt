package com.gy.ecotrace.db

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract.Data
import android.util.Log
import android.widget.Toast
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Exclude
import java.util.concurrent.TimeUnit
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.protobuf.Value
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.Globals
import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
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

//                                Log.d("response first", "$responseBody ${response.receivedResponseAtMillis - response.sentRequestAtMillis} ms")
//                                if (responseBody.isEmpty() || responseBody == "null") {
//                                    Log.d("response is null", responseBody)
//                                    callback(null)
//                                    return
//                                }

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
                                    val jsonObject = Gson().fromJson(responseBody, JsonArray::class.java)

                                    Log.d("got req", responseBody)
                                    saveInCache(pageGetName, fields, responseBody)
                                    callback(responseBody)
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

        fun requestGET(pageGetName: String, fields: String?, callback: (Response?) -> Unit) {
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

                        if (response.body == null) {
                            Log.d("response", "Response body is null")
                            callback(null)
                            return
                        }

                        Log.d("got req", "Response received")
                        callback(response)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Log.d("bad request", "Request failed: ${e.message}")
                    callback(null)
                }
            })
        }

        fun requestPOST(pageGetName: String, jsonData: String, fields: String, img: Bitmap? = null, callback: (Response?) -> Unit) {
            val authUser = FirebaseAuth.getInstance().currentUser
            authUser?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                if (tokenTask.isSuccessful) {
                    val oAuth2: String? = tokenTask.result?.token

                    val client = OkHttpClient()

                    val requestBodyBuilder = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("jsonData", jsonData)
                    img?.let {
                        Log.d("img", "adding img")
                        val stream = ByteArrayOutputStream()
                        it.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val byteArray = stream.toByteArray()
                        requestBodyBuilder.addFormDataPart(
                            "image", "image.png", RequestBody.create(
                                "image/png".toMediaTypeOrNull(), byteArray
                            )
                        )
                    }

                    val requestBody = requestBodyBuilder.build()

                    val request = Request.Builder()
                        .url("${BuildConfig.SERVER_API_URI}$pageGetName?$fields&cuid=${authUser.uid}&oauth=$oAuth2")
                        .post(requestBody)
                        .build()

                    Log.wtf("final url", request.url.toString())

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            Log.d("End", "Failed to send data ${e.message}")
                            callback(null)
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (response.isSuccessful) {
                                Log.d("End", "Data successfully sent!")
                                callback(response)
                            } else {
                                Log.d(
                                    "End",
                                    "Failed to send data ${response.code} ${response.message}"
                                )
                                callback(null)
                            }
                        }
                    })
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
            var senderId: String = "0",
            var isFriend: Int = 0,

            var username: String = "",
        )

        data class FiltersFriendship(
            var userId: String = "0",

            var username: String = "",
            var filters: String = ""
        )

        data class Event(
            var eventId: String = "0",
            var eventName: String = "",
            var eventAbout: String? = null,
            var eventStatus: Int = 0 /* 0 - Предстоит 1 - Проходит 2 - Закончилось*/,
            var eventCountMembers: Int = 0,
            var eventStart: String = "0;0", /* 0;x - начало с первым действием*/

            var eventCreatorId: String = "",
            var eventCreatorName: String = "",
            var filters: String = ""
        )

        // groups

        data class Group(
            var groupId: String = "0",
            var groupName: String = "",
            var groupAbout: String? = null,
            var groupExperience: Int = 0,
            var groupRank: Int = 0,
            var groupCountMembers: Int = 0,

            var groupCreatorId: String = "0",
            var groupCreatorName: String = "",

            var filters: String = "",
            var groupType: Int = 0 /* 0 - открытая  1 - по заявкам  2 - закрытая*/
        )

        data class Comment(
            var commentId: Int = -1,
            var commentTime: Long = 0,
            var commentCreatorId: String = "0",
            var commentCreatorName: String = "",
            var commentContentText: String? = null,
            var commentContentImage: String? = null,
            var commentCreatorRole: Int = 4
        )

        data class Post(
            var postId: Int = -1,
            var postTime: Long = 0,
            var postCreatorId: String = "0",
            var postCreatorName: String = "",
            var postContentText: String? = null,
            var postContentImage: String? = null,
            var postCommentsCount: Long = 0,
            var postCreatorRole: Int = 4
        )

        data class UserGroupAbilities(
            var manageUsers: Int = 4,
            var kickUsers: Boolean = false,
            var deletePosts: Boolean = false,
            var editGroup: Boolean = false,
            var role: Int = -1
        )


        // education

        data class EduFacts(
            var fact: String,
            var imageUrl: String,
            var question: String,
            var answers: MutableList<String>,
            var answer: Int
        )

        data class EduTask(
            var qType: Int,
            var question: String,
            var answer: Any,
            var answers: MutableList<String>?
        )


        // events

        data class MapObject(
            var objectName: String = "",
            var objectType: Int = 0, // 0 - Dot, 1 - Circle
            var objectRelation: String? = null,
            var objectCenter: Point = Point(),

            var fillColor: String = "#00000000",
            var strokeColor: String = "#00000000",

            var circleRadius: Float? = null,


        )

        data class EcoTraceLinkResource(
            var resourceObject: String,
            var resourceIdInList: Int,
            var resourceName: String,
            var resourceDescription: String
        )


        // calculator

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

        data class EcoCalcSaveData(
            var question: Int = 0,
            var value: Int = 0,
            var specify: MutableMap<String, Int> = mutableMapOf(),
            var useSpecify: Boolean = false
        )


        // uss, users searching systems

        data class Rating(
            var userId: String = "0",
            var experience: Int = 0,
            var username: String = ""
        )

        data class UserInGroup(
            var userId: String = "0",
            var username: String = "",
            var role: Int = -1
        )



//        companion object {
//            val EventRoles = arrayOf("Участник", "Помощник", "Создатель")
//            val GroupRanks = arrayOf("Владелец", "Совладелец", "Следящий", "Участник")
//            val UserRanks = arrayOf("Крутой", "Очень крутой")
//            val UserFiltersSearchBy = arrayOf(
//                Pair("Активный", "\"Активный\" пользователь всегдат\nготов присоединиться к новым мероприятиям"),
//                Pair("Веселый", "\"Веселый\" пользователь"),
//                Pair("Ветеран", "Пользователь, зарегистрировавшийся довольно давно (награда)"),
//                Pair("В сети", "Пользователь часто бывает в сети"))
//            val EventFiltersSearchBy = arrayOf(
//                Pair("На улице", "Часть или все мероприятие проходит на улице"),
//                Pair("В помещении", "Часть или все мероприятие проходит в помещении")
//            )
//
//            val filterColors = arrayOf(
//                Pair("#00FA9A", "#1A3329"), // main, text
//                Pair("#FF7F50", "#33201A"),
//                Pair("#00CED1", "#1A3333"),
//                Pair("#DA70D6", "#331A32"),
//                Pair("", ""),
//                Pair("", ""),
//            )
//        }
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
            var experience: Int = 0
        )

        class UserActivity(
            var userId: String = "0",
            var username: String = "",
            var experience: Int = 0,
            var role: Int = -1
        )

        class UserEvent(
            var eventInfo: DataClasses.Event = DataClasses.Event(),
            var isUserInEvent: Boolean = false,
            var isValidated: Boolean = false,
            var eventRole: Int = 2
        )

        class UserGroup(
            var isUserInGroup: Boolean = false,
            var groupRole: Int = 3,
            var groupInfo: DataClasses.Group = DataClasses.Group(),
        )

        suspend fun getUserEmail(login: String, password: String): String? {
            return suspendCoroutine {
                Tech().requestGET("getUserEmail", "lgn=$login&pss=${Tech().hash256(password)}") {
                        response ->
                    it.resume(response?.body?.string())
                }
            }
        } // servs

        fun saveEcoCalc(data: MutableList<DataClasses.EcoCalcSaveData>, calcType: Int, callback: (String) -> Unit) {
            val jsonData = Gson().toJson(data)
            Tech().requestPOST("setEcoData", jsonData, "calcType=$calcType") { response ->
                if (response == null) {
                    callback("Произошла ошибка")
                    return@requestPOST
                }

                val responseBody = response.body?.string() ?: ""
                val msg = Gson().fromJson(responseBody, HashMap::class.java)
                callback(msg["message"].toString())
            }
        }

        suspend fun getEcoCalc(calcType: Int): String? {
            return suspendCoroutine {
                Tech().requestGETAuth("getEcoCalc", "calcType=$calcType") { response ->
                    it.resume(response)
                }
            }
        }

        suspend fun getUsernameOnly(userId: String): String {
            return "TODO METHOD"
        }

        suspend fun joinEvent(eventId: String) {
            Tech().requestPOST("joinEvent", "", "eventId=$eventId") {}
        }// servs
        suspend fun leaveEvent(eventId: String) {
            Tech().requestPOST("leaveEvent", "", "eventId=$eventId") {}
        }// servs

        suspend fun getUserInfo(userId: String): User? {
            return suspendCoroutine {
                Tech().requestGETAuth("getUser", "uid=$userId") {
                    response ->
                    if (response == null) {
                        it.resume(null)
                        return@requestGETAuth
                    }

                    val userData = Gson().fromJson(response, User::class.java)
                    userData.userId = userId
                    it.resume(userData)
                }
            }
        }

        suspend fun getGraph(userId: String, time: Int, hideFilters: MutableList<Int>?): Bitmap? {
            return suspendCoroutine { continuation ->
                Tech().requestGET("getUserGraph", "uid=$userId&time=$time&types=${hideFilters?.joinToString(",")}") { response ->
                    if (response == null) {
                        continuation.resume(null)
                        return@requestGET
                    }

                    val bitmap = BitmapFactory.decodeStream(response.body?.byteStream())
                    continuation.resume(bitmap)
                }
            }
        }

        suspend fun getUserGroups(userId: String, gGot: String?): MutableList<UserGroup> {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserGroups", "uid=$userId&block=${gGot ?: ""}") { response ->
                    if (response == null) {
                        it.resume(mutableListOf())
                        return@requestGETAuth
                    }

                    val userGroups = Gson().fromJson(response, Array<UserGroup>::class.java)

                    it.resume(userGroups.toMutableList())
                }
            }
        } // servs

        suspend fun getUserEvents(userId: String, eGot: String?, sort: Int): MutableList<UserEvent>? {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserEvents", "uid=$userId&block=${eGot ?: ""}&sort=$sort") { response ->
                    if (response == null) {
                        it.resume(null)
                        return@requestGETAuth
                    }

                    val userEvents = Gson().fromJson(response, Array<UserEvent>::class.java)

                    it.resume(userEvents.toMutableList())
                }
            }
        } // servs
        suspend fun getUserEvent(eventId: String): UserEvent {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserEventData", "eventId=$eventId") { response ->
                    if (response == null) {
                        return@requestGETAuth
                    }
                    val data = Gson().fromJson(response, UserEvent::class.java)

                    it.resume(data)
                }
            }
        }


        suspend fun getUserEventsShort(startAt: String?): HashMap<String, String>? {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserEventsShort", "block=$startAt") {

                }
            }
        }

        suspend fun getUpdates(since: Long): Pair<MutableList<Int>, String> {
            return suspendCoroutine {
                Tech().requestGETAuth("getUpdates", "since=$since") { response ->
                    if (response == null) return@requestGETAuth
                    val data = Gson().fromJson(response, Array<Any>::class.java)

                    val userList = mutableListOf<Int>()
                    for (item in data) {
//                        Log.d("check", "$item is Int - ${item is Int}")
                        if (item is Number) {
                            userList.add(item.toInt())
                        }
                    }
                    val userCall = data.lastOrNull()?.toString() ?: ""
                    val result = Pair(userList, userCall)
                    it.resume(result)
                }
            }
        }

        suspend fun getEducations(): MutableList<Int> {
            return suspendCoroutine {
                Tech().requestGETAuth("getEducations", "") { response ->
                    if (response == null) return@requestGETAuth
                    val data = Gson().fromJson(response, Array<Int>::class.java)

                    it.resume(data.toMutableList())
                }
            }
        }

        suspend fun areUsersFriends(userId: String): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("areUsersFriends", "uid=$userId") { response ->
                    if (response == null) {
                        it.resume(false)
                        return@requestGETAuth
                    }

                    it.resume(Gson().fromJson(response, Array<Boolean>::class.java)[0])

                }
            }
        }

        suspend fun getUserFriends(userId: String, fGot: String?): MutableList<DataClasses.Friendship> {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserFriends", "uid=$userId&block=$fGot") { response ->
                    if (response == null) {
                        it.resume(mutableListOf())
                        return@requestGETAuth
                    }

                    val userFriends = Gson().fromJson(response, Array<DataClasses.Friendship>::class.java)

                    it.resume(userFriends.toMutableList())
                }
            }
        }

        suspend fun getUserFriendsFilterByName(username: String?, lastFoundId: String?): Pair<String?, MutableList<DataClasses.Friendship>?> {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserFriendsFilterByName", "filter=$username&lastId=$lastFoundId") { response ->
                    if (response == null) {
                        it.resume(Pair(null, null))
                        return@requestGETAuth
                    }

                    val userFriends: Pair<String?, MutableList<DataClasses.Friendship>?> = Gson().fromJson(response, object :
                        TypeToken<Pair<String?, MutableList<DataClasses.Friendship>?>>() {}.type)

                    it.resume(userFriends)
                }
            }
        }

        suspend fun sendEdu(eduType: Int): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("applyEdu", "edu=$eduType") { response ->
                    if (response == null) {
                        it.resume(false)
                        return@requestGETAuth
                    }

                    it.resume(Regex("\\w+").find(response)!!.value.toBoolean())
                }
            }
        }

        fun removeFriends(userId: String) {
            Tech().requestPOST("removeFriend", "", "uid=$userId") {}
        }// servs
        fun addFriends(userId: String) {
            Tech().requestPOST("addFriend", "", "uid=$userId") {}
        }


        suspend fun isUserInGroup(groupId: String): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("isUserInGroup", "gid=$groupId") { response ->
                    if (response == null) {
                        it.resume(false)
                        return@requestGETAuth
                    }

                    val status = Gson().fromJson(response, Array<Boolean>::class.java)

                    it.resume(status[0])
                }
            }
        }

        suspend fun joinGroup(groupId: String): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("joinGroup", "gid=$groupId") { response ->
                    if (response == null) {
                        it.resume(false)
                        return@requestGETAuth
                    }

                    it.resume(response.toBoolean())
                }
            }
        }// servs
        suspend fun leaveGroup(groupId: String): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("leaveGroup", "gid=$groupId") { response ->
                    if (response == null) {
                        it.resume(false)
                        return@requestGETAuth
                    }

                    it.resume(response.toBoolean())
                }
            }
        }// servs

        suspend fun getCountCalculators(calcType: Int): Int {
            return suspendCoroutine {
                Tech().requestGET("calc/getNumImages", "cType=$calcType") { response ->
                    if (response == null) {
                        it.resume(0)
                        return@requestGET
                    }

                    it.resume(response.body?.string().toString().toInt())

                }
            }
        }

        suspend fun getCalcImage(calcType: Int, imageId: Int): Bitmap? {
            return suspendCoroutine { continuation ->
                Tech().requestGET("calc/getImage", "cType=$calcType&img=$imageId&cid=${FirebaseAuth.getInstance().currentUser?.uid ?: '0'}") { response ->
                    if (response == null) {
                        continuation.resume(null)
                        return@requestGET
                    }

                    val bitmap = BitmapFactory.decodeStream(response.body?.byteStream())
                    continuation.resume(bitmap)
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
                Tech().requestGET("getEvent", "eid=$eventId") { response ->
                    if (response == null) {
                        return@requestGET
                    }

                    val responseBody = response.body?.string() ?: ""

                    val eventData = Gson().fromJson(responseBody, DataClasses.Event::class.java)

                    it.resume(eventData)
                }
            }
        }


        suspend fun getGroup(groupId: String): DataClasses.Group {
            return suspendCoroutine {
                Tech().requestGET("getGroup", "gid=$groupId") { response ->
                    if (response == null) {
                        return@requestGET
                    }

                    val responseBody = response.body?.string() ?: ""

                    val groupData = Gson().fromJson(responseBody, DataClasses.Group::class.java)

                    it.resume(groupData)
                }
            }
        }
        suspend fun createGroup(groupData: DataClasses.Group, text: String?, image: Bitmap?): String? {
            return suspendCoroutine {
                Tech().requestPOST("createGroup", Gson().toJson(arrayOf(groupData, text)), "", image) { response ->
                    if (response == null){
                        return@requestPOST
                    }

                    val responseBody = response.body?.string()
                    it.resume(Gson().fromJson(responseBody, Array<String?>::class.java)[0])
                }
            }
        }
        suspend fun isGroupNameAvailable(groupName: String): String? {
            return suspendCoroutine {
                Tech().requestGET("isGroupNameAvailable", "name=$groupName") { response ->
                    if (response == null) {
                        return@requestGET
                    }

                    val responseBody = response.body?.string() ?: ""

                    it.resume(Gson().fromJson(responseBody, Array<String?>::class.java)[0])
                }
            }
        }
        suspend fun getGroupMembers(groupId: String, lastGot: String?, role: Int):  Pair<Boolean, MutableList<DataClasses.UserInGroup>> {
            return suspendCoroutine {
                Tech().requestGETAuth("getGroupMembers", "gid=$groupId&last=$lastGot&role=$role") { response ->
                    if (response == null) {
                        it.resume(Pair(true, mutableListOf()))
                        return@requestGETAuth
                    }

                    val data: Array<Any> =
                        Gson().fromJson(response, object :
                            TypeToken<Array<Any>>() {}.type)

                    val objects = data[1] as? MutableList<*> ?: emptyList()
                    val usersType = object : TypeToken<Array<DataClasses.UserInGroup>>() {}.type
                    val usersJsonString = Gson().toJson(objects)
                    val users: Array<DataClasses.UserInGroup> = Gson().fromJson(usersJsonString, usersType)

                    val resultPair = Pair(data[0] as Boolean, users.toMutableList())

                    it.resume(resultPair)
                }
            }
        }

        suspend fun userGroupRoleAtLeastModerator(userId: String, groupId: String): Int {
            return suspendCoroutine {
                Tech().requestGET("isUserModerInGroup", "gid=$groupId&uid=$userId") { response ->
                    if (response == null) {
                        return@requestGET
                    }

                    val responseBody = response.body?.string() ?: ""

                    it.resume(responseBody.toInt())
                }
            }
        }// servs

        suspend fun setUserRoleInGroup(groupId: String, userId: String, role: Int): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("setUserRoleInGroup", "gid=$groupId&uid=$userId&role=$role") { it1 ->
                    it.resume(Gson().fromJson(it1, Array<Boolean>::class.java)[0])
                }
            }
        }// servs
        suspend fun kickUserFromGroup(groupId: String, userId: String): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("kickUserFromGroup", "gid=$groupId&uid=$userId") { it1 ->
                    it.resume(Gson().fromJson(it1, Array<Boolean>::class.java)[0])
                }
            }
        }// servs

        suspend fun getPosts(groupId: String, lastId: Int?): Pair<Boolean, Array<DataClasses.Post>?> {
            return suspendCoroutine {
                Tech().requestGETAuth("getPosts", "gid=$groupId&lastGot=$lastId") { response ->
                    if (response == null) {
                        it.resume(Pair(true, arrayOf()))
                        return@requestGETAuth
                    }

                    val data: Array<Any> =
                        Gson().fromJson(response, object :
                            TypeToken<Array<Any>>() {}.type)

                    val objects = data[1] as? MutableList<*> ?: emptyList()
                    val postsType = object : TypeToken<Array<DataClasses.Post>?>() {}.type
                    val postsJsonString = Gson().toJson(objects)
                    val posts: Array<DataClasses.Post>? = Gson().fromJson(postsJsonString, postsType)

                    val resultPair = Pair(data[0] as Boolean, posts)

                    Log.d("test1", resultPair.toString())

                    it.resume(resultPair)
                }
            }
        }// servs
        suspend fun getNewPosts(groupId: String, lastId: Int?): Array<DataClasses.Post>? {
            return suspendCoroutine {
                Tech().requestGETAuth("getNewPosts", "gid=$groupId&lastGot=$lastId") { response ->
                    if (response == null) {
                        it.resume(null)
                        return@requestGETAuth
                    }

                    val data: Array<DataClasses.Post>? =
                        Gson().fromJson(response, object :
                            TypeToken<Array<DataClasses.Post>?>() {}.type)

                    Log.d("test", data?.toMutableList().toString())

                    it.resume(if (data?.get(0) == null) null else data)
                }
            }
        }// servs
        suspend fun getPostComments(groupId: String, postId: Int, lastComment: Int?): MutableList<DataClasses.Comment> {
            return suspendCoroutine {
                Tech().requestGETAuth("getComments", "gid=$groupId&pid=$postId&lastGot=$lastComment") { response ->
                    if (response == null) {
                        it.resume(mutableListOf())
                        return@requestGETAuth
                    }


                    val data = Gson().fromJson(response, Array<DataClasses.Comment>::class.java)

                    it.resume(data.toMutableList())
                }
            }
        }// servs


        suspend fun getUserRoleInGroup(groupId: String): Int {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserRoleInGroup", "gid=$groupId") { response ->
                    if (response == null) {
                        it.resume(4)
                        return@requestGETAuth
                    }

                    it.resume(Gson().fromJson(response, Array<Int>::class.java)[0])
                }
            }
        }

        suspend fun sendComment(groupId: String, postId: Int, textContent: String?, image: Bitmap?): Boolean {
            return suspendCoroutine {
                Tech().requestPOST("sendComment", Gson().toJson(
                    arrayOf((textContent))
                ), "gid=$groupId&postId=$postId", image) { response ->
                    if (response == null) {
                        it.resume(false)
                        return@requestPOST
                    }

                    val responseBody = response.body?.string() ?: ""

                    it.resume(Gson().fromJson(responseBody, Array<Boolean>::class.java)[0])
                }
            }
        }// servs

        suspend fun getNumComments(groupId: String, postId: Int): Int {
            return suspendCoroutine {
                Tech().requestGETAuth("getPostNUmComments", "gid=$groupId&pid=$postId") { response ->
                    if (response == null) {
                        it.resume(0)
                        return@requestGETAuth
                    }

                    it.resume(Gson().fromJson(response, Array<Int>::class.java)[0])
                }
            }
        }


        suspend fun createPost(groupId: String, textContent: String?, image: Bitmap?): Boolean {
            return suspendCoroutine {
                Tech().requestPOST("createPost", Gson().toJson(
                    arrayOf(textContent)
                ), "gid=$groupId", image) { response ->

                    if (response == null) {
                        Log.d("null response", "null")
                        it.resume(false)
                        return@requestPOST
                    }
                    val responseBody = response.body?.string() ?: ""

                    Log.d("non null response", "tf")
                    it.resume(Gson().fromJson(responseBody, Array<Boolean>::class.java)[0])

                }
            }
        }// servs

        suspend fun deleteGroup(groupId: String): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("deleteGroup", "gid=$groupId") { response ->
                    if (response == null) {
                        it.resume(false)
                        return@requestGETAuth
                    }

                    it.resume(Gson().fromJson(response, Array<Boolean>::class.java)[0])
                }
            }
        }

        suspend fun deletePost(groupId: String, postId: Int): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("deletePost", "gid=$groupId&pid=$postId") { response ->
                    if (response == null) {
                        it.resume(false)
                        return@requestGETAuth
                    }

                    it.resume(Gson().fromJson(response, Array<Boolean>::class.java)[0])
                }
            }
        }

        suspend fun deleteComment(groupId: String, postId: Int, commentId: Int): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("deleteComment", "gid=$groupId&pid=$postId&comment=$commentId") { response ->
                    if (response == null) {
                        it.resume(false)
                        return@requestGETAuth
                    }

                    it.resume(Gson().fromJson(response, Array<Boolean>::class.java)[0])
                }
            }
        }

        suspend fun findUsersWithFilters(
            filters: String,
            newEventId: String?,
            name: String?
        ): Pair<Pair<String?, Boolean>, MutableList<DataClasses.FiltersFriendship>> {

            return suspendCoroutine {
                Tech().requestGET("getAllUsers", "filters=$filters&nei=$newEventId&name=$name") { response ->
                    if (response == null) {
                        return@requestGET
                    }

                    val responseBody = response.body?.string() ?: ""

                    Log.d("jfijgf", responseBody)


                    val type = object : TypeToken<Array<Any>>() {}.type
                    val dataList: Array<Any> = Gson().fromJson(responseBody, type)

                    val objects = dataList[1] as? MutableList<*> ?: emptyList()
                    val eventsType = object : TypeToken<Array<DataClasses.FiltersFriendship>>() {}.type
                    val eventsJsonString = Gson().toJson(objects)
                    val events: Array<DataClasses.FiltersFriendship> = Gson().fromJson(eventsJsonString, eventsType)
                    val last = if (events.isNotEmpty()) events.last().userId else null

                    it.resume(Pair(Pair(last, dataList[0] as? Boolean ?: false), events.toMutableList()))
                }
            }
        }

        suspend fun createEvent(eventData: MutableList<Any>): String {
            return suspendCoroutine { eventId ->
                Tech().requestPOST("createEvent", Gson().toJson(eventData), "") {
                    eventId.resume(it.toString())
                }
            }
        }// servs

        suspend fun findEventsWithFilters(
            filters: String,
            newEventId: String?
        ): Pair<Pair<String?, Boolean>, MutableList<DataClasses.Event>> {

            return suspendCoroutine {
                Tech().requestGET("getAllEvents", "filters=$filters&nei=$newEventId") { response ->
                    if (response == null) {
                        return@requestGET
                    }

                    val responseBody = response.body?.string() ?: ""

                    Log.d("jfijgf", responseBody)


                    val type = object : TypeToken<Array<Any>>() {}.type
                    val dataList: Array<Any> = Gson().fromJson(responseBody, type)

                    val objects = dataList[1] as? MutableList<*> ?: emptyList()
                    val eventsType = object : TypeToken<Array<DataClasses.Event>>() {}.type
                    val eventsJsonString = Gson().toJson(objects)
                    val events: Array<DataClasses.Event> = Gson().fromJson(eventsJsonString, eventsType)
                    val last = if (events.isNotEmpty()) events.last().eventId else null

                    it.resume(Pair(Pair(last, dataList[0] as? Boolean ?: false), events.toMutableList()))
                }
            }
        }
        suspend fun findGroupsWithFilters(
            filters: String,
            newGroupId: String?
        ): Pair<Pair<String?, Boolean>, MutableList<DataClasses.Group>> {

            return suspendCoroutine {
                Tech().requestGET("getAllGroups", "filters=$filters&nei=$newGroupId") { response ->
                    if (response == null) {
                        return@requestGET
                    }

                    val responseBody = response.body?.string() ?: ""

                    val type = object : TypeToken<Array<Any>>() {}.type
                    val dataList: Array<Any> = Gson().fromJson(responseBody, type)

                    val objects = dataList[1] as? MutableList<*> ?: emptyList()
                    val groupsType = object : TypeToken<Array<DataClasses.Group>>() {}.type
                    val groupsJsonString = Gson().toJson(objects)
                    val events: Array<DataClasses.Group> = Gson().fromJson(groupsJsonString, groupsType)
                    val last = if (events.isNotEmpty()) events.last().groupId else null

                    it.resume(Pair(Pair(last, dataList[0] as? Boolean ?: false), events.toMutableList()))
                }
            }
        }

        suspend fun getWebNews(undesirable: String): MutableList<HashMap<String, String?>> {
            return suspendCoroutine {
                Tech().requestGET("getWeb", "bad=$undesirable") { response ->
                    if (response == null) return@requestGET

                    val responseBody = response.body?.string()

                    val typeToken = object : TypeToken<Array<HashMap<String, String?>>>() {}.type
                    val data: Array<HashMap<String, String?>> = Gson().fromJson(responseBody, typeToken)

                    it.resume(data.toMutableList())
                }
            }
        }

        suspend fun getTranslation(url: String): JsonObject {
            return suspendCoroutine { finalIt ->
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .build()

                Log.d("final url", request.url.toString())

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!response.isSuccessful) {
                                Log.d("response", "Not successful: ${response.code}")
                                finalIt.resume(JsonObject())
                                return
                            }

                            val responseBody = response.body?.string() ?: ""
                            val jsonObject = Gson().fromJson(responseBody, JsonObject::class.java)
                            finalIt.resume(jsonObject)
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        Log.d("bad request", "Request failed: ${e.message}")
                        finalIt.resume(JsonObject())
                    }
                })
            }
        }


        suspend fun getEventGoals(eventId: String): MutableList<String> {
            return suspendCoroutine {
                Tech().requestGET("getEventGoals", "eventId=$eventId") { response ->
                    if (response == null) {
                        return@requestGET
                    }

                    val responseBody = response.body?.string() ?: ""


                    Log.d("parseing", responseBody)
                    val dataList: Array<String> = Gson().fromJson(responseBody, Array<String>::class.java)

                    it.resume(dataList.toMutableList())
                }
            }
        }
        suspend fun getEventTimes(eventId: String): HashMap<String, String> {
            return suspendCoroutine {
                Tech().requestGET("getEventTimes", "eventId=$eventId") { response ->
                    if (response == null) {
                        return@requestGET
                    }

                    val responseBody = response.body?.string() ?: ""


                    Log.d("parseing", responseBody)
                    val dataList: HashMap<String, String> = Gson().fromJson(responseBody, object : TypeToken<HashMap<String, String>>() {}.type)

                    it.resume(dataList)
                }
            }
        }
        suspend fun getEventCoords(eventId: String): MutableList<DataClasses.MapObject> {
            return suspendCoroutine {
                Tech().requestGET("getEventCoords", "eventId=$eventId") { response ->
                    if (response == null) {
                        return@requestGET
                    }

                    val responseBody = response.body?.string() ?: ""


                    Log.d("parseing", responseBody)
                    val dataList: Array<DataClasses.MapObject> = Gson().fromJson(responseBody, Array<DataClasses.MapObject>::class.java)

                    it.resume(dataList.toMutableList())
                }
            }
        }
        suspend fun getEventMembers(eventId: String, startAfter: String?, username: String?): MutableList<UserDatabaseMethods.UserActivity> {
            return suspendCoroutine {
                Tech().requestGET("getEventMembers", "eventId=$eventId&startAfter=$startAfter&username=$username") { response ->
                    if (response == null) {
                        return@requestGET
                    }

                    val responseBody = response.body?.string() ?: ""


                    Log.d("parseing", responseBody)
                    val dataList: Array<UserDatabaseMethods.UserActivity> = Gson().fromJson(responseBody, Array<UserDatabaseMethods.UserActivity>::class.java)

                    it.resume(dataList.toMutableList())
                }
            }
        }
        suspend fun isUserModerInEvent(eventId: String): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("isUserModerInEvent", "eventId=$eventId") { response ->
                    if (response == null) {
                        return@requestGETAuth
                    }


                    Log.d("parseing", response)
                    val dataList: Array<Boolean> = Gson().fromJson(response, Array<Boolean>::class.java)

                    it.resume(dataList[0])
                }
            }
        }
        suspend fun isUserValidated(eventId: String): Boolean {
            return suspendCoroutine {
                Tech().requestGETAuth("isUserValidated", "eventId=$eventId") { response ->
                    if (response == null) {
                        return@requestGETAuth
                    }


                    Log.d("parseing", response)
                    val dataList: Array<Boolean> = Gson().fromJson(response, Array<Boolean>::class.java)

                    it.resume(dataList[0])
                }
            }
        }
        suspend fun validateUser(userId: String, eventId: String): Boolean {
            return suspendCoroutine {
                Tech().requestPOST("validateUser", "", "uid=$userId&eid=$eventId") { response ->
                    it.resume(response != null)
                }
            }
        }




        fun getImageLink(folder: String, imageId: String): String {
            return "${BuildConfig.SERVER_API_URI}uploads/$folder/$imageId"
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

        suspend fun getUserRating(inCountry: Boolean): MutableList<DataClasses.Rating> {
            return suspendCoroutine {
                Tech().requestGETAuth("getUserRating", "country=$inCountry") { response ->

                    if (response == null) {
                        return@requestGETAuth
                    }

                    val data = Gson().fromJson(response, Array<DataClasses.Rating>::class.java)

                    Log.d("test", data.toString())

                    it.resume(data.toMutableList())
                }
            }
        }

    }// servs

    class Account {

        private fun insertUserData(userData: UserDatabaseMethods.User, uid: String, callback: (Boolean) -> Unit) {
            Tech().requestPOST("register", Gson().toJson(userData), ""){
                callback(it != null)
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

                        if (user == null) {
                            callback(null)
                            return@addOnCompleteListener
                        }
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val token = task.result
                                val userDevices = FirebaseFirestore.getInstance().collection("users").document(user.uid).collection("notifications")
                                userDevices.document(token).set(mapOf("token" to token, "timestamp" to System.currentTimeMillis()))
                                    .addOnSuccessListener {
                                        callback(user.uid)
                                    }
                                    .addOnFailureListener {
                                        callback(null)
                                    }
                            }
                        }
                    } else {
                        Toast.makeText(Tech().context(), "Не удалось войти в аккаунт!", Toast.LENGTH_LONG).show()
                        callback(null) // auth error
                    }
                }
        }

        fun signOut(): Boolean {
            val auth = FirebaseAuth.getInstance()
            auth.signOut()

            Globals.getInstance().setString("CurrentlyWatching", "0")

            return true
        }
    }
}
