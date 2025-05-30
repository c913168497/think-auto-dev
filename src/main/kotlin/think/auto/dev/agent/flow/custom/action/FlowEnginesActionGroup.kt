package think.auto.dev.agent.flow.custom.action

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import think.auto.dev.settings.flowEngine.database.FlowEngineConfigDataBaseComponent
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle
import java.util.function.Supplier

class FlowEnginesActionGroup :
    ActionGroup(Supplier { ThinkAutoDevMessagesBundle.message("intentions.agent.assistant.name") }, true), DumbAware {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val project: Project = e?.project ?: return emptyArray()
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return emptyArray()
        val file: PsiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return emptyArray()

        val intentions: List<IntentionAction> = getAiAssistantIntentions(project, editor, file)

        return intentions.map { action ->
            DumbAwareAction.create(action.text) {
                action.invoke(project, editor, file)
            }
        }.toTypedArray()
    }

    private fun getAiAssistantIntentions(project: Project, editor: Editor?, file: PsiFile): List<IntentionAction> {
        val allFlowEngineConfig = FlowEngineConfigDataBaseComponent.getAllFlowEngineConfig()

        val customActionIntentions: List<IntentionAction> =
            allFlowEngineConfig.filter { // 如果包含文件夹获取功能，则需要展示
                !it.content.contains("FOLDER_CONTENT_GET")
            }.map {
                FlowEngineAction.create(it)
            }

        val actionList = customActionIntentions
        return actionList.map { it }
    }

}