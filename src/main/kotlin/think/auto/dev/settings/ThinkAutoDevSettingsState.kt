package think.auto.dev.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.OptionTag
import think.auto.dev.constants.DEFAULT_HUMAN_LANGUAGE
import think.auto.dev.constants.HUMAN_LANGUAGES
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service(Service.Level.APP)
@State(name = "think.auto.dev.settings.AutoDevSettingsState", storages = [Storage("AutoDevSettings.xml")])
class ThinkAutoDevSettingsState : PersistentStateComponent<ThinkAutoDevSettingsState> {
    var language = DEFAULT_HUMAN_LANGUAGE

    @OptionTag(value = "lastCheckTime", converter = ZonedDateTimeConverter::class)
    var lastCheck: ZonedDateTime? = null

    fun fetchLocalLanguage(display: String = language): String {
        return HUMAN_LANGUAGES.getAbbrByDisplay(display)
    }

    @Synchronized
    override fun getState(): ThinkAutoDevSettingsState = this

    @Synchronized
    override fun loadState(state: ThinkAutoDevSettingsState) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        val language: String get() = getInstance().fetchLocalLanguage()

        fun getInstance(): ThinkAutoDevSettingsState {
            return ApplicationManager.getApplication().getService(ThinkAutoDevSettingsState::class.java).state
        }
    }
}

class ZonedDateTimeConverter : Converter<ZonedDateTime>() {
    override fun toString(value: ZonedDateTime): String? = value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)

    override fun fromString(value: String): ZonedDateTime? {
        return ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }
}
