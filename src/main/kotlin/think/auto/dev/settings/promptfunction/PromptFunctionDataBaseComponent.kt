package think.auto.dev.settings.promptfunction

import cn.hutool.core.collection.CollUtil
import think.auto.dev.settings.ThinkAutoDevDB
import java.sql.SQLException

object PromptFunctionDataBaseComponent {

    /**
     * 新增提示词功能
     */
    fun insertPromptFunction(promptFunction: PromptFunction): Int? {
        val sql = "INSERT INTO prompt_functions(title, content, config) VALUES(?, ?, ?)"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, promptFunction.title)
                    pstmt.setString(2, promptFunction.content)
                    pstmt.setString(3, promptFunction.config)
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

    fun deletePromptFunction(id: Int): Int? {
        val sql = "DELETE FROM prompt_functions WHERE id = ?"
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

    fun updatePromptFunction(promptFunction: PromptFunction): Int? {
        val sql = "UPDATE prompt_functions SET title = ?, content = ?, config = ? WHERE id = ?"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, promptFunction.title)
                    pstmt.setString(2, promptFunction.content)
                    pstmt.setString(3, promptFunction.config)
                    pstmt.setInt(4, promptFunction.id!!)
                    val affectedRows = pstmt.executeUpdate()
                    if (affectedRows == 0) {
                        throw SQLException("插入失败，未影响任何行。")
                    }

                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            // 处理异常，例如记录日志
        }
        return null
    }

    fun getPromptFunctionById(id: Int): PromptFunction? {
        val featureCodeBases = mutableListOf<PromptFunction>()
        val sql = "SELECT id, title, content, config FROM prompt_functions WHERE id = '$id'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val featureCodeBase = PromptFunction(
                            id = rs.getInt("id"),
                            title = rs.getString("title"),
                            content = rs.getString("content"),
                            config = rs.getString("config")
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

    fun getPromptFunctionByTitle(title: String): PromptFunction? {
        val featureCodeBases = mutableListOf<PromptFunction>()
        val sql = "SELECT id, title, content, config FROM prompt_functions WHERE title = '$title'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val featureCodeBase = PromptFunction(
                            id = rs.getInt("id"),
                            title = rs.getString("title"),
                            content = rs.getString("content"),
                            config = rs.getString("config")
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

    fun getAllPrompts(): List<PromptFunction> {
        val templates = mutableListOf<PromptFunction>()
        val sql = "SELECT id, title, content, config  FROM prompt_functions"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val template = PromptFunction(
                            id = rs.getInt("id"),
                            title = rs.getString("title"),
                            content = rs.getString("content"),
                            config = rs.getString("config")
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


    fun getAllPromptByTitle(): List<PromptFunction> {
        val templates = mutableListOf<PromptFunction>()
        val sql = "SELECT id, title, content, config FROM prompt_functions"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val template = PromptFunction(
                            id = rs.getInt("id"),
                            title = rs.getString("title"),
                            content = rs.getString("content"),
                            config = rs.getString("config")
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
