package think.auto.dev.agent.flow.custom.action

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.Json
import think.auto.dev.agent.flow.custom.gui.FlowDefinition
import think.auto.dev.agent.flow.custom.processEngine.FlowEngine
import think.auto.dev.agent.flow.custom.processEngine.process.*
import think.auto.dev.settings.flowEngine.database.FlowEngineConfig
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle

class FlowEngineAction(private val flowEngineConfig: FlowEngineConfig) : IntentionAction {


    override fun getText(): String = flowEngineConfig.title
    override fun startInWriteAction(): Boolean {
        return true
    }

    override fun getFamilyName() = ThinkAutoDevMessagesBundle.message("intentions.agent.assistant.name.family")
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false
        val config = flowEngineConfig.content
        Json.decodeFromString(FlowDefinition.serializer(), config)
        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val selectedText = editor.selectionModel.selectedText
        // 初始化流程引擎
        val flowEngine = FlowEngine(project)
        // 注册各类节点处理器
        flowEngine.registerNodeProcessor(MouseSelectionProcessor(project, editor, file))
        flowEngine.registerNodeProcessor(RegexProcessor())
        flowEngine.registerNodeProcessor(GetClassContentProcessor(project, editor, file))
        flowEngine.registerNodeProcessor(PromptEditProcessor())
        flowEngine.registerNodeProcessor(AIModelProcessor(project, editor, file))
        // 读取流程JSON
        val flowJson = flowEngineConfig.content
        try {
            // 解析流程
            val flowProcess = flowEngine.parseFlowFromJson(flowJson)
            println("流程名称: ${flowProcess.name}")
            println("节点数量: ${flowProcess.nodes.size}")
            ApplicationManager.getApplication().executeOnPooledThread {
                // 执行流程
                val results = flowEngine.executeFlow(flowProcess)

                // 输出结果
                println("\n执行结果摘要:")
                results.forEach { result ->
                    println("节点: ${result.nodeName}")
                    println("状态: ${result.status}")
                    println("输出: ${result.output}")
                    if (result.message.isNotEmpty()) {
                        println("消息: ${result.message}")
                    }
                    println("---")
                }
            }

        } catch (e: Exception) {
            println("流程执行错误: ${e.message}")
            e.printStackTrace()
        }
    }

    companion object {
        fun create(flowEngineConfig: FlowEngineConfig): FlowEngineAction {
            return FlowEngineAction(flowEngineConfig)
        }
    }

}