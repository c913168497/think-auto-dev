package think.auto.dev.settings.flowEngine.database
import cn.hutool.core.collection.CollUtil
import think.auto.dev.settings.ThinkAutoDevDB
import java.sql.SQLException

object FlowEngineConfigDataBaseComponent {
    fun insertFlowEngineConfig(flowEngineConfig: FlowEngineConfig): Int? {
        val sql = "INSERT INTO flow_engine_config(title, content) VALUES(?, ?)"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, flowEngineConfig.title)
                    pstmt.setString(2, flowEngineConfig.content)
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

    fun deleteFlowEngineConfig(title: String): Int? {
        val sql = "DELETE FROM flow_engine_config WHERE title = ?"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, title)
                    val affectedRows = pstmt.executeUpdate()
                    if (affectedRows == 0) {
                        throw SQLException("删除失败，未影响任何行。")
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常，例如记录日志
        }
        return null
    }

    fun updateFlowEngineConfig(flowEngineConfig: FlowEngineConfig): Int? {
        val sql = "UPDATE flow_engine_config SET title = ?, content = ? WHERE id = ?"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, flowEngineConfig.title)
                    pstmt.setString(2, flowEngineConfig.content)
                    pstmt.setInt(3, flowEngineConfig.id!!)
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

    fun getFlowEngineConfigById(id: Int): FlowEngineConfig? {
        val featureCodeBases = mutableListOf<FlowEngineConfig>()
        val sql = "SELECT id, title, content FROM flow_engine_config WHERE id = '$id'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val featureCodeBase = FlowEngineConfig(
                            id = rs.getInt("id"),
                            title = rs.getString("title"),
                            content = rs.getString("content")
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

    fun getFlowEngineConfigByTitle(title: String): FlowEngineConfig? {
        val featureCodeBases = mutableListOf<FlowEngineConfig>()
        val sql = "SELECT id, title, content FROM flow_engine_config WHERE title = '$title'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val featureCodeBase = FlowEngineConfig(
                            id = rs.getInt("id"),
                            title = rs.getString("title"),
                            content = rs.getString("content")
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

    fun getAllFlowEngineConfig(): List<FlowEngineConfig> {
        val templates = mutableListOf<FlowEngineConfig>()
        val sql = "SELECT id, title, content FROM flow_engine_config"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val template = FlowEngineConfig(
                            id = rs.getInt("id"),
                            title = rs.getString("title"),
                            content = rs.getString("content")
                        )
                        templates.add(template)
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常
        }
        return templates
    }

    fun getAllPromptByTitle(): List<FlowEngineConfig> {
        val templates = mutableListOf<FlowEngineConfig>()
        val sql = "SELECT id, title, content FROM flow_engine_config"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val template = FlowEngineConfig(
                            id = rs.getInt("id"),
                            title = rs.getString("title"),
                            content = rs.getString("content")
                        )
                        templates.add(template)
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常
        }
        return templates
    }
}
