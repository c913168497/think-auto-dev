package think.auto.dev.settings.promptfunction

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 提示词功能公共配置
 */
@Serializable
data class PromptFunctionConfig(
    /**
     * 对选中的内容进行正则过滤
     */
    @SerialName("selectedMatchRegex")
    val selectedMatchRegex: String,

    /**
     * 是否隐藏提示词
     */
    @SerialName("hidePrompt")
    val hidePrompt: Boolean,

    /**
     * 使用的ai模型
     */
    @SerialName("useAiModel")
    val useAiModel: Int,

    /**
     * 载入聊天历史上下文
     */
    @SerialName("loadChatContext")
    val loadChatContext: Int? = null  // 添加默认值
)