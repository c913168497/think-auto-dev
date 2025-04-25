package think.auto.dev.agent.chatpanel


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import think.auto.dev.agent.chatcontext.ChatPrompt
import think.auto.dev.settings.aiProvider.AiProvider
import think.auto.dev.settings.chatcontext.ChatContextItem
import java.util.concurrent.CompletableFuture

fun sendToChatWindow(
    project: Project,
    aiProvider: AiProvider,
    chatTitle: String,
    runnable: (ChatCodingPanel, ChatCodingService) -> Unit,
) {
    val chatCodingService = ChatCodingService(project, aiProvider, chatTitle)

    val toolWindowManager = ThinkAutoDevToolWindowFactory.getToolWindow(project) ?: run {
        logger<ChatCodingService>().warn("Tool window not found")
        return
    }

    val contentManager = toolWindowManager.contentManager
    val contentPanel = ChatCodingPanel(chatCodingService, aiProvider, toolWindowManager.disposable)
    val content = contentManager.factory.createContent(contentPanel, chatCodingService.getLabel(), false)

    ApplicationManager.getApplication().invokeLater {
        contentManager.addContent(content)
        toolWindowManager.activate {
            runnable(contentPanel, chatCodingService)
        }
    }
}

fun sendToChatPanel(project: Project, aiProvider: AiProvider, title: String, prompter: ChatPrompt) {
    sendToChatWindow(project, aiProvider, title) { contentPanel, chatCodingService ->
        chatCodingService.handlePromptAndResponse(contentPanel, prompter, newChatContext = true)
    }
}

fun sendToChatPanel(project: Project, aiProvider: AiProvider, title: String, runnable: (ChatCodingPanel, ChatCodingService) -> Unit) {
    sendToChatWindow(project, aiProvider, title, runnable)
}

fun sendToChatPanel(project: Project, aiProvider: AiProvider, title: String, historyChatMessages: List<ChatContextItem>) {
    sendToChatWindow(project, aiProvider, title) { contentPanel, chatCodingService ->
        chatCodingService.loadHistoryMessage(contentPanel, historyChatMessages)
    }
}

fun sendToChatWindowSync(
    project: Project,
    aiProvider: AiProvider,
    chatTitle: String,
    runnable: (ChatCodingPanel, ChatCodingService) -> CompletableFuture<String>,
): CompletableFuture<String> {
    val future = CompletableFuture<String>()
    val chatCodingService = ChatCodingService(project, aiProvider, chatTitle)

    val toolWindowManager = ThinkAutoDevToolWindowFactory.getToolWindow(project) ?: run {
        logger<ChatCodingService>().warn("Tool window not found")
        future.completeExceptionally(IllegalStateException("Tool window not found"))
        return future
    }

    val contentManager = toolWindowManager.contentManager
    val contentPanel = ChatCodingPanel(chatCodingService, aiProvider, toolWindowManager.disposable)
    val content = contentManager.factory.createContent(contentPanel, chatCodingService.getLabel(), false)

    ApplicationManager.getApplication().invokeLater {
        contentManager.addContent(content)
        // 设置新创建的content为当前选中的content
        contentManager.setSelectedContent(content)

        toolWindowManager.activate {
            val resultFuture = runnable(contentPanel, chatCodingService)
            resultFuture.thenAccept { result ->
                future.complete(result)
            }.exceptionally { ex ->
                future.completeExceptionally(ex)
                null
            }
        }
    }

    return future
}

// 使用同步方式发送到聊天面板
fun sendToChatPanelSync(project: Project, aiProvider: AiProvider, title: String, prompter: ChatPrompt): CompletableFuture<String> {
    return sendToChatWindowSync(project, aiProvider, title) { contentPanel, chatCodingService ->
        chatCodingService.handleSyncPromptAndResponse(contentPanel, prompter, newChatContext = true)
    }
}

