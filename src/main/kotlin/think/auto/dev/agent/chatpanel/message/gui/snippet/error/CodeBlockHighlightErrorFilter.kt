package think.auto.dev.agent.chatpanel.message.gui.snippet.error

import com.intellij.codeInsight.highlighting.HighlightErrorFilter
import com.intellij.psi.PsiErrorElement
import think.auto.dev.agent.chatpanel.message.MessageSnippetFile

class CodeBlockHighlightErrorFilter : HighlightErrorFilter() {
    override fun shouldHighlightErrorElement(element: PsiErrorElement): Boolean {
        val containingFile = element.containingFile
        val highlightedFile = containingFile?.virtualFile ?: return true
        return !MessageSnippetFile.isSnippet(highlightedFile)
    }
}
