package it.xyra.mamout.sync

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class SyncServer(
    private val onSessionStarted: suspend DefaultWebSocketServerSession.() -> Unit = {},
    private val onMessageReceived: suspend (String) -> Unit = {}
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState = _serverState.asStateFlow()

    private val _sessionCount = MutableStateFlow(0)
    val sessionCount = _sessionCount.asStateFlow()

    private val sessions = Collections.newSetFromMap(ConcurrentHashMap<DefaultWebSocketServerSession, Boolean>())

    sealed class ServerState {
        object Stopped : ServerState()
        data class Running(val ip: String, val port: Int) : ServerState()
        data class Error(val message: String) : ServerState()
    }

    fun start() {
        if (server != null) return

        val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
        
        scope.launch {
            try {
                val newServer = embeddedServer(Netty, port = 0, host = "0.0.0.0") {
                    install(WebSockets) {
                        pingPeriod = 30.seconds
                        timeout = 600.seconds // 10 minutes idle timeout
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }
                    install(ContentNegotiation) {
                        json(Json {
                            prettyPrint = true
                            isLenient = true
                        })
                    }
                    routing {
                        webSocket("/") {
                            sessions.add(this)
                            _sessionCount.value = sessions.size
                            try {
                                onSessionStarted(this)
                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        onMessageReceived(frame.readText())
                                    }
                                }
                            } catch (e: ClosedReceiveChannelException) {
                                // Connection closed
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                sessions.remove(this)
                                _sessionCount.value = sessions.size
                            }
                        }
                    }
                }
                
                server = newServer
                newServer.start(wait = false)
                
                // Get the actual port after starting
                val actualPort = newServer.engine.resolvedConnectors().firstOrNull()?.port ?: 0
                _serverState.value = ServerState.Running(ip, actualPort)
                
            } catch (e: Exception) {
                _serverState.value = ServerState.Error(e.message ?: "Unknown error")
                server = null
            }
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        _sessionCount.value = 0
        _serverState.value = ServerState.Stopped
    }

    fun broadcastMessage(message: String) {
        scope.launch {
            sessions.forEach { session ->
                try {
                    session.send(Frame.Text(message))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
