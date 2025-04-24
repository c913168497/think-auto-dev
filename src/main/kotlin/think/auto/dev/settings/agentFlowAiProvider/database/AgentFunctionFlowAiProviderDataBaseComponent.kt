package think.auto.dev.settings.agentFlowAiProvider.database

import cn.hutool.core.collection.CollUtil
import think.auto.dev.settings.ThinkAutoDevDB
import java.sql.SQLException

object AgentFunctionFlowAiProviderDataBaseComponent {
    fun insertAgentFunctionFlowAiProvider(agentFunctionFlowAiProvider: AgentFunctionFlowAiProvider): Int? {
        val sql = "INSERT INTO agent_function_flow_ai_provider (functionName, aiProviderId) VALUES(?, ?)"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, agentFunctionFlowAiProvider.functionName)
                    pstmt.setInt(2, agentFunctionFlowAiProvider.aiProviderId)
                    val affectedRows = pstmt.executeUpdate()
                    if (affectedRows == 0) {
                        throw SQLException("插入模板代码失败，未影响任何行。")
                    }

                    // 获取最后插入的行 ID
                    val lastInsertIdSql = "SELECT last_insert_rowid()"
                    conn.prepareStatement(lastInsertIdSql).use { lastInsertIdStmt ->
                        lastInsertIdStmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                return rs.getInt(1)
                            }
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常，例如记录日志
        }

        return 0
    }

    fun updateAgentFunctionFlowAiProvider(prompt: AgentFunctionFlowAiProvider): Int? {
        val sql = "UPDATE agent_function_flow_ai_provider SET aiProviderId = ? WHERE id = ?"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setInt(1, prompt.aiProviderId)
                    pstmt.setInt(2, prompt.id!!)
                    val affectedRows = pstmt.executeUpdate()
                    if (affectedRows == 0) {
                        throw SQLException("插入模板失败，未影响任何行。")
                    }

                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常，例如记录日志
        }
        return null
    }

    fun getAgentFunctionFlowAiProviderById(id: Int): AgentFunctionFlowAiProvider? {
        val featureCodeBases = mutableListOf<AgentFunctionFlowAiProvider>()
        val sql = "SELECT id, functionName, aiProviderId FROM agent_function_flow_ai_provider WHERE id = '$id'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val featureCodeBase = AgentFunctionFlowAiProvider(
                            id = rs.getInt("id"),
                            functionName = rs.getString("functionName"),
                            aiProviderId = rs.getInt("aiProviderId")
                        )
                        featureCodeBases.add(featureCodeBase)
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常
        }

        if (CollUtil.isNotEmpty(featureCodeBases)) {
            return featureCodeBases[0]
        }

        return null
    }

    fun getAgentFunctionFlowAiProviderByTitle(functionName: String): AgentFunctionFlowAiProvider? {
        val featureCodeBases = mutableListOf<AgentFunctionFlowAiProvider>()
        val sql = "SELECT id, functionName, aiProviderId  FROM agent_function_flow_ai_provider WHERE functionName = '$functionName'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val featureCodeBase = AgentFunctionFlowAiProvider(
                            id = rs.getInt("id"),
                            functionName = rs.getString("functionName"),
                            aiProviderId = rs.getInt("aiProviderId")
                        )
                        featureCodeBases.add(featureCodeBase)
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常
        }

        if (CollUtil.isNotEmpty(featureCodeBases)) {
            return featureCodeBases[0]
        }

        return null
    }

}
