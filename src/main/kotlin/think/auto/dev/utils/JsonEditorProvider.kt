package think.auto.dev.utils

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import think.auto.dev.settings.DefaultLanguageField


interface JsonTextProvider {
    fun createComponent(myProject: Project?, value: String, placeholder: String): LanguageTextField

    companion object {
        private val EP_NAME: ExtensionPointName<JsonTextProvider> =
            ExtensionPointName("think.auto.dev.jsonTextProvider")

        fun create(myProject: Project?, value: String, placeholder: String): LanguageTextField {
            return EP_NAME.extensionList.map {
                it.createComponent(myProject, value, placeholder)
            }.firstOrNull() ?: DefaultLanguageField(myProject, value, placeholder)
        }
    }
}


