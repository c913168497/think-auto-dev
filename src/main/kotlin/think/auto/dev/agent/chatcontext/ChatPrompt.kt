package think.auto.dev.agent.chatcontext

/**
 * 对话提示词
 */
data class ChatPrompt(
    // 展示的提示词
    val displayPrompt: String,
    // 真实请求的提示词
    val requestPrompt: String,
    // 上下文
    val contextItems: List<ChatMessage> = listOf()
)