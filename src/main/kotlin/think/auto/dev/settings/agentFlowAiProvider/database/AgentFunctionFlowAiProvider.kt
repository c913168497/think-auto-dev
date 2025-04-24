package think.auto.dev.settings.agentFlowAiProvider.database

data class AgentFunctionFlowAiProvider(
    val id: Int? = null, // 数据库自增ID，新增时可以为空
    val functionName: String,
    var aiProviderId: Int
) {
    override fun toString(): String {
        return functionName
    }
}
