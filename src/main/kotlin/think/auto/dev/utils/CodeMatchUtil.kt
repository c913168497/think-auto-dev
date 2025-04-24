package think.auto.dev.utils

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage

class MessageFormatCode private constructor(
    val language: Language,
    val text: String,
    val isComplete: Boolean
) {
    companion object {
        private val CODE_BLOCK_REGEX = Regex("^```([\\w#+\\-]*)\\s*$")
        private val LANGUAGE_ALIASES = mapOf(
            "csharp" to "c#",
            "cpp" to "c++",
            "js" to "javascript",
            "ts" to "typescript",
            "py" to "python"
        )

        fun parse(content: String): MessageFormatCode {
            require(content.isNotEmpty()) { "Content cannot be empty" }

            val lines = content.replace("\\n", "\n").lines()
            var codeStarted = false
            var languageId: String? = null
            val codeBuilder = StringBuilder()

            for (line in lines) {
                when {
                    !codeStarted -> {
                        CODE_BLOCK_REGEX.find(line.trimStart())?.let { matchResult ->
                            languageId = matchResult.groups[1]?.value?.takeIf { it.isNotEmpty() }
                            codeStarted = true
                        }
                    }
                    line.trim() == "```" -> {
                        return buildMessageFormatCode(
                            languageId,
                            codeBuilder.toString(),
                            content,
                            isComplete = true
                        )
                    }
                    else -> codeBuilder.append(line).append("\n")
                }
            }

            return buildMessageFormatCode(
                languageId,
                codeBuilder.toString(),
                content,
                isComplete = false
            )
        }

        private fun buildMessageFormatCode(
            languageId: String?,
            code: String,
            originalContent: String,
            isComplete: Boolean
        ): MessageFormatCode {
            val trimmedCode = code.trim()

            return when {
                trimmedCode.isEmpty() ->
                    MessageFormatCode(
                        findLanguage("markdown"),
                        originalContent.replace("\\n", "\n"),
                        isComplete
                    )
                else ->
                    MessageFormatCode(
                        findLanguage(languageId),
                        trimmedCode,
                        isComplete
                    )
            }
        }

        fun findLanguage(languageName: String?): Language {
            if (languageName.isNullOrEmpty()) {
                return PlainTextLanguage.INSTANCE
            }

            val normalizedName = LANGUAGE_ALIASES[languageName.lowercase()] ?: languageName

            return Language.getRegisteredLanguages()
                .filter { it.displayName.isNotBlank() }
                .find { it.displayName.equals(normalizedName, ignoreCase = true) }
                ?: PlainTextLanguage.INSTANCE
        }
    }
}