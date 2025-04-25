package think.auto.dev.settings.aiProvider

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.FormBuilder
import think.auto.dev.settings.FormBuildParamComponents
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle
import java.awt.Dimension
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class AddAiProviderDialog(project: Project, val customModelComboBox: ComboBox<String>, val id: Int?) :
    DialogWrapper(project) {

    val aiProvider = id?.let { AiProviderDBComponent.getAiProviderById(it) }

    val formBuilder: FormBuilder = FormBuilder.createFormBuilder()

    private val customModelParam: FormBuildParamComponents by FormBuildParamComponents.creating {
        Editable(aiProvider?.customModel ?: "")
    }

    private val customModelNameParam: FormBuildParamComponents by FormBuildParamComponents.creating {
        Editable(aiProvider?.customModelName ?: "")
    }

    private val customOpenAIHostParam: FormBuildParamComponents by FormBuildParamComponents.creating {
        Editable(aiProvider?.customOpenAiHost ?: "")
    }

    private val customEngineTokenParam: FormBuildParamComponents by FormBuildParamComponents.creating {
        Password(aiProvider?.customEngineToken ?: "")
    }

    private val customEngineHeadParam: FormBuildParamComponents by FormBuildParamComponents.creating {
        Editable(aiProvider?.customEngineHead ?: "")
    }

    init {
        if (id == null) {
            title =
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.ai.provider.add.dialog.title")
        } else {
            title =
                ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.ai.provider.edit.dialog.title")
        }

        init()
        isModal = false
    }

    private val panel: JPanel
        get() = formBuilder.panel.apply {
            preferredSize = Dimension(500, 180) // 设置首选宽度
            minimumSize = Dimension(500, 180) // 设置最小宽度
            maximumSize = Dimension(500, 180) // 设置最大宽度
        }

    override fun createCenterPanel(): JComponent {
        panel.removeAll()
        val currentLLMParams = listOf(
            customModelNameParam,
            customModelParam,
            customOpenAIHostParam,
            customEngineTokenParam,
            customEngineHeadParam,
        )
        currentLLMParams.forEach { it.addToFormBuilder(formBuilder) }
        formBuilder.addComponentFillVertically(JPanel(), 0).panel
        panel.invalidate()
        panel.repaint()
        return panel
    }

    override fun doOKAction() {
        // 获取表单值
        val customModel = customModelParam.value
        val customModelName = customModelNameParam.value
        val customOpenAIHost = customOpenAIHostParam.value
        val customEngineToken = customEngineTokenParam.value
        val customEngineHead = customEngineHeadParam.value

        // 创建AIProvider对象
        val aiProvider = AiProvider(
            id, customOpenAIHost, customEngineToken, customEngineHead, customModel, customModelName
        )
        if (id == null) {
            // 保存到数据库
            AiProviderDBComponent.insertAiProvider(aiProvider)
        }else {
            AiProviderDBComponent.updateAiProvider(aiProvider)
        }

        super.doOKAction() // 确保调用父类方法关闭对话框
        refreshAiProviderComboBox(customModelComboBox)
    }


    /**
     * 刷新
     */
    private fun refreshAiProviderComboBox(comboBox: JComboBox<String>) {
        // 清空现有数据
        comboBox.removeAllItems()
        val allProvider = AiProviderDBComponent.getAllAiProviders()
        for (aiProvider in allProvider) {
            comboBox.addItem(aiProvider.customModelName)
        }

        // 刷新显示（可选）
        comboBox.revalidate()
        comboBox.repaint()
    }
}