package think.auto.dev.agent.flow.custom.processEngine.process


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking
import think.auto.dev.agent.chatcontext.ChatPrompt
import think.auto.dev.agent.chatpanel.sendToChatPanelSync
import think.auto.dev.agent.flow.custom.processEngine.*
import think.auto.dev.settings.aiProvider.AiProviderDBComponent

// AI模型处理器
class AIModelProcessor(var project: Project, editor: Editor?, file: PsiFile?) : NodeProcessor {
    override fun canProcess(nodeType: String): Boolean {
        return nodeType == "AI_MODEL_PROCESS"
    }

    override fun process(node: Node, inputs: Map<String, String?>, context: ExecutionContext): NodeExecutionResult {
        // 获取提示词
        val prompt = inputs.values
        val contentBuilder: StringBuilder = StringBuilder()
        prompt.forEach {
            contentBuilder.append(it).append("\n")
        }
        // 获取模型配置
        val modelName = node.config // 例如 "deep-seek"

        // TODO: 实现AI模型调用逻辑
        val response = callAIModel(contentBuilder.toString(), modelName, node)

        return NodeExecutionResult(
            nodeId = node.id,
            nodeName = node.name,
            output = response,
            status = ExecutionStatus.SUCCESS
        )
    }

    private fun callAIModel(prompt: String, modelName: String, node: Node): String {
        val aiProvider = AiProviderDBComponent.getAiProviderByModelName(modelName) ?: return ""
        var result = sendToChatPanelSync(project, aiProvider, node.name, ChatPrompt(prompt, prompt)).get()
        // 实现AI模型调用逻辑
        return result
    }
}