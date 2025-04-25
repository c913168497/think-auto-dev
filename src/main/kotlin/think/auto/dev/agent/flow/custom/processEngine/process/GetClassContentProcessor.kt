package think.auto.dev.agent.flow.custom.processEngine.process

import com.intellij.openapi.project.Project
import think.auto.dev.agent.flow.custom.processEngine.*
import think.auto.dev.utils.findPsiClassesByClassNameSafely
import think.auto.dev.utils.getClassInfoAndFunInfo
import java.util.regex.Pattern

// 获取类内容处理器
class GetClassContentProcessor(var project: Project) : NodeProcessor {
    override fun canProcess(nodeType: String): Boolean {
        return nodeType == "GET_CLASS_CONTENT"
    }

    override fun process(node: Node, inputs: Map<String, String?>, context: ExecutionContext): NodeExecutionResult {
        // 获取输入文本（可能是类名或文件路径等）
        val inputValues = inputs.values
        val contentBuilder: StringBuilder = StringBuilder()
        inputValues.forEach{
            contentBuilder.append(getClassAndFunInfo(it, project))
        }

        return NodeExecutionResult(
            nodeId = node.id,
            nodeName = node.name,
            output = contentBuilder.toString(),
            status = ExecutionStatus.SUCCESS
        )
    }
    private fun getClassAndFunInfo(resultAi: String?, project: Project): String {
        if (resultAi.isNullOrBlank()) {
            return ""
        }

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
            val findPsiClasses = findPsiClassesByClassNameSafely(project, it)
            findPsiClasses.forEach { classPsi ->
                resultMessage.append(getClassInfoAndFunInfo(classPsi))
            }
        }

        return resultMessage.toString()
    }
}