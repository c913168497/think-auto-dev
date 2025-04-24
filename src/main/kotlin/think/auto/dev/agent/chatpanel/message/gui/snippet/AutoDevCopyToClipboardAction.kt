package think.auto.dev.agent.chatpanel.message.gui.snippet

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * 对话panel中，code模块的 toolbar , 复制代码到文件 button
 */
class AutoDevCopyToClipboardAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.EDITOR) ?: return
        val document = editor.document
        val text = document.text

        val selection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, null)
    }

}
