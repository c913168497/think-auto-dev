package think.auto.dev.agent.chatpanel

import think.auto.dev.agent.chatpanel.message.CompletableMessage
import think.auto.dev.agent.chatpanel.message.MessageBlockTextListener
import think.auto.dev.llm.role.ChatRole


class ChatSimpleMessage(
    override val displayText: String,
    override val text: String,
    val chatRole: ChatRole, override var rating: CompletableMessage.ChatMessageRating = CompletableMessage.ChatMessageRating.None,
) : CompletableMessage {
    private val textListeners: MutableList<MessageBlockTextListener> = mutableListOf()
    override fun getRole(): ChatRole = chatRole

    override fun addTextListener(textListener: MessageBlockTextListener) {
        textListeners += textListener
    }

    override fun removeTextListener(textListener: MessageBlockTextListener) {
        textListeners -= textListener
    }
}