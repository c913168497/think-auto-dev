package think.auto.dev.utils

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache

fun findPsiClassesByClassName(project: Project, className: String): List<PsiClass> {
    return ReadAction.compute<List<PsiClass>, Throwable> {
        val shortNamesCache = PsiShortNamesCache.getInstance(project)
        val psiClasses = shortNamesCache.getClassesByName(className, GlobalSearchScope.allScope(project))
        psiClasses.toList()
    }
}

fun findPsiClassesByClassNameSafely(project: Project, className: String): List<PsiClass> {
    val dumbService = DumbService.getInstance(project)
    if (dumbService.isDumb) {
        // 等待索引完成
        dumbService.runWhenSmart {
            findPsiClassesByClassNameSafely(project, className)
        }
    } else {
        // 索引完成，执行搜索
        val psiClasses = findPsiClassesByClassName(project, className)
        return psiClasses
    }

    return emptyList()
}
