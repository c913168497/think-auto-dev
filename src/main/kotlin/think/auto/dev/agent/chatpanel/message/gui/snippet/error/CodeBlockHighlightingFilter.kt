package think.auto.dev.agent.chatpanel.message.gui.snippet.error

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiFile
import think.auto.dev.agent.chatpanel.message.MessageSnippetFile

class CodeBlockHighlightingFilter : HighlightInfoFilter {
    override fun accept(highlightInfo: HighlightInfo, file: PsiFile?): Boolean {
        val hasError = highlightInfo.severity >= HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING;

        if (file == null || !hasError) {
            return true;
        }

        val virtualFile = file.virtualFile;

        return !(virtualFile != null && MessageSnippetFile.isSnippet(virtualFile));
    }
}
