package think.auto.dev.agent.chatpanel.toolbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.ProjectManager
import think.auto.dev.agent.chatpanel.message.CompletableMessage
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.Icon

abstract class AutoDevRateMessageAction : DumbAwareToggleAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    abstract fun getReaction(): CompletableMessage.ChatMessageRating
    abstract fun getReactionIcon(): Icon
    abstract fun getReactionIconSelected(): Icon

    public fun getMessage(event: AnActionEvent): CompletableMessage? {
        return event.dataContext.getData(CompletableMessage.key)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val icon: Icon = if (!isSelected(e)) getReactionIcon() else getReactionIconSelected()
        e.presentation.setIcon(icon)
    }

    override fun isSelected(e: AnActionEvent): Boolean {
        return getMessage(e)?.rating == getReaction()
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: ProjectManager.getInstance().openProjects.firstOrNull() ?: return
        val message = getMessage(e) ?: return
        message.rating = if (isSelected(e)) CompletableMessage.ChatMessageRating.None else getReaction()
    }

    class Copy : AutoDevRateMessageAction() {
        override fun getReaction(): CompletableMessage.ChatMessageRating = CompletableMessage.ChatMessageRating.Copy

        override fun getReactionIcon(): Icon = AllIcons.Actions.Copy

        override fun getReactionIconSelected(): Icon = AllIcons.Actions.Copy

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            val message = getMessage(e) ?: return
            val selection = StringSelection(message.text)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, null)
        }
    }
}
