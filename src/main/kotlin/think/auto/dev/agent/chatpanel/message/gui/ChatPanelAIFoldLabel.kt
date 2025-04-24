package think.auto.dev.agent.chatpanel.message.gui

import com.intellij.ui.components.JBLabel
import think.auto.dev.llm.role.ChatRole


/**
 * 聊天 ai 折叠标签
 */
class ChatPanelAIFoldLabel(private val side: ChatRole) : JBLabel("", 0) {
    init {
        setOpaque(true)
        isVisible = false
    }
}