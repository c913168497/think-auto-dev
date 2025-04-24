package think.auto.dev.agent.flow.autocode
import com.intellij.psi.PsiClass


data class TrueCodeFieldClassCodeContext(
    var fileName: String,
    val pisClass: PsiClass,
)
