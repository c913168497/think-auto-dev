package think.auto.dev.settings.promptfunction

import kotlinx.serialization.Serializable

@Serializable
data class PromptFunction(
    // 数据库自增ID，新增时可以为空
    val id: Int? = null,
    // 标题
    val title: String,
    // 内容
    val content: String,
    // 关联配置
    val config: String

) {
    override fun toString(): String {
        return title
    }
}
