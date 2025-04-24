package think.auto.dev.settings

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

fun getThinkAutoDevSqlitePath(): String {
    return Paths.get(getIndexFolderPath(), "think-auto-dev.sqlite").toString()
}

fun getThinkAutoDevSqliteAbPath(): Path {
    return Paths.get(getIndexFolderPath(), "think-auto-dev.sqlite")
}


class ThinkAutoDevDB {

    private var db: Connection? = null

    private var indexSqlitePath = getThinkAutoDevSqlitePath()

    @Throws(SQLException::class)
    private fun createTables(db: Connection) {
        db.createStatement().use { stmt ->
            stmt.execute(""" CREATE TABLE IF NOT EXISTS prompts ( id INTEGER PRIMARY KEY AUTOINCREMENT, `title` STRING NOT NULL, `content` TEXT NOT NULL  )""")
            stmt.execute(
                """ CREATE TABLE IF NOT EXISTS prompt_functions ( id INTEGER PRIMARY KEY AUTOINCREMENT
                |, `title` STRING NOT NULL, `content` TEXT NOT NULL, `config` STRING NOT NULL )""".trimMargin()
            )
            stmt.execute(""" CREATE TABLE IF NOT EXISTS init_chat_context ( id INTEGER PRIMARY KEY AUTOINCREMENT, `title` STRING NOT NULL)""")
            stmt.execute(
                """ CREATE TABLE IF NOT EXISTS init_chat_context_node ( id INTEGER PRIMARY KEY AUTOINCREMENT,  chat_id INTEGER NOT NULL, `role` STRING NOT NULL, 
                |`content` TEXT NOT NULL, `index` INTEGER NOT NULL)""".trimMargin()
            )
            stmt.execute(
                """ CREATE TABLE IF NOT EXISTS ai_providers ( 
                |id INTEGER PRIMARY KEY AUTOINCREMENT, 
                |customOpenAiHost TEXT NOT NULL, customEngineToken TEXT NOT NULL, customEngineHead TEXT NOT NULL, customModel STRING NOT NULL, customModelName STRING NOT NULL, recordingInLocal Boolean NOT NULL)""".trimMargin()
            )
            stmt.execute(
                """ CREATE TABLE IF NOT EXISTS chat_context_title ( 
                |id INTEGER PRIMARY KEY AUTOINCREMENT, 
                |title STRING NOT NULL, aiProviderId STRING NOT NULL)""".trimMargin()
            )
            stmt.execute(
                """ CREATE TABLE IF NOT EXISTS chat_context_item ( 
                |id INTEGER PRIMARY KEY AUTOINCREMENT, 
                |titleId INTEGER NOT NULL, `role` STRING NOT NULL, content TEXT NOT NULL, createTime INTEGER NOT NULL)""".trimMargin()
            )

            stmt.execute(
                """ CREATE TABLE IF NOT EXISTS agent_function_flow_ai_provider ( 
                |id INTEGER PRIMARY KEY AUTOINCREMENT, 
                |functionName STRING NOT NULL, aiProviderId INTEGER NOT NULL)""".trimMargin()
            )


            stmt.execute(
                """ CREATE TABLE IF NOT EXISTS pseudocode_standard ( 
                |id INTEGER PRIMARY KEY AUTOINCREMENT, 
                |title STRING NOT NULL, content TEXT NOT NULL)""".trimMargin()
            )

            stmt.execute(
                """ CREATE TABLE IF NOT EXISTS flow_engine_config ( 
                |id INTEGER PRIMARY KEY AUTOINCREMENT, 
                |title STRING NOT NULL, content TEXT NOT NULL)""".trimMargin()
            )
//            stmt.execute(""" CREATE TABLE IF NOT EXISTS feature_prompt ( id INTEGER PRIMARY KEY AUTOINCREMENT, `title` STRING NOT NULL, `content` TEXT NOT NULL)""")
//            stmt.execute(""" CREATE TABLE IF NOT EXISTS init_prompt ( id INTEGER PRIMARY KEY AUTOINCREMENT, `title` STRING NOT NULL)""")
//            stmt.execute(
//                """ CREATE TABLE IF NOT EXISTS init_prompt_node ( id INTEGER PRIMARY KEY AUTOINCREMENT,  prompt_id INTEGER NOT NULL, `role` STRING NOT NULL,
//                |`content` TEXT NOT NULL, `index` INTEGER NOT NULL)""".trimMargin()
//            )

        }
    }

    @Throws(SQLException::class)
    fun get(): Connection {
        if (db != null && Files.exists(Paths.get(indexSqlitePath))) {
            return db!!
        }

        indexSqlitePath = getThinkAutoDevSqlitePath()
        db = DriverManager.getConnection("jdbc:sqlite:$indexSqlitePath")
        createTables(db!!)
        return db!!
    }
}

fun getAutoDevGlobalPath(): String {
    // This is ~/.GenerateDev on mac/linux
    val homeDir = System.getProperty("user.home")
    val autodevPath = Paths.get(homeDir, ".autodev")
    if (!autodevPath.exists()) {
        autodevPath.createDirectories()
    }

    return autodevPath.toString()
}

fun getIndexFolderPath(): String {
    val indexPath = Paths.get(getAutoDevGlobalPath(), "index")
    if (!indexPath.exists()) {
        indexPath.createDirectories()
    }
    return indexPath.toString()
}
