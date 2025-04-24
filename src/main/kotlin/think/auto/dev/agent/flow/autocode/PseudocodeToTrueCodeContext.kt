package think.auto.dev.agent.flow.autocode

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import think.auto.dev.agent.flow.core.WorkflowContext


data class PseudocodeToTrueCodeContext(
    var prompt: String = "",
    val virtualFile: VirtualFile,
    var psiFile: PsiFile,
    var project: Project,
    var psiElement: PsiElement,
    var classAndFunInfo: String,
    var editor: Editor,
    val fieldClasses: List<TrueCodeFieldClassCodeContext> = emptyList(),
) : WorkflowContext
