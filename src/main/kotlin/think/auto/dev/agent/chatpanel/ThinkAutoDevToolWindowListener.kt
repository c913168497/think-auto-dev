package think.auto.dev.agent.chatpanel

import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import think.auto.dev.settings.aiProvider.AiProviderDBComponent
import think.auto.dev.settings.chatcontext.ChatContextDataBaseComponent

class ThinkAutoDevToolWindowListener : ToolWindowManagerListener {
    override fun toolWindowShown(toolWindow: ToolWindow) {
        if (toolWindow.id == ThinkAutoDevToolWindowFactory.Util.id) {
            // 当你的 ToolWindow 显示时执行
            println("ThinkAutoDev ToolWindow shown!")
            val allChatContextTitles = ChatContextDataBaseComponent.getAllChatContextTitles()
            val project = toolWindow.project
            val contents = toolWindow.contentManager.contents
            val historyChatIds: MutableList<Long> = mutableListOf()
            contents.forEach {
                val component = it.component as ChatCodingPanel
                if (component.getChatHistoryContext().isNotEmpty()) {
                    val contextItem = component.getChatHistoryContext()[0]
                    historyChatIds.add(contextItem.titleId)
                }
            }

            val allAiProviders = AiProviderDBComponent.getAllAiProviders()
            if (allAiProviders.isEmpty()) {
                return
            }

            val idOfAiProvider = allAiProviders.associateBy { it.id }
            allChatContextTitles.forEach {
                if (!historyChatIds.contains(it.id)) {
                    var aiProviderCurrent = idOfAiProvider[it.aiProviderId]
                    if (aiProviderCurrent == null) {
                        aiProviderCurrent = allAiProviders[0]
                    }

                    val allChatContextItems = ChatContextDataBaseComponent.getAllChatContextItems(it.id)
                    sendToChatPanel(project, aiProviderCurrent, it.title, allChatContextItems)
                }
            }

        }
    }

}