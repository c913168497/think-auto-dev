package think.auto.dev.agent.flow.autocode.handler

import com.intellij.openapi.progress.ProgressIndicator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.runBlocking
import think.auto.dev.agent.flow.autocode.PseudocodeToTrueCodeContext
import think.auto.dev.agent.flow.core.BaseWorkflowStep
import think.auto.dev.llm.LlmFactory
import think.auto.dev.llm.role.ChatRole
import think.auto.dev.utils.VmFileReadUtils
import think.auto.dev.utils.findPsiClassesByClassNameSafely
import think.auto.dev.utils.getClassInfoAndFunInfo
import java.util.regex.Pattern

class GetNeedClassAndFunFlow(val indicator: ProgressIndicator) : BaseWorkflowStep<PseudocodeToTrueCodeContext>(indicator) {


    companion object {
        const val FUNCTION_NAME = "解析伪代码中存在的类信息"
    }

    override fun process(context: PseudocodeToTrueCodeContext): PseudocodeToTrueCodeContext {
        val prompt = context.prompt
        val project = context.project
        val aiProvider = getAiProvider(FUNCTION_NAME) ?: return context

        val llmProvider = LlmFactory().create(project, aiProvider)
        val templateRender = VmFileReadUtils("pseudocodeFlow")
        val msg_1 = templateRender.readTemplate("1_prompt_system.vm")
        val msg_2 = templateRender.readTemplate("2_prompt_user.vm") + "\n" + prompt
        llmProvider.appendLocalMessage(msg_1, ChatRole.System)
        llmProvider.appendLocalMessage(msg_2, ChatRole.User)
        val resultMessage = StringBuilder()
        val promptResult = llmProvider.stream(prompt, prompt)
        runBlocking {
            resultMessage.append(getMessageResult(promptResult))
        }
        // 获取关联类方法, 关联的类信息
        val classAndFunInfo = getClassAndFunInfo(resultMessage.toString(), context)
        context.classAndFunInfo = classAndFunInfo
        return context
    }

    fun getClassAndFunInfo(resultAi: String, context: PseudocodeToTrueCodeContext): String {
        var resultInfo = resultAi.trimIndent()
        // 正则表达式模式
        val pattern = Pattern.compile("\\[(.*?)]")
        // 匹配结果列表
        val result = mutableListOf<String>()
        // 使用正则表达式匹配输入文本
        val matcher = pattern.matcher(resultInfo)
        while (matcher.find()) {
            val classPath = matcher.group(1) // 类路径
            result.add(classPath)
        }
        val resultMessage = StringBuilder()
        // 打印结果
        result.forEach {
            val findPsiClasses = findPsiClassesByClassNameSafely(context.project, it)
            findPsiClasses.forEach { classPsi ->
                resultMessage.append(getClassInfoAndFunInfo(classPsi))
            }
        }

        return resultMessage.toString()
    }

    private suspend fun getMessageResult(content: Flow<String>): String {
        var text = ""
        val batchSize = 5
        val buffer = mutableListOf<String>()
        content
            .buffer(capacity = 10)
            .collect { newText ->
                buffer.add(newText)
                if (buffer.size >= batchSize) {
                    text += buffer.joinToString("")
                    buffer.clear()
                }
            }
        // 处理剩余的缓冲内容
        if (buffer.isNotEmpty()) {
            text += buffer.joinToString("")
        }
        return text
    }
}
