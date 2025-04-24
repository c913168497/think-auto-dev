package think.auto.dev.agent.chatpanel.message.gui.snippet.error

import com.intellij.codeInsight.daemon.impl.IntentionActionFilter
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiFile
import think.auto.dev.agent.chatpanel.message.MessageSnippetFile

class CodeBlockIntentionActionFilter : IntentionActionFilter {
    override fun accept(intentionAction: IntentionAction, file: PsiFile?): Boolean {
        val virtualFile = file?.virtualFile ?: return true
        return !MessageSnippetFile.isSnippet(virtualFile)
    }
}
