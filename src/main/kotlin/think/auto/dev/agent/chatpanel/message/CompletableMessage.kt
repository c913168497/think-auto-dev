package think.auto.dev.agent.chatpanel.message

import com.intellij.openapi.actionSystem.DataKey
import think.auto.dev.llm.role.ChatRole

/**
 * 完整消息体
 */
interface CompletableMessage {
    val text: String
    val displayText: String
    var rating: ChatMessageRating
    fun getRole(): ChatRole

    fun addTextListener(textListener: MessageBlockTextListener)
    fun removeTextListener(textListener: MessageBlockTextListener)

    companion object {
        val key: DataKey<CompletableMessage> = DataKey.create("CompletableMessage")

    }

    enum class ChatMessageRating {
        None,
        Like,
        Dislike,
        Copy
    }
}

