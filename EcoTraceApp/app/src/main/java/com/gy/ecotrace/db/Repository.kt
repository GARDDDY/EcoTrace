package com.gy.ecotrace.db

import android.graphics.Bitmap
import android.util.Log

class Repository(
    private val userDatabase: DatabaseMethods.UserDatabaseMethods,
    private val appDatabase: DatabaseMethods.ApplicationDatabaseMethods
) {
    suspend fun getUser(userId: String): DatabaseMethods.UserDatabaseMethods.User? {
        return userDatabase.getUserInfo(userId)
    }

    suspend fun getUserEmail(login: String, password: String): String? {
        return userDatabase.getUserEmail(login, password)
    }

    fun _user(): DatabaseMethods.UserDatabaseMethods.User {
        return DatabaseMethods.UserDatabaseMethods.User()
    }

    suspend fun setUser(data: DatabaseMethods.UserDatabaseMethods.User, userId: String, userPass: String) {

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

    suspend fun getUserEvents(userId: String, eGot: String?): MutableList<DatabaseMethods.UserDatabaseMethods.UserEvent>? {
        val userEvents = userDatabase.getUserEvents(userId, eGot) ?: return null
        val eventsData = mutableListOf<DatabaseMethods.UserDatabaseMethods.UserEvent>()
        userEvents.forEach {
            eventsData.add(DatabaseMethods.UserDatabaseMethods.UserEvent(getEvent(it.key), it.value))
        }
        return eventsData
    }

    fun observeEventMembers(groupId: String,
                            callback: (DatabaseMethods.DataClasses.Event?) -> Unit) {

        return appDatabase.observeEvent(groupId, callback)

    }

    suspend fun getUsernameOnly(userId: String): String {
        return userDatabase.getUsernameOnly(userId)
    }



    suspend fun getUserFriends(userId: String, fGot: String?): MutableList<DatabaseMethods.DataClasses.Friendship>? {
        return userDatabase.getUserFriends(userId, fGot)
    }


    suspend fun getUserGroups(userId: String, gGot: String?): MutableList<DatabaseMethods.UserDatabaseMethods.UserGroup>? {
        val userGroups = userDatabase.getUserGroups(userId, gGot) ?: return null
        val groupsData = mutableListOf<DatabaseMethods.UserDatabaseMethods.UserGroup>()
        userGroups.forEach {
            groupsData.add(DatabaseMethods.UserDatabaseMethods.UserGroup(it.value, getGroup(it.key)))
        }

        return groupsData
    }

    fun observeGroupMembers(groupId: String,
                            callback: (DatabaseMethods.DataClasses.Group?) -> Unit) {

        return appDatabase.observeGroup(groupId, callback)
    }

    fun removeFriend(userId: String) {
        userDatabase.removeFriends(userId)
    }
    fun addFriend(userId: String) {
        userDatabase.addFriends(userId)
    }

    data class UserEventInfoShort(
        var userId: String = "error-userId",
        var username: String = "error-username",
        var userRole: Int = 0,
        var userBestGroupName: String = "error-group",
        var userBestGroup: String = "0",
        var userRank: String = "error-rank",

        var userExperience: Int = 0
    )

    suspend fun getUserShortInfoForEventMembersLayout(userId: String) : UserEventInfoShort {
        val userClass = UserEventInfoShort()
//        val userInfo = userDatabase.getUserInfo(userId)
//        userClass.userId = userId
//        userClass.username = userInfo.username
//        userClass.userRank = DatabaseMethods.DataClasses.UserRanks[0]
//        userClass.userExperience = userInfo.experience
//
//        val userGroupsIds = userDatabase.getUserInfo(userId).groups
//        val userGroups = mutableListOf<DatabaseMethods.DataClasses.Group>()
//        if (userGroupsIds != null) {
//            for (id in userGroupsIds.keys) {
//                val data = appDatabase.getGroup(id)
//                userGroups.add(data)
//            }
//        }
//
//        val bestGroupInExperience = userGroups.maxByOrNull { it.groupExperience }
//        if (bestGroupInExperience != null) {
//            userClass.userBestGroupName = bestGroupInExperience.groupName
//            userClass.userBestGroup = bestGroupInExperience.groupId
//        }
//        else userClass.userBestGroupName = "Группа не найдена"
        return userClass
    }

    suspend fun getEvent(eventId: String): DatabaseMethods.DataClasses.Event {
        return appDatabase.getEvent(eventId)
    }

    suspend fun findUsersWithFilters(filters: MutableList<Int>, lastUser: String?): Pair<String?, MutableList<DatabaseMethods.DataClasses.FiltersFriendship>> {
        return appDatabase.findUsersWithFilters(filters, lastUser)
    }

    suspend fun findEventsWithFilters(filters: MutableList<Int>, lastEventId: Pair<Boolean, String?>, string: String?): Pair<MutableList<DatabaseMethods.DataClasses.Event>, Pair<Boolean, String?>> {
        return appDatabase.findEventsWithFilters(filters, lastEventId, string)
    }

    suspend fun getEventMore(eventId: String): DatabaseMethods.ApplicationDatabaseMethods.EventMore {
        return appDatabase.getEventMore(eventId)
    }

    suspend fun joinEvent(eventId: String, userId: String) {
        userDatabase.joinEvent(eventId, userId)
    }
    suspend fun leaveEvent(eventId: String, userId: String) {
        userDatabase.leaveEvent(eventId, userId)
    }

    suspend fun getGroup(groupId: String): DatabaseMethods.DataClasses.Group {
        return appDatabase.getGroup(groupId)
    }

    suspend fun getPosts(groupId: String, lastId: Triple<Boolean, String?, Int>): Pair<MutableList<DatabaseMethods.DataClasses.Post>, Triple<Boolean, String?, Int>> {
        return appDatabase.getPosts(groupId, lastId)
    }

    suspend fun getComments(groupId: String, postId: String, lastComment: Pair<String?, String?>): Pair<MutableList<DatabaseMethods.DataClasses.Comment>, Pair<String, String>> {
        return appDatabase.getPostComments(groupId, postId, lastComment)
    }

    fun observePosts(groupId: String, showedPosts: MutableList<String>, lastFound: String, callback: (Pair<Boolean, DatabaseMethods.DataClasses.Post>) -> Unit) {
        return appDatabase.observePosts(groupId, showedPosts, lastFound, callback)
    }

    fun sendComment(groupId: String, postId: String, userId: String, textContent: String?, imageURI: String?) {
        appDatabase.sendComment(groupId, postId, userId, textContent, imageURI)
    }

    fun createPost(groupId: String, userId: String, textContent: String?, imageURI: String?) {
        appDatabase.createPost(groupId, userId, textContent, imageURI)
    }

    /*suspend*/ fun getImage(folder: String, imageId: String): String {
        return appDatabase.getImageLink(folder, imageId)
    }

    suspend fun uploadImage(folder: String, imageId: String, imageData: Bitmap): String {
        return appDatabase.uploadImage(folder, imageId, imageData)
    }

    suspend fun joinGroup(userId: String, groupId: String): Boolean {
        return userDatabase.joinGroup(userId, groupId)
    }
    suspend fun leaveGroup(userId: String, groupId: String): Boolean {
        return userDatabase.leaveGroup(userId, groupId)
    }
    suspend fun isUserInGroup(userId: String, groupId: String): Boolean {
        return userDatabase.isUserInGroup(userId, groupId)
    }
    suspend fun getGroupUsers(groupId: String, lastGot: String?): Pair<String, HashMap<String, HashMap<String, Boolean>>> {
        return appDatabase.getGroupMembers(groupId, lastGot)
    }
    fun setUserRoleInGroup(groupId: String, userId: String, roleFrom: Int, roleTo: Int){
        appDatabase.setUserRoleInGroup(groupId, userId, roleFrom, roleTo)
    }
    fun kickUserFromGroup(groupId: String, userId: String, role: Int) {
        appDatabase.kickUserFromGroup(groupId, userId, role)
    }
    suspend fun userGroupRoleAtLeastModerator(userId: String, groupId: String): Int {
        return appDatabase.userGroupRoleAtLeastModerator(userId, groupId)
    }
    suspend fun isGroupNameAvailable(groupName: String): Boolean {
        return appDatabase.isGroupNameAvailable(groupName)
    }
    suspend fun createGroup(groupData: DatabaseMethods.DataClasses.Group) {
        appDatabase.createGroup(groupData)
    }
    suspend fun getUserRating(userId: String): MutableList<DatabaseMethods.DataClasses.Rating> {
        return appDatabase.getUserRating(userId)
    }


}