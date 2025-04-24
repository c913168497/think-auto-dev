package think.auto.dev.utils

import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField

fun JsonLanguageField(myProject: Project?, value: String, placeholder: String): LanguageTextField {
    return JsonTextProvider.create(myProject, value, placeholder)
}


