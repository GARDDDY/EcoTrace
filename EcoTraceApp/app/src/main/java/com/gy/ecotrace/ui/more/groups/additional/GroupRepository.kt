package com.gy.ecotrace.ui.more.groups.additional

import android.graphics.Bitmap
import com.gy.ecotrace.db.DatabaseMethods
import com.gy.ecotrace.db.Repository

class GroupRepository {
    class DataStorage {
        var groupData: DatabaseMethods.DataClasses.Group =
            DatabaseMethods.DataClasses.Group()

        var groupPosts: MutableMap<String, DatabaseMethods.DataClasses.Post> =
            mutableMapOf()
    }

    class Functions {

        val repository = Repository(DatabaseMethods.UserDatabaseMethods(), DatabaseMethods.ApplicationDatabaseMethods())



        fun userAbilitiesInGroup(roleLevel: Int): DatabaseMethods.DataClasses.UserGroupAbilities {
            return when (roleLevel) {
                2 -> DatabaseMethods.DataClasses.UserGroupAbilities(deletePosts = true)
                1 -> DatabaseMethods.DataClasses.UserGroupAbilities(editGroup = true, deletePosts = true, manageUsers = 2, kickUsers = true)
                0 -> DatabaseMethods.DataClasses.UserGroupAbilities(editGroup = true, deletePosts = true, manageUsers = 0, kickUsers = true)
                else -> DatabaseMethods.DataClasses.UserGroupAbilities()
            }
        }


        // suspend

        suspend fun deletePost(groupId: String, postId: String) {

        }

        suspend fun userRoleAtLeastModerator(userId: String, groupId: String): Int {
            return repository.userGroupRoleAtLeastModerator(userId, groupId)
        }



        suspend fun uploadImage(folder: String, imageId: String, imageData: Bitmap): String {
            return repository.uploadImage(folder, imageId, imageData)
        }


        fun createPost(groupId: String, userId: String, textContent: String?, imageURI: String?) {
            if (textContent.isNullOrEmpty() && imageURI.isNullOrBlank()) {
                return
            }

            repository.createPost(groupId, userId, textContent, imageURI)
        }


        suspend fun getUsernameOnly(userId: String): String {
            return repository.getUsernameOnly(userId)
        }

    }
}