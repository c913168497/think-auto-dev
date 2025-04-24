package think.auto.dev.agent.flow.autocode.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import kotlinx.coroutines.runBlocking
import think.auto.dev.agent.chatpanel.sendToChatPanel
import think.auto.dev.agent.flow.automappercode.AutoMapperCodeBackgroundTask
import think.auto.dev.llm.LlmFactory
import think.auto.dev.settings.aiProvider.AiProviderDBComponent
import think.auto.dev.utils.VmFileReadUtils
import think.auto.dev.utils.findPsiClassesByClassNameSafely
import think.auto.dev.utils.getClassInfoAndFunInfo

class GenerateMapperFunctionAction : AnAction() {

    init {
        templatePresentation.text = "MybatisSql代码生成Mapper代码"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (psiFile == null || psiFile.virtualFile == null) {
            return
        }

        val selectedText = editor?.selectionModel?.selectedText ?: return
        val regex = Regex("result(?:Type|Map)=\"([^\"]+)\"")
        val matchResult1 = regex.find(selectedText)
        if (matchResult1 != null) {
            val resultSplit = matchResult1.groupValues[1].split(".")
            val className = resultSplit[resultSplit.size - 1]
            val findPsiClasses = findPsiClassesByClassNameSafely(project, className)
            runBlocking {
                // 确保在 EDT 上执行
                ApplicationManager.getApplication().invokeLater {
                    // 使用 WriteCommandAction 确保在写入状态下
                    WriteCommandAction.runWriteCommandAction(project) {
                        var resultMsgStr = ""
                        findPsiClasses.forEach { classPsi ->
                            val resultMsg = getClassInfoAndFunInfo(classPsi)
                            resultMsgStr += resultMsg
                        }

                        val templateRender = VmFileReadUtils("autocode")
                        val prompter = templateRender.readTemplate("prompt_generate_mapper.vm")
                        val allAiProviders = AiProviderDBComponent.getAllAiProviders()
                        if (allAiProviders.isEmpty()) {
                            return@runWriteCommandAction
                        }

                        val aiProvider = allAiProviders[0]
                        val llmProvider = LlmFactory().create(project, aiProvider)
                        runBlocking {
                            // 确保在 EDT 上执行
                            ApplicationManager.getApplication().invokeLater {
                                sendToChatPanel(project, aiProvider, templatePresentation.text) { contentPanel, _ ->
                                    val task =
                                        AutoMapperCodeBackgroundTask(project, prompter, contentPanel, llmProvider)
                                    ProgressManager.getInstance().runProcessWithProgressAsynchronously(
                                        task, BackgroundableProcessIndicator(task)
                                    )
                                }
                            }
                        }
                    }
                }
            }

        } else {
            println("No match found in text1")
        }

    }

}
