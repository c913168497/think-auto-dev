package think.auto.dev.agent


import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.Json
import think.auto.dev.agent.chatcontext.ChatPrompt
import think.auto.dev.agent.chatpanel.sendToChatPanel
import think.auto.dev.settings.aiProvider.AiProviderDBComponent
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle
import think.auto.dev.settings.prompt.database.PromptDataBaseComponent
import think.auto.dev.settings.promptfunction.PromptFunction
import think.auto.dev.settings.promptfunction.PromptFunctionConfig
import java.util.regex.Pattern

/**
 * 自定义右键点击动作出发
 */
class CustomRightClickAction(private val promptFunction: PromptFunction) : IntentionAction {
    private val logger = logger<CustomRightClickAction>()

    override fun getText(): String = promptFunction.title
    override fun startInWriteAction(): Boolean {
        return true
    }

    override fun getFamilyName() = ThinkAutoDevMessagesBundle.message("auto.dev.custom.intentions.family")
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null) return false
        val config = promptFunction.config
        Json.decodeFromString(PromptFunctionConfig.serializer(), config)
        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val selectedText = editor.selectionModel.selectedText
        val config = promptFunction.config
        val promptFunctionConfig = Json.decodeFromString(PromptFunctionConfig.serializer(), config)
        val useAiModelId = promptFunctionConfig.useAiModel
        val aiProvider = AiProviderDBComponent.getAiProviderById(useAiModelId) ?: return

        val content = promptFunction.content
        var result = content
        // 先处理 ${SELECTION} 替换
        if (!selectedText.isNullOrBlank()) {
            result = replaceSelection(content, selectedText)
        } else {
            result = replaceSelection(content, "")
        }

        val allPrompts = PromptDataBaseComponent.getAllPrompts()
        val specReplacements = allPrompts.associate { it.title to it.content }
        // 然后处理 ${SPEC_*} 替换
        result = replaceSpecPlaceholders(result, specReplacements)
        var displayPrompt = ""
        if (promptFunctionConfig.hidePrompt) {
            displayPrompt = "正在处理操作： " + promptFunction.title
        } else {
            displayPrompt = result
        }

        sendToChatPanel(project, aiProvider, promptFunction.title, ChatPrompt(displayPrompt, result))
    }

    /**
     * 替换 ${SELECTION} 占位符
     */
    private fun replaceSelection(input: String, replacement: String): String {
        return input.replace(Pattern.compile("\\$\\{SELECTION\\}").toRegex(), replacement)
    }

    /**
     * 替换 ${SPEC_*} 占位符
     */
    private fun replaceSpecPlaceholders(input: String, replacements: Map<String, String>): String {
        val pattern = Pattern.compile("\\$\\{SPEC_(\\w+)}")
        val matcher = pattern.matcher(input)

        val sb = StringBuffer()
        while (matcher.find()) {
            val key = matcher.group(1)      // 捕获组，如 manager
            // 从映射表中获取替换值，如果没有则保留原样
            val replacementValue = replacements[key]
            if (replacementValue != null) {
                matcher.appendReplacement(sb, replacementValue)
            }
        }

        matcher.appendTail(sb)
        return sb.toString()
    }

    companion object {
        fun create(promptFunction: PromptFunction): CustomRightClickAction {
            return CustomRightClickAction(promptFunction)
        }
    }
}