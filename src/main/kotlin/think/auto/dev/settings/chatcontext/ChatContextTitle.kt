package think.auto.dev.settings.chatcontext


/**
 * 聊天上下文标题
 */
data class ChatContextTitle(
    val id: Long, // 数据库自增ID，新增时可以为空
    val title: String,
    val aiProviderId: Int,
) {
    override fun toString(): String {
        return title
    }
}
