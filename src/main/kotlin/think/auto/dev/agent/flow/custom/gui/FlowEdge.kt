package think.auto.dev.agent.flow.custom.gui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlowEdge(
    @SerialName("id")
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("sourceNodeId")
    val sourceNodeId: String,
    @SerialName("targetNodeId")
    val targetNodeId: String
)