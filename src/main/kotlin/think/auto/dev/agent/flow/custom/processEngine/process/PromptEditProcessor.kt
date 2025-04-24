package think.auto.dev.agent.flow.custom.processEngine.process

import think.auto.dev.agent.flow.custom.processEngine.*

// 提示词编排处理器
class PromptEditProcessor : NodeProcessor {
    override fun canProcess(nodeType: String): Boolean {
        return nodeType == "PROMPT_EDIT"
    }

    override fun process(node: Node, inputs: Map<String, String?>, context: ExecutionContext): NodeExecutionResult {
        // 处理后的配置中已经替换了输入占位符
        val promptTemplate = node.config

        // 创建一个新的映射，将节点ID转换为节点名称
        val nameBasedInputs = mutableMapOf<String, String?>()
        inputs.forEach { (nodeId, value) ->
            // 从上下文中获取节点名称
            val exeOrder = context.calculateExecutionOrder[nodeId]
            val nodeName = context.results[nodeId]?.nodeName
            if (nodeName != null) {
                nameBasedInputs["$nodeName-$exeOrder"] = value
            }
        }
        // 计算执行顺序
        val replacedText = replacePlaceholders(promptTemplate, nameBasedInputs)
        return NodeExecutionResult(
            nodeId = node.id,
            nodeName = node.name,
            output = replacedText,
            status = ExecutionStatus.SUCCESS
        )
    }

    private fun replacePlaceholders(text: String, replacements: Map<String, String?>): String {
        var result = text

        // 处理 ${input_节点名称} 格式的占位符
        val pattern = """\$\{input_([^}]+)}""".toRegex()
        result = pattern.replace(result) { matchResult ->
            val key = matchResult.groupValues[1]  // 获取括号内的键（节点名称）
            replacements[key] ?: matchResult.value  // 如果找不到替换值，保留原文本
        }

        return result
    }
}