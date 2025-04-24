package think.auto.dev.agent.flow.custom.processEngine

class ExecutionContext(val calculateExecutionOrder: Map<String, Int>) {
    // 存储所有节点的执行结果
    val results = mutableMapOf<String, NodeExecutionResult>()
    // 获取节点的输出结果
    fun getNodeOutput(nodeId: String): String? {
        return results[nodeId]?.output
    }

}