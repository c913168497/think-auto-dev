package think.auto.dev.agent.chatcontext

import think.auto.dev.llm.role.ChatRole

/**
 * 聊天上下文项
 */
class ChatMessage(
    val role: ChatRole,
    var context: String
)