package com.gy.ecotrace.db

import android.util.Log
import kotlin.math.floor

class Repository(
    private val userDatabase: DatabaseMethods.UserDatabaseMethods,
    private val appDatabase: DatabaseMethods.ApplicationDatabaseMethods
) {
    suspend fun getUser(userId: String): DatabaseMethods.UserDatabaseMethods.UserInfo {
        Log.d("Interval", "repos")
        return userDatabase.getUserInfo(userId)!!
    }

    fun _user(): DatabaseMethods.UserDatabaseMethods.UserInfo {
        return DatabaseMethods.UserDatabaseMethods.UserInfo()
    }

    suspend fun setUser(data: DatabaseMethods.UserDatabaseMethods.UserInfo, userId: String, userPass: String) {

    }


    suspend fun getPrivate(userId: String, userPass: String){//: DatabaseMethods.UserDatabaseMethods.UserPrivate? {

    }

    fun _private(): DatabaseMethods.UserDatabaseMethods.UserPrivate {
        return DatabaseMethods.UserDatabaseMethods.UserPrivate()
    }

    suspend fun setPrivate(data: DatabaseMethods.UserDatabaseMethods.UserPrivate, userId: String, userPass: String) {

    }


    suspend fun getRules(userId: String){//: DatabaseMethods.UserDatabaseMethods.UserRules {

    }

    fun _rules(): DatabaseMethods.UserDatabaseMethods.UserRules {
        return DatabaseMethods.UserDatabaseMethods.UserRules()
    }

    suspend fun setRules(data: DatabaseMethods.UserDatabaseMethods.UserRules, userId: String, userPass: String) {

    }



    suspend fun getEvents(userId: String): MutableList<DatabaseMethods.UserDatabaseMethods.UserActivity> {
        return userDatabase.getUserEvents(userId)
    }



    suspend fun getFriends(userId: String): MutableList<DatabaseMethods.DataClasses.Friendship> {
        return userDatabase.getUserFriends(userId)
    }

    fun _group(): DatabaseMethods.UserDatabaseMethods.UserGroups {
        return DatabaseMethods.UserDatabaseMethods.UserGroups()
    }

    suspend fun getGroups(userId: String): MutableList<DatabaseMethods.UserDatabaseMethods.UserGroups> {
        val groupIds = userDatabase.getUserGroups(userId)
        val groups = mutableListOf<DatabaseMethods.UserDatabaseMethods.UserGroups>()
        for ((key, value) in groupIds) {
            val userGroupClass = _group()
            val groupInfo = appDatabase.getGroup(key)
            userGroupClass.groupInfo = groupInfo
            userGroupClass.isUserInGroup = value
            groups.add(userGroupClass)
        }
        return groups
    }

    fun observeGroupMembers(groupId: String,
                            callback: (DatabaseMethods.DataClasses.Group?) -> Unit) {

        return appDatabase.observeGroup(groupId, callback)

    }

    suspend fun getMaximums(userId: String): MutableList<Int> {
        return userDatabase.getMaximums(userId)
    }

    fun removeFriend(userId1: String, userId2: String) {
        userDatabase.removeFriends(userId1, userId2)
    }
    fun addFriend(userId1: String, userId2: String) {
        userDatabase.addFriends(userId1, userId2)
    }

    // app

        data class UserEventInfoShort(
            var userId: String = "error-userId",
            var username: String = "error-username",
            var userRole: Int = 0,
            var userBestGroupName: String = "error-group",
            var userRank: String = "error-rank",

            var userExperience: Int = 0
        )

        suspend fun getUserShortInfoForEventMembersLayout(userId: String) : UserEventInfoShort {
            val userClass = UserEventInfoShort()
            val userInfo = userDatabase.getUserInfo(userId)
            userClass.userId = userId
            userClass.username = userInfo.username
            userClass.userRank = DatabaseMethods.DataClasses.UserRanks[floor(userInfo.experience / 1000.0).toInt()]
            userClass.userExperience = userInfo.experience

            val userGroupsIds = userDatabase.getUserGroups(userId)
            val userGroups = mutableListOf<DatabaseMethods.DataClasses.Group>()
            for (id in userGroupsIds.keys) {
                val data = appDatabase.getGroup(id)
                userGroups.add(data)
            }

            val bestGroupInExperience = userGroups.maxByOrNull { it.groupExperience }
            if (bestGroupInExperience != null) userClass.userBestGroupName = bestGroupInExperience.groupName
            else userClass.userBestGroupName = "Группа не найдена"
//
            return userClass
        }

        suspend fun getEvent(eventId: String): DatabaseMethods.DataClasses.Event {
            return appDatabase.getEvent(eventId)
        }

        suspend fun findUsersWithFilters(filters: MutableList<Int>, lastUser: String?): Pair<String?, MutableList<DatabaseMethods.DataClasses.FiltersFriendship>> {
            return appDatabase.findUsersWithFilters(filters, lastUser)
        }

}