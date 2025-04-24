package think.auto.dev.common.ui.statusbar

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle

class ThinkAutoDevStatusBarWidget(project: Project) : EditorBasedStatusBarPopup(project, false) {
    override fun ID(): String = ThinkAutoDevMessagesBundle.message("autodev.statusbar.id")
    override fun createInstance(project: Project): StatusBarWidget {
        return ThinkAutoDevStatusBarWidget(project)
    }

    override fun createPopup(context: DataContext): ListPopup? {
        val configuredGroup = ActionManager.getInstance().getAction(ID()) as? ActionGroup ?: return null

        return JBPopupFactory.getInstance().createActionGroupPopup(
            ThinkAutoDevMessagesBundle.message("autodev.statusbar.popup.title"),
            DefaultActionGroup(configuredGroup),
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )
    }

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        val widgetState = WidgetState("", "", true)
        val currentStatus = ThinkAutoDevStatusService.currentStatus.first
        return widgetState
    }

    override fun dispose() {
        super.dispose()
    }

    companion object {
        fun update(project: Project) {
            val bar = WindowManager.getInstance().getStatusBar(project)
            val barWidget =
                bar.getWidget(ThinkAutoDevMessagesBundle.message("autodev.statusbar.id")) as? ThinkAutoDevStatusBarWidget ?: return

            barWidget.update {
                barWidget.myStatusBar?.updateWidget(ThinkAutoDevMessagesBundle.message("autodev.statusbar.id"))
            }
        }
    }
}
