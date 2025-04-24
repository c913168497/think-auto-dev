package think.auto.dev.common.ui.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import org.jetbrains.annotations.NonNls
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle

class AutoDevStatusBarWidgetFactory : StatusBarWidgetFactory {

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun getId(): @NonNls String = "GenerateAuto"

    override fun getDisplayName(): @NlsContexts.ConfigurableName String {
        return ThinkAutoDevMessagesBundle.message("autodev.statusbar.name")
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return ThinkAutoDevStatusBarWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }
}
