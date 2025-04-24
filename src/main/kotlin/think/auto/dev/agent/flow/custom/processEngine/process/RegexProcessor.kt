package think.auto.dev.agent.flow.custom.processEngine.process

import think.auto.dev.agent.flow.custom.processEngine.*
import java.util.regex.Pattern


// 正则过滤器
class RegexProcessor : NodeProcessor {
    override fun canProcess(nodeType: String): Boolean {
        return nodeType == "REGEX_PROCESS"
    }

    override fun process(node: Node, inputs: Map<String, String?>, context: ExecutionContext): NodeExecutionResult {
        // 获取输入文本
        val inputText = inputs.values.toString() ?: ""
        val processedText = processRegex(inputText, node.config)
        return NodeExecutionResult(
            nodeId = node.id,
            nodeName = node.name,
            output = processedText,
            status = ExecutionStatus.SUCCESS
        )
    }

    private fun processRegex(text: String, regexConfig: String): String {
        // 正则表达式模式
        val pattern = Pattern.compile(regexConfig)
        // 匹配结果列表
        val result = mutableListOf<String>()
        // 使用正则表达式匹配输入文本
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val classPath = matcher.group(1) // 类路径
            result.add(classPath)
        }
        return result.joinToString(",")
    }
}