package think.auto.dev.settings.flowEngine.database
data class FlowEngineConfig(
    val id: Int? = null, // 数据库自增ID，新增时可以为空
    val title: String,
    val content: String
) {
    override fun toString(): String {
        return title
    }
}
