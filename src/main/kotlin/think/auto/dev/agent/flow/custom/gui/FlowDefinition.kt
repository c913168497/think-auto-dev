package think.auto.dev.agent.flow.custom.gui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlowDefinition(
    @SerialName("id")
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("name")
    val name: String,
    @SerialName("nodes")
    val nodes: List<FlowNode> = emptyList(),
    @SerialName("edges")
    val edges: List<FlowEdge> = emptyList()
)