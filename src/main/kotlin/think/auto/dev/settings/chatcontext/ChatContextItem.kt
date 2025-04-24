package think.auto.dev.settings.chatcontext

/**
 * 聊天上下文项
 */
data class ChatContextItem(
    val id: Long? = null, // 数据库自增ID，新增时可以为空
    val titleId: Long,
    val role: String,
    val content: String,
    val createTime: Long,
) {
    override fun toString(): String {
        return content
    }
}
