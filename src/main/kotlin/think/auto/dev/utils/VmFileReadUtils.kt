package think.auto.dev.utils

import java.nio.charset.Charset


const val ROOT = "prompts"

class VmFileReadUtils(private val pathPrefix: String) {

    fun readTemplate(filename: String): String {
        val path = getDefaultFilePath(filename)
        val resourceUrl = javaClass.classLoader.getResource(path) ?: return ""
        val bytes = resourceUrl.readBytes()
        val string = String(bytes, Charset.forName("UTF-8"))
        return string
    }

    private fun getDefaultFilePath(filename: String): String {
        val languagePrefix = "$ROOT/$pathPrefix".trimEnd('/')
        val path = "$languagePrefix/$filename"
        if (javaClass.classLoader.getResource(path) != null) {
            return path
        }

        val defaultLanguagePrefix = "$ROOT/$pathPrefix".trimEnd('/')
        return "$defaultLanguagePrefix/$filename"
    }
}