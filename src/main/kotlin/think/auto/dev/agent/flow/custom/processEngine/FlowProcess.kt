package think.auto.dev.agent.flow.custom.processEngine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 流程数据模型

@Serializable
data class FlowProcess(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("nodes")
    val nodes: List<Node>,
    @SerialName("edges")
    val edges: List<Edge>
)
@Serializable
data class Node(
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String,
    @SerialName("name")
    val name: String,
    @SerialName("config")
    val config: String,
    @SerialName("x")
    val x: Int,
    @SerialName("y")
    val y: Int

)
@Serializable
data class Edge(
    @SerialName("id")
    val id: String,
    @SerialName("sourceNodeId")
    val sourceNodeId: String,
    @SerialName("targetNodeId")
    val targetNodeId: String
)

// 执行状态枚举
@Serializable
enum class ExecutionStatus {
    PENDING, RUNNING, SUCCESS, FAILURE, SKIPPED
}

// 节点执行结果
@Serializable
data class NodeExecutionResult(
    @SerialName("nodeId")
    val nodeId: String,
    @SerialName("nodeName")
    val nodeName: String,
    @SerialName("output")
    val output: String?,
    @SerialName("status")
    val status: ExecutionStatus,
    @SerialName("message")
    val message: String = "",
)
