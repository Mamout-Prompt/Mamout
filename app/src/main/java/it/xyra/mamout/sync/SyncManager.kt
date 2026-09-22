package it.xyra.mamout.sync

import it.xyra.mamout.domain.model.PromptSearchable
import it.xyra.mamout.domain.repository.PromptRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SyncManager {
    private var repository: PromptRepository? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    private val server = SyncServer(
        onSessionStarted = {
            // When a new session starts, send all prompts
            syncAll()
        },
        onMessageReceived = { message ->
            handleIncomingMessage(message)
        }
    )

    val serverState = server.serverState
    val sessionCount = server.sessionCount

    fun initialize(repository: PromptRepository) {
        this.repository = repository
    }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }

    /**
     * Broadcasts all prompts to all connected clients
     */
    fun syncAll() {
        scope.launch {
            repository?.let { repo ->
                val allPrompts = repo.getAllPromptsSync()
                val payload = SyncPayload(allPrompts = allPrompts)
                server.broadcastMessage(json.encodeToString(payload))
            }
        }
    }

    private fun handleIncomingMessage(message: String) {
        scope.launch {
            try {
                val payload = json.decodeFromString<SyncPayload>(message)
                payload.allPrompts?.let { incoming ->
                    repository?.syncPrompts(incoming)
                }
                // Handle single prompt update from extension if needed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Serializable
data class SyncPayload(
    val allPrompts: List<PromptSearchable>? = null,
    val singlePrompt: String? = null
)
