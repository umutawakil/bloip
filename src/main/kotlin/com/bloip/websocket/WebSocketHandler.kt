package com.bloip.websocket

import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.*
import javax.annotation.PostConstruct
import kotlin.collections.HashMap

/**
 * Created by Usman Mutawakil on 10/27/22.
 */

@Component
class WebSocketHandler: TextWebSocketHandler() {
    val userIdToWebSocketSession: MutableMap<Long, WebSocketSession> = Collections.synchronizedMap(HashMap())
    val secretKeyToUserId: MutableMap<String, Long> = Collections.synchronizedMap(HashMap())
    val webSocketSessionToSecretKey: MutableMap<WebSocketSession, String> = Collections.synchronizedMap(HashMap())

    @PostConstruct
    fun init() {
        println("Websocket handler functioning as a bean?????.... Hold onto your butts!!!!!!")
    }

    fun mapSecretAndUserId(secretKey: String, userId: Long) {
        secretKeyToUserId[secretKey] = userId
    }

    fun mapSecretKeyToWebSocketSession(secretKey: String, webSocketSession: WebSocketSession) {
        val userId = secretKeyToUserId[secretKey] ?: return
        userIdToWebSocketSession[userId] = webSocketSession
        secretKeyToUserId[secretKey] = userId
    }

    override fun handleMessage(webSocketSession: WebSocketSession, message: WebSocketMessage<*>) {
        val secretKey: String = message.payload as String
        mapSecretKeyToWebSocketSession(secretKey = secretKey, webSocketSession = webSocketSession)
    }

    fun sendInboxAlert(userId: Long, count: Int) {
        val webSocketSession: WebSocketSession = userIdToWebSocketSession[userId] ?: return

        println("Sending alert!!!")

        webSocketSession.sendMessage(TextMessage("$count"))
    }

    override fun afterConnectionEstablished(webSocketSession: WebSocketSession) {
         println("New websocket connection initiated");
    }

    override fun afterConnectionClosed(webSocketSession: WebSocketSession, status: CloseStatus) {
        val secretKey: String = webSocketSessionToSecretKey[webSocketSession] ?: return
        webSocketSessionToSecretKey.remove(webSocketSession)

        val userId: Long = secretKeyToUserId[secretKey] ?: return
        secretKeyToUserId.remove(secretKey)

        userIdToWebSocketSession.remove(userId)
    }

}