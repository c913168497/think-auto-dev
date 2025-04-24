package think.auto.dev.agent.chatpanel

import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import think.auto.dev.settings.aiProvider.AiProviderDBComponent
import think.auto.dev.settings.chatcontext.ChatContextDataBaseComponent
import think.auto.dev.settings.language.LanguageChangedCallback

class ThinkAutoDevToolWindowFactory : ToolWindowFactory, DumbAware {
    object Util {
        const val id = "ThinkAutoDev"
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val allAiProviders = AiProviderDBComponent.getAllAiProviders()
        if (allAiProviders.isEmpty()) {
            return
        }

        val aiProvider = allAiProviders[0]
        // 取第一个
        val chatCodingService = ChatCodingService(project, aiProvider, "New Chat")
        val contentPanel = ChatCodingPanel(chatCodingService, aiProvider, toolWindow.disposable)
        val content = ContentFactory.getInstance().createContent(contentPanel, "", false).apply {
            setInitialDisplayName(this)
        }

        // 监听 Content 移除事件（例如 Tab 关闭）
        toolWindow.addContentManagerListener(object : ContentManagerListener {
            override fun contentRemoved(event: ContentManagerEvent) {
                val chatCodingPanel = event.content.component as ChatCodingPanel
                val chatHistoryContext = chatCodingPanel.getChatHistoryContext()
                if (chatHistoryContext.isEmpty()) {
                    return
                }

                val chatContextItem = chatHistoryContext[0]
                val titleId = chatContextItem.titleId
                val chatContextTitle = ChatContextDataBaseComponent.getChatContextTitleById(titleId)
                val allChatContextItems = ChatContextDataBaseComponent.getAllChatContextItems(titleId)
                if (chatContextTitle != null) {
                    ChatContextDataBaseComponent.deleteChatContextTitles(titleId)
                }

                if (allChatContextItems.isNotEmpty()) {
                    ChatContextDataBaseComponent.deleteChatContextItems(titleId)
                }
            }
        })


        contentPanel.resetChatSession()
        ApplicationManager.getApplication().invokeLater {
            toolWindow.contentManager.addContent(content)
        }
    }

    /**
     * 窗口 tool bar 功能， New Chat
     */
    override fun init(toolWindow: ToolWindow) {
        toolWindow.setTitleActions(listOfNotNull(ActionUtil.getActionGroup("ThinkAutoDev.ToolWindow.Chat.TitleActions")))
        val connection = toolWindow.project.messageBus.connect()
        connection.subscribe(ToolWindowManagerListener.TOPIC, ThinkAutoDevToolWindowListener())
    }

    companion object {
        fun getToolWindow(project: Project): ToolWindow? {
            return ToolWindowManager.getInstance(project).getToolWindow(Util.id)
        }

        fun setInitialDisplayName(content: Content) {
            LanguageChangedCallback.componentStateChanged("think.auto.dev.chat", content, 2) { c, d ->
                c.displayName = d
            }
        }

    }
}
