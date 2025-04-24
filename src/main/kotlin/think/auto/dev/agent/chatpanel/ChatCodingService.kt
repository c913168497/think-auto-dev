package think.auto.dev.agent.chatpanel

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import think.auto.dev.agent.chatcontext.ChatMessage
import think.auto.dev.agent.chatcontext.ChatPrompt
import think.auto.dev.llm.LLMCoroutineScope
import think.auto.dev.llm.LlmFactory
import think.auto.dev.settings.aiProvider.AiProvider
import think.auto.dev.settings.chatcontext.ChatContextItem
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle
import java.util.concurrent.CompletableFuture

class ChatCodingService(val project: Project, var aiProvider: AiProvider, var title: String) {
    private val llmProvider = LlmFactory().create(project, aiProvider)

    var currentJob: Job? = null

    fun getLabel(): String = title

    fun stop() {
        currentJob?.cancel()
    }

    fun handlePromptAndResponse(
        ui: ChatCodingPanel,
        prompter: ChatPrompt,
        newChatContext: Boolean
    ) {
        var requestPrompt = prompter.requestPrompt
        var displayPrompt = prompter.displayPrompt
        ui.content
        ui.addMessage(requestPrompt, true, displayPrompt)
        ui.addMessage(ThinkAutoDevMessagesBundle.message("think.auto.dev.loading"), isSystem = true)
        ApplicationManager.getApplication().executeOnPooledThread {
            val response = this.makeChatBotRequest(requestPrompt, newChatContext, prompter.contextItems)
            currentJob = LLMCoroutineScope.scope(project).launch {
                ui.updateMessage(response)
            }
        }
    }
    // 修改handleSyncPromptAndResponse方法
    fun handleSyncPromptAndResponse(
        ui: ChatCodingPanel,
        prompter: ChatPrompt,
        newChatContext: Boolean
    ): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        var requestPrompt = prompter.requestPrompt
        var displayPrompt = prompter.displayPrompt
        ui.content
        ui.addMessage(requestPrompt, true, displayPrompt)
        ui.addMessage(ThinkAutoDevMessagesBundle.message("think.auto.dev.loading"), isSystem = true)

        ApplicationManager.getApplication().executeOnPooledThread {
            val response = this.makeChatBotRequest(requestPrompt, newChatContext, prompter.contextItems)
            currentJob = LLMCoroutineScope.scope(project).launch {
                try {
                    val result = ui.updateMessage(response)
                    future.complete(result)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }


        }

        return future
    }
    fun loadHistoryMessage(
        ui: ChatCodingPanel,
        historyChatMessages: List<ChatContextItem>
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            ui.loadHistoryMessage(historyChatMessages)
        }
    }

    private fun makeChatBotRequest(
        requestPrompt: String,
        newChatContext: Boolean,
        contextItems: List<ChatMessage>
    ): Flow<String> {
        contextItems.forEach { llmProvider.appendLocalMessage(it.context, it.role) }
        return llmProvider.stream(requestPrompt, "", keepHistory = !newChatContext)
    }

    fun clearSession() {
        llmProvider.clearMessage()
    }
}
