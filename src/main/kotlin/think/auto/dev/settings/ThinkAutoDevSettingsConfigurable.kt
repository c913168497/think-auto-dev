package think.auto.dev.settings

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nullable
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle
import javax.swing.JComponent

class ThinkAutoDevSettingsConfigurable : Configurable {
    private val component: LLMParamSettingUI = LLMParamSettingUI(ThinkAutoDevSettingsState.getInstance())

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String = ThinkAutoDevMessagesBundle.message("name")

    override fun apply() = component.exportSettings(ThinkAutoDevSettingsState.getInstance())

    override fun reset() = component.applySettings(ThinkAutoDevSettingsState.getInstance(), true)
    override fun getPreferredFocusedComponent(): JComponent? = null

    @Nullable
    override fun createComponent(): JComponent = component.panel

    override fun isModified(): Boolean {
        val settings: ThinkAutoDevSettingsState = ThinkAutoDevSettingsState.getInstance()
        return component.isModified(settings)
    }
}
