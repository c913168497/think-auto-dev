package think.auto.dev.settings

import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsUtil
import com.intellij.openapi.editor.ex.EditorEx

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField

import java.awt.Dimension
import java.awt.FontMetrics

class DefaultLanguageField(
    private val myProject: Project?,
    val value: String,
    private val placeholder: String,
) :
    LanguageTextField(PlainTextLanguage.INSTANCE, myProject, value,
        object : SimpleDocumentCreator() {
            override fun createDocument(value: String?, language: Language?, project: Project?): Document {
                return createDocument(value, language, project, this)
            }
        }
    ) {

    override fun createEditor(): EditorEx {
        return super.createEditor().apply {
            setShowPlaceholderWhenFocused(true)
            setHorizontalScrollbarVisible(false)
            setVerticalScrollbarVisible(true)
            setPlaceholder(placeholder)
            val scheme = EditorColorsUtil.getColorSchemeForBackground(this.colorsScheme.defaultBackground)
            this.colorsScheme = this.createBoundColorSchemeDelegate(scheme)
            val metrics: FontMetrics = getFontMetrics(font)
            val columnWidth = metrics.charWidth('m')
            isOneLineMode = false
            preferredSize = Dimension(25 * columnWidth, 25 * metrics.height)
        }
    }
}
