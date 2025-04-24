package think.auto.dev.settings

import com.intellij.util.ui.FormBuilder
import think.auto.dev.settings.language.LanguageChangedCallback.jBLabel
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 参数组件工具
 */
class FormBuildParamComponents(
    value: String = "",
    var label: String = "",
    val isEditable: Boolean = true,
    val type: ParamType = ParamType.Text,
    var items: List<String> = emptyList(),
) {
    enum class ParamType {
        Text, Password, ComboBox, Separator
    }

    private var onChange: (FormBuildParamComponents.(String) -> Unit)? = null

    var value: String = value
        set(newValue) {
            val changed = field != newValue
            field = newValue
            if (changed) {
                onChange?.invoke(this, newValue)
            }
        }

    companion object {
        fun creating(
            onChange: FormBuildParamComponents.(String) -> Unit = {},
            block: Companion.() -> FormBuildParamComponents
        ) =
            PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, FormBuildParamComponents>> { _, _ ->
                object : ReadOnlyProperty<Any?, FormBuildParamComponents> {
                    private var param: FormBuildParamComponents? = null
                    override fun getValue(thisRef: Any?, property: KProperty<*>): FormBuildParamComponents {
                        return param ?: this@Companion.block().apply {
                            if (label.isEmpty()) {
                                label = "settings.${property.name}"
                            }

                            this.onChange = onChange
                            param = this
                        }
                    }
                }
            }

        fun Editable(value: String = "") = FormBuildParamComponents(value = value)
        fun Password(password: String = "") = FormBuildParamComponents(value = password, type = ParamType.Password)
        fun ComboBox(value: String, items: List<String>) =
            FormBuildParamComponents(value = value, type = ParamType.ComboBox, items = items.toList())
    }

    fun addToFormBuilder(formBuilder: FormBuilder) {
        when (this.type) {
            ParamType.Password -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactivePasswordField(this) {
                    this.text = it.value
                    this.isEnabled = it.isEditable
                }, 1, false)
            }

            ParamType.Text -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactiveTextField(this) {
                    this.isEnabled = it.isEditable
                }, 1, false)
            }

            ParamType.ComboBox -> {
                formBuilder.addLabeledComponent(jBLabel(this.label), ReactiveComboBox(this), 1, false)
            }

            else -> {
                formBuilder.addSeparator()
            }
        }
    }
}
