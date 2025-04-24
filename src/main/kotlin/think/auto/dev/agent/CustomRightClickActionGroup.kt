package think.auto.dev.agent

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionActionBean
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle
import think.auto.dev.settings.promptfunction.PromptFunctionDataBaseComponent
import java.util.function.Supplier

class CustomRightClickActionGroup :
    ActionGroup(Supplier { ThinkAutoDevMessagesBundle.message("intentions.assistant.name") }, true), DumbAware {

    private val EP_NAME: ExtensionPointName<IntentionActionBean> = ExtensionPointName("cc.auto.autoDevIntention")
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
        val extensionList = EP_NAME.extensionList

        val builtinIntentions = extensionList
            .asSequence()
            .map { it.instance.asIntention() }
            .filter { it.isAvailable(project, editor, file) }
            .toList()

        val promptFunctions = PromptFunctionDataBaseComponent.getAllPrompts()
        val customActionIntentions: List<IntentionAction> = promptFunctions.map {
            CustomRightClickAction.create(it)
        }

        val actionList = builtinIntentions + customActionIntentions
        return actionList.map { it }
    }
}
