package think.auto.dev.agent.chatpanel

import com.intellij.openapi.editor.ex.EditorEx
import java.util.*

interface ChatPanelInputListener : EventListener {
    fun editorAdded(editor: EditorEx) {}
    fun onSubmit(component: ChatPanelInputSection, trigger: ChatPanelInputTrigger) {}
    fun onStop(component: ChatPanelInputSection) {}
}