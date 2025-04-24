package think.auto.dev.settings.chatcontext

import think.auto.dev.settings.ThinkAutoDevDB
import java.sql.SQLException

object ChatContextDataBaseComponent {
    fun insertChatContextTitles(chatContextTitle: ChatContextTitle): Int {
        val sql = "INSERT INTO chat_context_title(id, title, aiProviderId) VALUES(?, ?, ?)"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setLong(1, chatContextTitle.id)
                    pstmt.setString(2, chatContextTitle.title)
                    pstmt.setInt(3, chatContextTitle.aiProviderId)
                    val affectedRows = pstmt.executeUpdate()
                    if (affectedRows == 0) {
                        throw SQLException("插入模板代码失败，未影响任何行。")
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常，例如记录日志
        }

        return 0
    }

    fun deleteChatContextTitles(id: Long): Int? {
        val sql = "DELETE FROM chat_context_title WHERE id = ?"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setLong(1, id)
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



    fun getAllChatContextTitles(): List<ChatContextTitle> {
        val templates = mutableListOf<ChatContextTitle>()
        val sql = "SELECT id, title, aiProviderId  FROM chat_context_title"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val template = ChatContextTitle(
                            id = rs.getLong("id"),
                            title = rs.getString("title"),
                            aiProviderId = rs.getInt("aiProviderId")
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

    fun getChatContextTitleById(id: Long): ChatContextTitle? {
        val templates = mutableListOf<ChatContextTitle>()
        val sql = "SELECT id, title, aiProviderId FROM chat_context_title where id = $id"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val template = ChatContextTitle(
                            id = rs.getLong("id"),
                            title = rs.getString("title"),
                            aiProviderId = rs.getInt("aiProviderId")
                        )
                        templates.add(template)
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常
        }

        if (templates.isNotEmpty()) {
            return templates[0]
        }

        return null
    }
    /**
     * 新增
     */
    fun insertChatContextItem(contextItem: ChatContextItem): Int? {
        val sql = "INSERT INTO chat_context_item(`titleId`, `role`, content, createTime) VALUES(?, ?, ?, ?)"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setLong(1, contextItem.titleId)
                    pstmt.setString(2, contextItem.role)
                    pstmt.setString(3, contextItem.content)
                    pstmt.setLong(4, contextItem.createTime)
                    val affectedRows = pstmt.executeUpdate()
                    if (affectedRows == 0) {
                        throw SQLException("插入失败")
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
    fun insertChatContextItems(contextItems: List<ChatContextItem>): List<Int?> {
        val sql = "INSERT INTO chat_context_item(`titleId`, `role`, content, createTime) VALUES(?, ?, ?, ?)"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get()
        val generatedKeysList = mutableListOf<Int?>()

        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    for (contextItem in contextItems) {
                        pstmt.setLong(1, contextItem.titleId)
                        pstmt.setString(2, contextItem.role)
                        pstmt.setString(3, contextItem.content)
                        pstmt.setLong(4, contextItem.createTime)
                        pstmt.addBatch()
                    }

                    val affectedRows = pstmt.executeBatch()
                    if (affectedRows.any { it == 0 }) {
                        throw SQLException("插入模板失败，未影响任何行。")
                    }
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常，例如记录日志
        }

        return generatedKeysList
    }

    fun deleteChatContextItems(titleId: Long): Int? {
        val sql = "DELETE FROM chat_context_item WHERE titleId = ?"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setLong(1, titleId)
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

    fun getAllChatContextItems(titleId:Long): List<ChatContextItem> {
        val templates = mutableListOf<ChatContextItem>()
        val sql = "SELECT id, titleId, role, content, createTime FROM chat_context_item WHERE titleId = $titleId order by createTime asc"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val template = ChatContextItem(
                            id = rs.getLong("id"),
                            titleId = rs.getLong("titleId"),
                            role = rs.getString("role"),
                            content = rs.getString("content"),
                            createTime = rs.getLong("createTime")
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
