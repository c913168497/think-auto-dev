package think.auto.dev.llm

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import think.auto.dev.llm.role.ChatRole

interface LLMProvider {
    val defaultTimeout: Long get() = 6000

    fun prompt(promptText: String): String

    @OptIn(ExperimentalCoroutinesApi::class)
    fun stream(promptText: String, systemPrompt: String, keepHistory: Boolean = true): Flow<String> {
        return callbackFlow {
            val prompt = prompt(promptText)
            trySend(prompt)

            awaitClose()
        }
    }

    fun clearMessage() {

    }

    fun appendLocalMessage(msg: String, role: ChatRole) {}


}
