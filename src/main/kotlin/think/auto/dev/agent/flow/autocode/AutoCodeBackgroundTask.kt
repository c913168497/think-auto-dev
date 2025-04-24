package think.auto.dev.agent.flow.autocode

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import think.auto.dev.agent.chatpanel.ChatCodingPanel
import think.auto.dev.llm.LLMProvider

class AutoCodeBackgroundTask(
    project: Project,
    private val prompt: String,
    private val contentPanel: ChatCodingPanel,
    val llmProvider: LLMProvider,
) : Task.Backgroundable(project, "伪代码生成真实代码", true) {
    override fun run(indicator: ProgressIndicator) {
        runBlocking {
            val promptResult = llmProvider.stream(prompt, "")
            val updateMessage = contentPanel.updateMessage(promptResult)
            return@runBlocking updateMessage
        }
    }
}
