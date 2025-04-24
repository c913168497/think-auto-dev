package think.auto.dev.common.ui.statusbar

import com.intellij.util.messages.Topic

interface ThinkAutoDevStatusListener {
    fun onCopilotStatus(status: ThinkAutoDevStatus, icon: String?)

    companion object {
        val TOPIC = Topic.create("autodev.status", ThinkAutoDevStatusListener::class.java)
    }
}