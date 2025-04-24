package think.auto.dev.settings.prompt.database

import cn.hutool.core.collection.CollUtil
import think.auto.dev.settings.ThinkAutoDevDB
import java.sql.SQLException

object PromptDataBaseComponent {
    fun insertPrompts(prompt: Prompt): Int? {
        val sql = "INSERT INTO prompts(title, content) VALUES(?, ?)"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, prompt.title)
                    pstmt.setString(2, prompt.content)
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

    fun deletePrompts(id: Int): Int? {
        val sql = "DELETE FROM prompts WHERE id = ?"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
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

    fun updatePrompts(prompt: Prompt): Int? {
        val sql = "UPDATE prompts SET title = ?, content = ? WHERE id = ?"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, prompt.title)
                    pstmt.setString(2, prompt.content)
                    pstmt.setInt(3, prompt.id!!)
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

    fun getPromptsById(id: Int): Prompt? {
        val featureCodeBases = mutableListOf<Prompt>()
        val sql = "SELECT id, title, content FROM prompts WHERE id = '$id'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val featureCodeBase = Prompt(
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

    fun getPromptsByTitle(title: String): Prompt? {
        val featureCodeBases = mutableListOf<Prompt>()
        val sql = "SELECT id, title, content FROM prompts WHERE title = '$title'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val featureCodeBase = Prompt(
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

    fun getAllPrompts(): List<Prompt> {
        val templates = mutableListOf<Prompt>()
        val sql = "SELECT id, title, content FROM prompts"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val template = Prompt(
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

    fun getAllPromptByTitle(): List<Prompt> {
        val templates = mutableListOf<Prompt>()
        val sql = "SELECT id, title, content FROM prompts"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val template = Prompt(
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
