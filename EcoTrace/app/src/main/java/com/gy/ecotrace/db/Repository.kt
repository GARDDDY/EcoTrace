package com.gy.ecotrace.db

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.JsonObject
import com.gy.ecotrace.db.DatabaseMethods.DataClasses
import com.gy.ecotrace.db.DatabaseMethods.UserDatabaseMethods

class Repository(
    private val userDatabase: DatabaseMethods.UserDatabaseMethods,
    private val appDatabase: DatabaseMethods.ApplicationDatabaseMethods
) {
    suspend fun getUser(userId: String?): DatabaseMethods.UserDatabaseMethods.User? {
        return userDatabase.getUserInfo(userId)
    }

    suspend fun getGraph(userId: String, time: Int, hideFilters: MutableList<Int>?): Bitmap? {
        return userDatabase.getGraph(userId, time, hideFilters)
    }

    suspend fun getUserEmail(login: String, password: String): String? {
        return userDatabase.getUserEmail(login, password)
    }

    suspend fun getUserUpdates(since: Long): Pair<MutableList<Int>, String> {
        return userDatabase.getUpdates(since)
    }

    suspend fun getWebNews(undesirable: String): MutableList<DataClasses.News> {
        return appDatabase.getWebNews(undesirable)
    }

    suspend fun getUserEducations(): MutableList<Int> {
        return userDatabase.getEducations()
    }

    suspend fun getTranslation(url: String): JsonObject {
        return appDatabase.getTranslation(url)
    }

    suspend fun setUser(data: DatabaseMethods.UserDatabaseMethods.User, userId: String, userPass: String) {

    }


    suspend fun getPrivate(): UserDatabaseMethods.UserPrivate {
        return userDatabase.getUserPrivate()
    }


    suspend fun getRules(): MutableMap<String, Int> {
        return userDatabase.getUserRules()
    }

    suspend fun getUserEvents(userId: String, eGot: String?, sort: Int): MutableList<DatabaseMethods.UserDatabaseMethods.UserEvent>? {
        return userDatabase.getUserEvents(userId, eGot, sort)
    }


    suspend fun getUsernameOnly(userId: String): String {
        return userDatabase.getUsernameOnly(userId)
    }



    suspend fun getUserFriends(userId: String, fGot: String?): MutableList<DatabaseMethods.DataClasses.Friendship> {
        return userDatabase.getUserFriends(userId, fGot)
    }


    suspend fun getUserGroups(userId: String, gGot: String?): MutableList<UserDatabaseMethods.UserGroup> {
        return userDatabase.getUserGroups(userId, gGot)
    }

    fun removeFriend(userId: String) {
        userDatabase.removeFriends(userId)
    }
    fun addFriend(userId: String) {
        userDatabase.addFriends(userId)
    }

    suspend fun areUsersFriends(userId: String): Int {
        return userDatabase.areUsersFriends(userId)
    }

    suspend fun sendEdu(eduType: String): Boolean {
        return userDatabase.sendEdu(Regex("\\d+").find(eduType)?.value?.toInt() ?: -1)
    }

    suspend fun getEvent(eventId: String): DatabaseMethods.DataClasses.Event? {
        return appDatabase.getEvent(eventId)
    }
    suspend fun createEvent(eventData: MutableList<Any>, eventImage: Bitmap?): String? {
        return appDatabase.createEvent(eventData, eventImage)
    }
    suspend fun isUserValidated(eventId: String): Boolean {
        return appDatabase.isUserValidated(eventId)
    }
    suspend fun validateUser(userId: String, eventId: String): Boolean {
        return appDatabase.validateUser(userId, eventId)
    }

    suspend fun findUsersWithFilters(filters: String, lastUser: String?, name: String?): Pair<Pair<String?, Boolean>, MutableList<DataClasses.FiltersFriendship>> {
        return appDatabase.findUsersWithFilters(filters, lastUser, name)
    }
    suspend fun findEventsWithFilters(filters: String, newEventId: String?, s: Long?, e: Long?): Pair<Pair<String?, Boolean>, MutableList<DataClasses.Event>> {
        return appDatabase.findEventsWithFilters(filters, newEventId, s, e)
    }

    suspend fun getEventGoals(eventId: String): MutableList<String> {
        return appDatabase.getEventGoals(eventId)
    }
    suspend fun getEventTimes(eventId: String): HashMap<String, String> {
        return appDatabase.getEventTimes(eventId)
    }
    suspend fun getEventCoords(eventId: String): MutableList<DataClasses.MapObject> {
        return appDatabase.getEventCoords(eventId)
    }
    suspend fun getEventMembers(eventId: String, startAfter: String?, username: String?): MutableList<DatabaseMethods.UserDatabaseMethods.UserActivity> {
        return appDatabase.getEventMembers(eventId, startAfter, username)
    }

    suspend fun joinEvent(eventId: String, callback: (Boolean) -> Unit) {
        userDatabase.joinEvent(eventId) {
            callback(it)
        }
    }
    suspend fun leaveEvent(eventId: String, callback: (Boolean) -> Unit) {
        userDatabase.leaveEvent(eventId) {
            callback(it)
        }
    }

    suspend fun isUserModerInEvent(eventId: String): Boolean {
        return appDatabase.isUserModerInEvent(eventId)
    }

    suspend fun getUserEvent(eventId: String): DatabaseMethods.UserDatabaseMethods.UserEvent {
        return userDatabase.getUserEvent(eventId)
    }

    suspend fun getGroup(groupId: String): DatabaseMethods.DataClasses.Group {
        return appDatabase.getGroup(groupId)
    }
    suspend fun findGroupsWithFilters(filters: String, newEventId: String?): Pair<Pair<String?, Boolean>, MutableList<DataClasses.Group>> {
        return appDatabase.findGroupsWithFilters(filters, newEventId)
    }

    suspend fun getPosts(groupId: String, lastId: Int?): Pair<Boolean, Array<DataClasses.Post>?> {
        return appDatabase.getPosts(groupId, lastId)
    }
    suspend fun getNewPosts(groupId: String, lastNewId: Int?): Array<DataClasses.Post>? {
        return appDatabase.getNewPosts(groupId, lastNewId)
    }

    suspend fun getComments(groupId: String, postId: Int, lastComment: Int?): MutableList<DataClasses.Comment> {
        return appDatabase.getPostComments(groupId, postId, lastComment)
    }

    suspend fun getUserRoleInGroup(groupId: String): Int {
        return appDatabase.getUserRoleInGroup(groupId)
    }

    suspend fun sendComment(groupId: String, postId: Int, textContent: String?, image: Bitmap?): Boolean {
        return appDatabase.sendComment(groupId, postId, textContent, image)
    }

    suspend fun createPost(groupId: String, textContent: String?, imageData: Bitmap?): Boolean {
        return appDatabase.createPost(groupId, textContent, imageData)
    }

    suspend fun getNumComments(groupId: String, postId: Int): Int {
        return appDatabase.getNumComments(groupId, postId)
    }

    suspend fun deleteGroup(groupId: String): Boolean {
        return appDatabase.deleteGroup(groupId)
    }
    suspend fun deleteEvent(eventId: String): Boolean {
        return appDatabase.deleteEvent(eventId)
    }

    suspend fun deletePost(groupId: String, postId: Int): Boolean {
        return appDatabase.deletePost(groupId, postId)
    }
    suspend fun deleteComment(groupId: String, postId: Int, commentId: Int): Boolean {
        return appDatabase.deleteComment(groupId, postId, commentId)
    }

    /*suspend*/ fun getImage(folder: String, imageId: String): String {
        return appDatabase.getImageLink(folder, imageId)
    }

    suspend fun uploadImage(folder: String, imageId: String, imageData: Bitmap): String {
        return appDatabase.uploadImage(folder, imageId, imageData)
    }

    fun joinGroup(groupId: String, callback: (Boolean) -> Unit) {
        userDatabase.joinGroup(groupId) {
            callback(it)
        }
    }
    fun leaveGroup(groupId: String, callback: (Boolean) -> Unit) {
        userDatabase.leaveGroup(groupId) {
            callback(it)
        }
    }

    suspend fun setUserRoleInEvent(userId: String, eventId: String, role: Int): Boolean {
        return appDatabase.setUserRoleInEvent(userId, eventId, role)
    }

    suspend fun isUserInGroup(groupId: String): Boolean {
        return userDatabase.isUserInGroup(groupId)
    }
    suspend fun getGroupUsers(groupId: String, lastGot: String?, role: Int): Pair<Boolean, MutableList<DatabaseMethods.DataClasses.UserInGroup>> {
        return appDatabase.getGroupMembers(groupId, lastGot, role)
    }
    suspend fun setUserRoleInGroup(groupId: String, userId: String, role: Int): Boolean {
        return appDatabase.setUserRoleInGroup(groupId, userId, role)
    }
    suspend fun kickUserFromGroup(groupId: String, userId: String): Boolean {
        return appDatabase.kickUserFromGroup(groupId, userId)
    }
    suspend fun userGroupRoleAtLeastModerator(userId: String, groupId: String): Int {
        return appDatabase.userGroupRoleAtLeastModerator(userId, groupId)
    }
    suspend fun isGroupNameAvailable(groupName: String): String? {
        return appDatabase.isGroupNameAvailable(groupName)
    }
    suspend fun createGroup(groupData: DatabaseMethods.DataClasses.GroupChange, textRules: String?, image: Bitmap?): String? {
        return appDatabase.createGroup(groupData, textRules, image)
    }
    suspend fun getUserRating(inCountry: Boolean): MutableList<DatabaseMethods.DataClasses.Rating> {
        return appDatabase.getUserRating(inCountry)
    }
    fun saveEcoData(data: MutableList<DatabaseMethods.DataClasses.EcoCalcSaveData>, calcType: Int, callback: (String) -> Unit) {
        return userDatabase.saveEcoCalc(data, calcType, callback)
    }

    suspend fun getCountCalculators(): String {
        return userDatabase.getCountCalculators()
    }
    suspend fun getCalcImage(calcType: Int, imageId: Int): Bitmap? {
        return userDatabase.getCalcImage(calcType, imageId)
    }
    suspend fun getCalcAdvices(calcType: Int, image: Int): Array<String> {
        return userDatabase.getCalcAdvices(calcType, image)
    }

    suspend fun getGroupRules(groupId: String): MutableList<String?> {
        return appDatabase.getGroupRules(groupId)
    }

    suspend fun getEduStatus(edu: Int): Boolean? {
        return userDatabase.getEduStatus(edu)
    }




    // account

    suspend fun sendForgotCode(email: String): Boolean {
        return DatabaseMethods.Account().sendForgotCode(email)
    }

    suspend fun checkCode(email: String, code: Int): Boolean {
        return DatabaseMethods.Account().checkCode(email, code)
    }

    suspend fun applyPassword(email: String, code: Int, password: String): Boolean {
        return DatabaseMethods.Account().applyPassword(email, code, password)
    }

}