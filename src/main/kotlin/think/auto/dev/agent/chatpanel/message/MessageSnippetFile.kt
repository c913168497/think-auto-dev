package think.auto.dev.agent.chatpanel.message

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

/**
 * 消息是否是片段
 */
const val SNIPPET_NAME = "ThinkAutoDevSnippetName"

/**
 * 判断文件内容是否是片段代码
 */
object MessageSnippetFile {
    fun isSnippet(file: VirtualFile): Boolean {
        if (file is LightVirtualFile) {
            return file.getName() == SNIPPET_NAME
        }

        return false
    }
}