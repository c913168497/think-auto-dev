package think.auto.dev.settings.pseudocode.database

import cn.hutool.core.collection.CollUtil
import think.auto.dev.settings.ThinkAutoDevDB
import java.sql.SQLException

object PseudocodeStandardDataBaseComponent {
    fun insertPseudocodeStandard(pseudocodeStandard: PseudocodeStandard): Int? {
        val sql = "INSERT INTO pseudocode_standard(title, content) VALUES(?, ?)"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, pseudocodeStandard.title)
                    pstmt.setString(2, pseudocodeStandard.content)
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

    fun deletePseudocodeStandard(id: Int): Int? {
        val sql = "DELETE FROM pseudocode_standard WHERE id = ?"
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

    fun updatePseudocodeStandard(prompt: PseudocodeStandard): Int? {
        val sql = "UPDATE pseudocode_standard SET title = ?, content = ? WHERE id = ?"
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

    fun getPseudocodeStandardById(id: Int): PseudocodeStandard? {
        val featureCodeBases = mutableListOf<PseudocodeStandard>()
        val sql = "SELECT id, title, content FROM pseudocode_standard WHERE id = '$id'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val featureCodeBase = PseudocodeStandard(
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

    fun getPseudocodeStandardByTitle(title: String): PseudocodeStandard? {
        val featureCodeBases = mutableListOf<PseudocodeStandard>()
        val sql = "SELECT id, title, content FROM pseudocode_standard WHERE title = '$title'"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val featureCodeBase = PseudocodeStandard(
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

    fun getAllPseudocodeStandardTitle(): MutableList<PseudocodeStandard> {
        val templates = mutableListOf<PseudocodeStandard>()
        val sql = "SELECT id, title, content FROM pseudocode_standard"
        val sqlite = ThinkAutoDevDB()
        val sqliteDb = sqlite.get();
        try {
            sqliteDb.use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(sql)
                    while (rs.next()) {
                        val template = PseudocodeStandard(
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
