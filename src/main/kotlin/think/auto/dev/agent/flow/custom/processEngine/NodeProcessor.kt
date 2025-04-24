package think.auto.dev.agent.flow.custom.processEngine

interface NodeProcessor {
    // 判断该处理器是否可以处理指定类型的节点
    fun canProcess(nodeType: String): Boolean
    
    // 处理节点并返回结果
    fun process(node: Node, inputs: Map<String, String?>, context: ExecutionContext): NodeExecutionResult
}