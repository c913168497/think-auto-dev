package think.auto.dev.agent.flow.custom.gui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class NodeType(val displayName: String) {
//    EXTRACT_FROM_CLASS("根据类名抽取代码"),
//    WRITE_FILE("写入文件"),
    MOUSE_SELECTION("鼠标选中内容"),
    FOLDER_CONTENT_GET("读取鼠标选中文件内容"),
//    GET_TABLE_STRUCTURE("根据表名获取表结构"),
    PROMPT_EDIT("提示词编辑"),
    REGEX_PROCESS("正则过滤处理"),
    AI_MODEL_PROCESS("大模型处理"),
    GET_CLASS_CONTENT("根据类名获取类内容"),

}
@Serializable
data class FlowNode(
    @SerialName("id")
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("type")
    val type: NodeType,
    @SerialName("name")
    val name: String,
    @SerialName("config")
    val config: String = "",
    @SerialName("x")
    val x: Int = 0,
    @SerialName("y")
    val y: Int = 0
)