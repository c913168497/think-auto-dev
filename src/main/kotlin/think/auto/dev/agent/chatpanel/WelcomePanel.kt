package think.auto.dev.agent.chatpanel

import com.intellij.ui.dsl.builder.panel
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle
import java.awt.BorderLayout
import javax.swing.JPanel

class WelcomePanel : JPanel(BorderLayout()) {
    // 聊天界面，提示
    private val welcomeItems: List<WelcomeItem> = listOf(
        WelcomeItem(ThinkAutoDevMessagesBundle.message("settings.welcome.feature.context")),
    )

    init {
        val panel = panel {
            welcomeItems.forEach {
                row {
                    text(it.text)
                }
            }
        }.apply {
            border = javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20)
        }

        add(panel, BorderLayout.CENTER)
    }
}