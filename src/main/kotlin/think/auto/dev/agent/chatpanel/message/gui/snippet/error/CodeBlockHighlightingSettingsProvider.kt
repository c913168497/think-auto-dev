package think.auto.dev.agent.chatpanel.message.gui.snippet.error

import com.intellij.codeInsight.daemon.impl.analysis.DefaultHighlightingSettingProvider
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import think.auto.dev.agent.chatpanel.message.MessageSnippetFile

class CodeBlockHighlightingSettingsProvider : DefaultHighlightingSettingProvider() {
    override fun getDefaultSetting(project: Project, file: VirtualFile): FileHighlightingSetting? {
        return if (MessageSnippetFile.isSnippet(file)) FileHighlightingSetting.SKIP_HIGHLIGHTING else null
    }
}
