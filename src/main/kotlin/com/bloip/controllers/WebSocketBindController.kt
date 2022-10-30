package com.bloip.controllers

import com.bloip.websocket.WebSocketHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.util.*
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 10/27/22.
 */
@Controller
class WebSocketBindController(
    @Autowired val webSocketHandler: WebSocketHandler
)
{
    @GetMapping("/ws-info")
    @ResponseBody
    fun get(httpSession: HttpSession): String {
        val userId: Long = httpSession.getAttribute("userId") as Long
        val secretKey = UUID.randomUUID().toString()

        webSocketHandler.mapSecretAndUserId(secretKey = secretKey, userId = userId)

        return secretKey
    }
}