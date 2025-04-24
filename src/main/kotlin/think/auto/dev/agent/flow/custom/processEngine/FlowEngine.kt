package think.auto.dev.agent.flow.custom.processEngine


import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import think.auto.dev.agent.flow.custom.processEngine.gui.FlowExecutionToolWindowFactory
import think.auto.dev.common.ui.statusbar.ThinkAutoDevStatus
import think.auto.dev.common.ui.statusbar.ThinkAutoDevStatusService
import java.util.logging.Logger

class FlowEngine(private val project: Project) {
    private val nodeProcessors = mutableListOf<NodeProcessor>()
    private val logger = Logger.getLogger(this.javaClass.name)

    // 注册节点处理器
    fun registerNodeProcessor(processor: NodeProcessor) {
        nodeProcessors.add(processor)
    }

    // 执行流程
    // 执行流程
    fun executeFlow(flowProcess: FlowProcess): List<NodeExecutionResult> {
        // 打开执行面板并确保它已初始化
        val executionPanel =  FlowExecutionToolWindowFactory.showToolWindow(project)
        executionPanel.clear()

        val calculateExecutionOrder = calculateExecutionOrder(flowProcess.nodes, flowProcess.edges)
        val context = ExecutionContext(calculateExecutionOrder)
        val executionOrder = calculateExecutionOrder(flowProcess)

        val results = mutableListOf<NodeExecutionResult>()
        // 预先初始化所有节点到UI，按照执行顺序排列
        executionPanel.initializeNodes(flowProcess.nodes, executionOrder)

        // 按顺序执行节点
        for (nodeId in executionOrder) {
            val node = flowProcess.nodes.find { it.id == nodeId }
                ?: throw IllegalStateException("找不到节点: $nodeId")

            logger.info("开始执行节点: ${node.name} (${node.type})")

            // 更新UI状态为运行中
            executionPanel.updateNodeStatus(node.id, ExecutionStatus.RUNNING)

            // 查找适合的处理器
            val processor = findProcessor(node.type)
                ?: throw IllegalStateException("找不到节点处理器: ${node.type}")

            // 获取输入节点
            val inputNodeIds = flowProcess.edges
                .filter { it.targetNodeId == node.id }
                .map { it.sourceNodeId }

            // 构建输入映射
            val inputs = inputNodeIds.associate { inputId ->
                val inputNode = flowProcess.nodes.find { it.id == inputId }!!
                inputNode.id to context.getNodeOutput(inputId)
            }

            // 处理配置中的占位符
            try {
                // 执行节点
                val result = processor.process(node, inputs, context)
                context.results[node.id] = result
                results.add(result)

                // 更新UI
                executionPanel.updateNodeResult(result)

                logger.info("节点 ${node.name} 执行完成，状态: ${result.status}")
                logger.info("输出: ${result.output}")
            } catch (e: Exception) {
                logger.severe("节点 ${node.name} 执行失败: ${e.message}")
                ThinkAutoDevStatusService.notifyApplication(ThinkAutoDevStatus.Error, "流程执行失败，请检查 ${e.message}")

                // 更新UI状态为失败
                val failResult = NodeExecutionResult(
                    nodeId = node.id,
                    nodeName = node.name,
                    output = "执行失败: ${e.message}\n${e.stackTraceToString()}",
                    status = ExecutionStatus.FAILURE,
                    message = e.message ?: "未知错误"
                )
                executionPanel.updateNodeResult(failResult)

                break
            }
        }

        return results
    }

    // 从JSON解析流程
    fun parseFlowFromJson(json: String): FlowProcess {
        return try {
            return Json.decodeFromString(FlowProcess.serializer(), json)
        } catch (e: Exception) {
            throw IllegalArgumentException("流程JSON解析失败: ${e.message}", e)
        }
    }

    // 查找适合的处理器
    private fun findProcessor(nodeType: String): NodeProcessor? {
        return nodeProcessors.find { it.canProcess(nodeType) }
    }

    // 计算节点执行顺序（拓扑排序）
    private fun calculateExecutionOrder(flowProcess: FlowProcess): List<String> {
        val graph = buildGraph(flowProcess)
        return topologicalSort(graph)
    }

    // 构建图形结构
    private fun buildGraph(flowProcess: FlowProcess): Map<String, Set<String>> {
        val graph = mutableMapOf<String, MutableSet<String>>()

        // 初始化图
        flowProcess.nodes.forEach { node ->
            graph[node.id] = mutableSetOf()
        }

        // 添加边
        flowProcess.edges.forEach { edge ->
            graph[edge.sourceNodeId]?.add(edge.targetNodeId)
        }

        return graph
    }

    // 拓扑排序算法
    private fun topologicalSort(graph: Map<String, Set<String>>): List<String> {
        val visited = mutableSetOf<String>()
        val tempVisited = mutableSetOf<String>()
        val order = mutableListOf<String>()

        fun visit(nodeId: String) {
            if (tempVisited.contains(nodeId)) {
                throw IllegalStateException("流程中存在循环依赖")
            }

            if (!visited.contains(nodeId)) {
                tempVisited.add(nodeId)
                graph[nodeId]?.forEach { visit(it) }
                visited.add(nodeId)
                tempVisited.remove(nodeId)
                order.add(0, nodeId)
            }
        }

        graph.keys.forEach {
            if (!visited.contains(it)) {
                visit(it)
            }
        }

        return order
    }

    // 计算节点执行顺序（从FlowCanvasPanel复制并简化）
    private fun calculateExecutionOrder(nodes: List<Node>, edges: List<Edge>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val visited = mutableSetOf<String>()
        var order = 1

        // 找出所有没有入边的节点（起始节点）
        val startNodes = nodes.filter { node ->
            edges.none { it.targetNodeId == node.id }
        }

        // 从每个起始节点开始进行深度优先搜索
        for (startNode in startNodes) {
            dfs(startNode.id, visited, result, order, edges)
            order++
        }

        // 处理可能的孤立节点或环
        for (node in nodes) {
            if (node.id !in result) {
                result[node.id] = order++
            }
        }

        return result
    }

    private fun dfs(
        nodeId: String,
        visited: MutableSet<String>,
        result: MutableMap<String, Int>,
        order: Int,
        edges: List<Edge>
    ) {
        if (nodeId in visited) return

        visited.add(nodeId)
        result[nodeId] = order

        // 找出所有从当前节点出发的边
        val outEdges = edges.filter { it.sourceNodeId == nodeId }

        // 递归处理所有相邻节点
        for (edge in outEdges) {
            dfs(edge.targetNodeId, visited, result, order + 1, edges)
        }
    }
}