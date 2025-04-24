package think.auto.dev.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import java.awt.event.ItemEvent
import javax.swing.event.DocumentEvent
import kotlin.reflect.KProperty


class Reactive<V>(var value: V, val onChange: (V) -> Unit)

operator fun <V> Reactive<V>.setValue(thisRef: Any?, property: KProperty<*>, value: V) {
    if (this.value == value) return
    this.value = value
    onChange(value)
}

operator fun <V> Reactive<V>.getValue(thisRef: Any?, property: KProperty<*>): V = this.value

/**
 * 文本输入
 */
fun ReactiveTextField(
    param: FormBuildParamComponents,
    initBlock: JBTextField.(FormBuildParamComponents) -> Unit = {}
): JBTextField {
    val component = JBTextField(param.value)
    val reactive by Reactive(param) {
        component.text = param.value
    }

    component.initBlock(reactive)
    component.document.addDocumentListener(CustomDocumentListener {
        param.value = component.text
    })
    return component
}

/**
 * 密码输入
 */
fun ReactivePasswordField(
    param: FormBuildParamComponents,
    initBlock: JBPasswordField.(FormBuildParamComponents) -> Unit = {}
): JBPasswordField {
    val component = JBPasswordField().apply {
        text = param.value
    }
    val reactive = Reactive(param) {
        component.text = it.value
    }

    component.initBlock(reactive.value)
    component.document.addDocumentListener(CustomDocumentListener {
        if (component.password.joinToString("") == param.value) return@CustomDocumentListener
        reactive.value.value = component.password.joinToString("")
    })

    return component
}

/**
 * 下拉选择
 */
fun ReactiveComboBox(
    param: FormBuildParamComponents,
    initBlock: ComboBox<String>.(FormBuildParamComponents) -> Unit = {}
): ComboBox<String> {
    val component = ComboBox(param.items.toTypedArray()).apply {
        selectedItem = param.value
    }
    val reactive by Reactive(param) {
        component.selectedItem = it.value
    }

    component.initBlock(reactive)
    component.addItemListener {
        if (it.stateChange == ItemEvent.SELECTED) {
            reactive.value = component.selectedItem as String
        }
    }
    return component
}

class CustomDocumentListener(val onChange: () -> Unit) : DocumentAdapter() {
    override fun textChanged(e: DocumentEvent) {
        onChange()
    }
}



