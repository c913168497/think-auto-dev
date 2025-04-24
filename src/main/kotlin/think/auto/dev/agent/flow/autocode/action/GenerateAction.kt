package think.auto.dev.agent.flow.autocode.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import think.auto.dev.agent.flow.autocode.TrueCodeFieldClassCodeContext
import think.auto.dev.agent.flow.autocode.PseudocodeToTrueCodeContext
import think.auto.dev.agent.flow.autocode.PseudocodeToTrueCodeTask
import think.auto.dev.utils.getElementToAction

class GenerateAction : AnAction() {

    init {
        templatePresentation.text = "伪代码生成真实代码"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val offset = editor?.caretModel?.offset ?: 0
        if (psiFile == null || psiFile.virtualFile == null) {
            return
        }
        val element = getElementToAction(project, editor) ?: return
        val selectedText = editor?.selectionModel?.selectedText ?: return
        val classContextList = getClassField(psiFile, offset)
        val autoCodeContext = PseudocodeToTrueCodeContext(
            prompt = selectedText,
            project = project,
            psiFile = psiFile,
            virtualFile = psiFile.virtualFile,
            psiElement = element,
            classAndFunInfo = "",
            editor = editor,
            fieldClasses = classContextList
        )

        val autoGenCodeTask = PseudocodeToTrueCodeTask(
            project,
            autoCodeContext
        )

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(autoGenCodeTask, BackgroundableProcessIndicator(autoGenCodeTask))

    }

    private fun getClassField(psiFile: PsiFile, offset: Int): List<TrueCodeFieldClassCodeContext> {
        val fieldClassList = mutableListOf<TrueCodeFieldClassCodeContext>()
        psiFile.findElementAt(offset)?.let { element ->
            PsiTreeUtil.getParentOfType(element, PsiClass::class.java)?.let { clazz ->
                buildString {
                    val fields = clazz.fields
                    for (field in fields) {
                        if (field.modifierList?.annotations?.any { annotation ->
                                annotation.qualifiedName?.endsWith("Resource") == true
                            } == true) {
                            val targetClass = PsiUtil.resolveClassInType(field.type)
                            if (targetClass != null) {
                                fieldClassList.add(TrueCodeFieldClassCodeContext(field.name, targetClass))
                            }
                        }
                    }
                }
            } ?: "当前不在类内部"
        } ?: "光标位置无效"

        return fieldClassList
    }
}
