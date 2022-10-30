package com.bloip.configuration

import com.bloip.websocket.WebSocketHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * Created by Usman Mutawakil on 10/27/22.
 */
@Configuration
@EnableWebSocket
class BloipWebSocketConfiguration(
        @Autowired val webSocketHandler: WebSocketHandler
    ) : WebSocketConfigurer
{
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(webSocketHandler, "/web-socket");
    }
}