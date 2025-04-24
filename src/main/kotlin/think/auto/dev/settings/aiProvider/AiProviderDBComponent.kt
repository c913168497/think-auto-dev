package think.auto.dev.settings.aiProvider

import cn.hutool.core.collection.CollUtil
import think.auto.dev.settings.ThinkAutoDevDB
import java.sql.SQLException

object AiProviderDBComponent {
    fun insertAiProvider(provider: AiProvider): Int? {
        val sql = "INSERT INTO ai_providers(customOpenAiHost, customEngineToken, customEngineHead, customModel, customModelName, recordingInLocal) VALUES(?, ?, ?, ?, ?, ?)"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get()
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, provider.customOpenAiHost)
                    pstmt.setString(2, provider.customEngineToken)
                    pstmt.setString(3, provider.customEngineHead)
                    pstmt.setString(4, provider.customModel)
                    pstmt.setString(5, provider.customModelName)
                    pstmt.setBoolean(6, provider.recordingInLocal)
                    val affectedRows = pstmt.executeUpdate()
                    if (affectedRows == 0) {
                        throw SQLException("插入AI提供商失败，未影响任何行。")
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

    fun deleteAiProvider(id: Int): Int? {
        val sql = "DELETE FROM ai_providers WHERE id = ?"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get()
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setInt(1, id)
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

    fun updateAiProvider(provider: AiProvider): Int? {
        val sql = "UPDATE ai_providers SET customOpenAiHost = ?, customEngineToken = ?, customEngineHead = ?, customModel = ?, customModelName = ?, recordingInLocal = ? WHERE id = ?"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get()
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, provider.customOpenAiHost)
                    pstmt.setString(2, provider.customEngineToken)
                    pstmt.setString(3, provider.customEngineHead)
                    pstmt.setString(4, provider.customModel)
                    pstmt.setString(5, provider.customModelName)
                    pstmt.setBoolean(6, provider.recordingInLocal)
                    pstmt.setInt(7, provider.id!!)
                    val affectedRows = pstmt.executeUpdate()
                    if (affectedRows == 0) {
                        throw SQLException("更新AI提供商失败，未影响任何行。")
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常，例如记录日志
        }
        return null
    }

    fun getAiProviderById(id: Int): AiProvider? {
        val providers = mutableListOf<AiProvider>()
        val sql = "SELECT id, customOpenAiHost, customEngineToken, customEngineHead, customModel, customModelName, recordingInLocal FROM ai_providers WHERE id = '$id'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get()
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val provider = AiProvider(
                            id = rs.getInt("id"),
                            customOpenAiHost = rs.getString("customOpenAiHost"),
                            customEngineToken = rs.getString("customEngineToken"),
                            customEngineHead = rs.getString("customEngineHead"),
                            customModel = rs.getString("customModel"),
                            customModelName = rs.getString("customModelName"),
                            recordingInLocal = rs.getBoolean("recordingInLocal")
                        )
                        providers.add(provider)
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常
        }

        if (CollUtil.isNotEmpty(providers)) {
            return providers[0]
        }

        return null
    }

    fun getAiProviderByModelName(modelName: String): AiProvider? {
        val providers = mutableListOf<AiProvider>()
        val sql = "SELECT id, customOpenAiHost, customEngineToken, customEngineHead, customModel, customModelName, recordingInLocal FROM ai_providers WHERE customModelName = '$modelName'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get()
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val provider = AiProvider(
                            id = rs.getInt("id"),
                            customOpenAiHost = rs.getString("customOpenAiHost"),
                            customEngineToken = rs.getString("customEngineToken"),
                            customEngineHead = rs.getString("customEngineHead"),
                            customModel = rs.getString("customModel"),
                            customModelName = rs.getString("customModelName"),
                            recordingInLocal = rs.getBoolean("recordingInLocal")
                        )
                        providers.add(provider)
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常
        }

        if (CollUtil.isNotEmpty(providers)) {
            return providers[0]
        }

        return null
    }

    fun getAllAiProviders(): List<AiProvider> {
        val providers = mutableListOf<AiProvider>()
        val sql = "SELECT id, customOpenAiHost, customEngineToken, customEngineHead, customModel, customModelName, recordingInLocal FROM ai_providers"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get()
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val provider = AiProvider(
                            id = rs.getInt("id"),
                            customOpenAiHost = rs.getString("customOpenAiHost"),
                            customEngineToken = rs.getString("customEngineToken"),
                            customEngineHead = rs.getString("customEngineHead"),
                            customModel = rs.getString("customModel"),
                            customModelName = rs.getString("customModelName"),
                            recordingInLocal = rs.getBoolean("recordingInLocal")
                        )
                        providers.add(provider)
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常
        }
        return providers
    }
}