package com.gy.ecotrace.network

import com.google.gson.Gson
import com.gy.ecotrace.BuildConfig
import com.gy.ecotrace.network.dto.GroupComment
import com.gy.ecotrace.network.dto.GroupPost
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PostEvent {
    data class New(val post: GroupPost) : PostEvent
    data class Deleted(val id: Long) : PostEvent
}

sealed interface CommentEvent {
    data class New(val comment: GroupComment) : CommentEvent
    data class Deleted(val id: Long) : CommentEvent
}

@Singleton
class RealtimeClient @Inject constructor(
    private val tokenStore: TokenStore,
    private val gson: Gson
) {
    @Volatile
    private var socket: Socket? = null

    @Synchronized
    private fun socket(): Socket {
        socket?.let { return it }
        val opts = IO.Options().apply {
            auth = mapOf("token" to (tokenStore.token ?: ""))
            reconnection = true
        }
        return IO.socket(BuildConfig.SERVER_API_URI, opts).also { socket = it }
    }

    fun groupPostEvents(groupId: Long): Flow<PostEvent> = callbackFlow {
        val s = socket()
        val channel = "group:$groupId:posts"

        val onNew = Emitter.Listener { args ->
            val json = args.getOrNull(0) as? JSONObject ?: return@Listener
            val post = runCatching { gson.fromJson(json.toString(), GroupPost::class.java) }.getOrNull()
            // Shared socket receives all groups' events; filter client-side by groupId.
            if (post != null && post.groupId == groupId) trySend(PostEvent.New(post))
        }
        val onDeleted = Emitter.Listener { args ->
            val json = args.getOrNull(0) as? JSONObject ?: return@Listener
            val id = json.optLong("id", -1L)
            if (id > 0) trySend(PostEvent.Deleted(id))
        }
        val onConnect = Emitter.Listener { s.emit("subscribe", channel) }

        s.on("post:new", onNew)
        s.on("post:deleted", onDeleted)
        s.on(Socket.EVENT_CONNECT, onConnect)
        if (s.connected()) s.emit("subscribe", channel) else s.connect()

        awaitClose {
            s.emit("unsubscribe", channel)
            s.off("post:new", onNew)
            s.off("post:deleted", onDeleted)
            s.off(Socket.EVENT_CONNECT, onConnect)
        }
    }

    fun commentEvents(postId: Long): Flow<CommentEvent> = callbackFlow {
        val s = socket()
        val channel = "post:$postId:comments"

        val onNew = Emitter.Listener { args ->
            val json = args.getOrNull(0) as? JSONObject ?: return@Listener
            val comment = runCatching { gson.fromJson(json.toString(), GroupComment::class.java) }.getOrNull()
            if (comment != null && comment.postId == postId) trySend(CommentEvent.New(comment))
        }
        val onDeleted = Emitter.Listener { args ->
            val json = args.getOrNull(0) as? JSONObject ?: return@Listener
            val id = json.optLong("id", -1L)
            if (id > 0) trySend(CommentEvent.Deleted(id))
        }
        val onConnect = Emitter.Listener { s.emit("subscribe", channel) }

        s.on("comment:new", onNew)
        s.on("comment:deleted", onDeleted)
        s.on(Socket.EVENT_CONNECT, onConnect)
        if (s.connected()) s.emit("subscribe", channel) else s.connect()

        awaitClose {
            s.emit("unsubscribe", channel)
            s.off("comment:new", onNew)
            s.off("comment:deleted", onDeleted)
            s.off(Socket.EVENT_CONNECT, onConnect)
        }
    }
}
