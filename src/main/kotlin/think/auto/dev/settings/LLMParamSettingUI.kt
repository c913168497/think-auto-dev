package think.auto.dev.settings

import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.FormBuilder
import kotlinx.serialization.json.Json
import think.auto.dev.agent.flow.custom.gui.FlowDefinition
import think.auto.dev.agent.flow.custom.gui.FlowDesignerDialog
import think.auto.dev.constants.DEFAULT_HUMAN_LANGUAGE
import think.auto.dev.constants.HUMAN_LANGUAGES
import think.auto.dev.settings.agentFlowAiProvider.database.AgentFunctionFlowAiProvider
import think.auto.dev.settings.agentFlowAiProvider.database.AgentFunctionFlowAiProviderDataBaseComponent
import think.auto.dev.settings.aiProvider.AddAiProviderDialog
import think.auto.dev.settings.aiProvider.AiProvider
import think.auto.dev.settings.aiProvider.AiProviderDBComponent
import think.auto.dev.settings.flowEngine.database.FlowEngineConfigDataBaseComponent
import think.auto.dev.settings.language.LanguageChangedCallback
import think.auto.dev.settings.language.ThinkAutoDevMessagesBundle
import think.auto.dev.settings.prompt.AddOrEditPromptDialog
import think.auto.dev.settings.prompt.database.Prompt
import think.auto.dev.settings.prompt.database.PromptDataBaseComponent
import think.auto.dev.settings.promptfunction.AddEditPromptFunctionDialog
import think.auto.dev.settings.promptfunction.PromptFunction
import think.auto.dev.settings.promptfunction.PromptFunctionDataBaseComponent
import think.auto.dev.settings.pseudocode.PseudocodeStandardDialog
import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.FlowLayout
import javax.swing.*

/**
 * 初始化 settingUI
 */
class LLMParamSettingUI(private val settings: ThinkAutoDevSettingsState) {


    val project = ProjectManager.getInstance().openProjects.firstOrNull()
    lateinit var customLanguagesComboBox: ComboBox<String>

    private val formBuilder: FormBuilder = FormBuilder.createFormBuilder()
    val panel: JPanel get() = formBuilder.panel

    fun applySettings(settings: ThinkAutoDevSettingsState, updateParams: Boolean = false) {
        if (updateParams && true.also { updateParams(settings) }) {
            return
        }

        panel.removeAll()
        val allAiProviders = AiProviderDBComponent.getAllAiProviders()
        // 创建组件
        val customModelLabel = JBLabel("AI模型")
        val customModelComboBox = ComboBox(allAiProviders.map { v -> v.customModelName }.toTypedArray())
        val customLanguagesLabel = JBLabel(ThinkAutoDevSettingsState.language)
        val customLanguagesAddBtn = JButton("Add AI Model")
        customLanguagesAddBtn.addActionListener {
            aiProviderAdd(
                project!!,
                customModelComboBox
            )
        }

        val customLanguagesEditBtn = JButton("Edit AI Model")
        customLanguagesEditBtn.addActionListener {
            aiProviderEdit(
                project!!,
                customModelComboBox
            )
        }

        val customLanguagesDelBtn = JButton("Del AI Model")
        customLanguagesDelBtn.addActionListener {
            aiProviderDel(
                project!!,
                customModelComboBox
            )
        }

        val customModelSelected = customModelComboBox.selectedItem
        var currentModelName = ""
        if (customModelSelected != null) {
            currentModelName = customModelComboBox.selectedItem as String
        }

        val currentAiProvider = AiProviderDBComponent.getAiProviderByModelName(currentModelName)
        customLanguagesComboBox = ComboBox(HUMAN_LANGUAGES.entries.map { it.display }.toTypedArray())
        // 创建一行面板
        val rowModelListPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        rowModelListPanel.add(customModelLabel)
        rowModelListPanel.add(customModelComboBox)
        rowModelListPanel.add(customLanguagesAddBtn)
        rowModelListPanel.add(customLanguagesEditBtn)
        rowModelListPanel.add(customLanguagesDelBtn)

        // 创建一行面板
        val rowLanguagesPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        rowLanguagesPanel.add(customLanguagesLabel)
        rowLanguagesPanel.add(customLanguagesComboBox)

        val allPrompts = PromptDataBaseComponent.getAllPrompts()
        val allPromptFunctions = PromptFunctionDataBaseComponent.getAllPrompts()
        // 提示词模板
        val initRowPromptPanel = initRowTemplateFunctionPanel(
            ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.label"),
            ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.add"),
            ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.edit"),
            ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.customPromptTemplate.delete"),
            allPrompts.toTypedArray(),
            allPromptFunctions.toTypedArray(),
            "prompt"
        )

        // 提示词功能
        val initRowTemplateFunctionPanel = initRowTemplateFunctionPanel(
            ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.templateFunction.label"),
            ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.templateFunction.add"),
            ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.templateFunction.edit"),
            ThinkAutoDevMessagesBundle.messageWithLanguageFromLLMSetting("settings.think.auto.dev.templateFunction.delete"),
            allPrompts.toTypedArray(),
            allPromptFunctions.toTypedArray(),
            "function"
        )

        formBuilder
            .addComponent(rowLanguagesPanel)
            .addSeparator()
            .addComponent(rowModelListPanel)
            .addComponent(panel {
                row {
                    comment("Prompts database Path: <a>Open Path</a>") {
                        RevealFileAction.openFile(getThinkAutoDevSqliteAbPath())
                    }
                }
            })
            .addComponent(panel {
                if (project != null) {
                    testLLMConnection(project, currentAiProvider)
                }
            })
            .addVerticalGap(2)
            .addSeparator()
        // 添加组件到行
        formBuilder
            .addComponent(initRowPromptPanel)
            .addComponent(initRowTemplateFunctionPanel)
            .addSeparator()
            .addComponent(initAiAgentPanel(project))
            .addComponentFillVertically(JPanel(), 0)
            .panel
        panel.invalidate()
        panel.repaint()
    }

    private fun initAiAgentPanel(project: Project?): JPanel {
        val rowModelListPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        val flowComboBox = JComboBox<String>()
        val comboBoxModel = DefaultComboBoxModel<String>()

        val newButton = JButton("新增")
        val editButton = JButton("编辑")
        val deleteButton = JButton("删除")
        loadFlows(flowComboBox, comboBoxModel, editButton, deleteButton)
        // 设置ComboBox
        flowComboBox.model = comboBoxModel
        flowComboBox.addActionListener {
            updateButtonState(flowComboBox, editButton, deleteButton)
        }
        val comboPanel = JPanel(BorderLayout(5, 5))
        comboPanel.add(JLabel("Ai Engine Flow:"), BorderLayout.WEST)
        comboPanel.add(flowComboBox, BorderLayout.CENTER)

        rowModelListPanel.add(comboPanel, BorderLayout.NORTH)
        // 按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 10))

        // 新增按钮
        newButton.addActionListener {
            val dialog = FlowDesignerDialog(project!!)
            dialog.show()

            // 对话框关闭后刷新列表
            if (dialog.isOK) {
                loadFlows(flowComboBox, comboBoxModel, editButton, deleteButton)
            }
        }
        buttonPanel.add(newButton)

        // 编辑按钮
        editButton.addActionListener {
            val selectedFlow = flowComboBox.selectedItem as? String ?: return@addActionListener

            // 获取选中的流程
            val flowConfig = FlowEngineConfigDataBaseComponent.getFlowEngineConfigByTitle(selectedFlow)
            if (flowConfig != null) {
                // 解析流程定义
                val json = Json { ignoreUnknownKeys = true }
                try {
                    val flowDefinition = json.decodeFromString(FlowDefinition.serializer(), flowConfig.content)

                    // 打开编辑对话框，并传入已有流程
                    val dialog = FlowDesignerDialog(project!!, flowDefinition, flowConfig)
                    dialog.show()

                    // 对话框关闭后刷新列表
                    if (dialog.isOK) {
                        loadFlows(flowComboBox, comboBoxModel, editButton, deleteButton)
                    }
                } catch (e: Exception) {
                    Messages.showErrorDialog(
                        "无法解析流程定义: ${e.message}",
                        "错误"
                    )
                }
            }
        }
        buttonPanel.add(editButton)

        // 删除按钮
        deleteButton.addActionListener {
            val selectedFlow = flowComboBox.selectedItem as? String ?: return@addActionListener

            // 确认删除
            val result = Messages.showYesNoDialog(
                "确定要删除流程 '$selectedFlow' 吗？",
                "确认删除",
                Messages.getQuestionIcon()
            )

            if (result == Messages.YES) {
                // 从数据库删除流程
                FlowEngineConfigDataBaseComponent.deleteFlowEngineConfig(selectedFlow)
                loadFlows(flowComboBox, comboBoxModel, editButton, deleteButton)
            }
        }
        buttonPanel.add(deleteButton)

        rowModelListPanel.add(buttonPanel, BorderLayout.CENTER)

        // 初始化按钮状态
        updateButtonState(flowComboBox, editButton, deleteButton)

        return rowModelListPanel
    }

    private fun loadFlows(
        flowComboBox: JComboBox<String>,
        comboBoxModel: DefaultComboBoxModel<String>,
        editButton: JButton,
        deleteButton: JButton
    ) {
        // 从数据库加载所有流程
        val flows = FlowEngineConfigDataBaseComponent.getAllFlowEngineConfig()

        comboBoxModel.removeAllElements()
        flows.forEach {
            comboBoxModel.addElement(it.title)
        }

        // Set the model to the ComboBox
        flowComboBox.model = comboBoxModel

        // Update button state based on whether there are any flows
        if (comboBoxModel.size > 0) {
            flowComboBox.selectedIndex = 0
        }

        updateButtonState(flowComboBox, editButton, deleteButton)
    }

    private fun updateButtonState(flowList: JComboBox<String>, editButton: JButton, deleteButton: JButton) {
        val hasSelection = flowList.selectedIndex >= 0
        editButton.isEnabled = hasSelection
        deleteButton.isEnabled = hasSelection
    }
//    private fun initProjectInfoPanel(project: Project?): JPanel {
//        val projectInfoLabel = JBLabel("Ai Engine: ")
//
//        val getNeedClassAndFunFlowButton = JButton(GetNeedClassAndFunFlow.FUNCTION_NAME)
//        getNeedClassAndFunFlowButton.addActionListener {
//            showProviderSelectionDialog(
//                functionName = GetNeedClassAndFunFlow.FUNCTION_NAME,
//                "Select Provider for GetNeedClassAndFunFlow"
//            )
//        }
//
//        val generateCodeFlowButton = JButton(GenerateCodeFlow.FUNCTION_NAME)
//        generateCodeFlowButton.addActionListener {
//            showProviderSelectionDialog(
//                functionName = GenerateCodeFlow.FUNCTION_NAME,
//                "Select Provider for GenerateCodeFlow"
//            )
//        }
//
//        val editProjectButton = JButton("Pseudocode Standard Edit")
//        editProjectButton.addActionListener {
//            projectInfoEditClicked(project)
//        }
//
//        val rowPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
//        rowPanel.add(projectInfoLabel)
//        rowPanel.add(getNeedClassAndFunFlowButton)
//        rowPanel.add(generateCodeFlowButton)
//        rowPanel.add(editProjectButton)
//        return rowPanel
//    }

    private fun showProviderSelectionDialog(functionName: String, title: String) {
        val allProviders = AiProviderDBComponent.getAllAiProviders()
        val allProvidersComboBox = ComboBox(allProviders.toTypedArray())
        // 设置默认值（这里假设你有一个获取默认值的方法，如果没有需要自行实现）
        val defaultProvider = getDefaultProviderForProject() // 你需要实现这个方法
        allProvidersComboBox.selectedItem = defaultProvider

        val panel = JPanel(BorderLayout())
        panel.add(JLabel("Select AI Provider:"), BorderLayout.NORTH)
        panel.add(allProvidersComboBox, BorderLayout.CENTER)

        val dialog = JDialog()
        dialog.title = title
        dialog.modalityType = Dialog.ModalityType.APPLICATION_MODAL
        dialog.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE

        val okButton = JButton("OK")
        okButton.addActionListener {
            val selectedProvider = allProvidersComboBox.selectedItem as AiProvider
            // 保存选择的provider，你需要实现保存逻辑
            saveSelectedProvider(selectedProvider, functionName) // 你需要实现这个方法
            dialog.dispose()
        }

        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener {
            dialog.dispose()
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.add(okButton)
        buttonPanel.add(cancelButton)

        panel.add(buttonPanel, BorderLayout.SOUTH)

        dialog.contentPane = panel
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }

    // 你需要实现这两个方法，根据你的业务逻辑
    private fun getDefaultProviderForProject(): AiProvider {
        // 返回项目的默认provider
        // 例如: return project?.getDefaultProvider() ?: AiProviderDBComponent.getAllAiProviders().first()
        return AiProviderDBComponent.getAllAiProviders().first() // 临时实现，请根据实际情况修改
    }

    private fun saveSelectedProvider(provider: AiProvider, functionName: String) {
        // 保存选择的provider到项目或全局设置
        val functionAiProvider =
            AgentFunctionFlowAiProviderDataBaseComponent.getAgentFunctionFlowAiProviderByTitle(functionName)
        if (functionAiProvider == null) {
            AgentFunctionFlowAiProviderDataBaseComponent.insertAgentFunctionFlowAiProvider(
                AgentFunctionFlowAiProvider(
                    functionName = functionName,
                    aiProviderId = provider.id!!
                )
            )
        } else {
            functionAiProvider.aiProviderId = provider.id!!
            AgentFunctionFlowAiProviderDataBaseComponent.updateAgentFunctionFlowAiProvider(functionAiProvider)
        }
    }

    /**
     * 处理项目信息编辑点击事件
     */
    private fun projectInfoEditClicked(project: Project?) {
        val dialog = PseudocodeStandardDialog(project)
        dialog.show()
    }


    /**
     * 行
     */
    private fun initRowTemplateFunctionPanel(
        label: String,
        add: String,
        edit: String,
        delete: String,
        prompts: Array<Prompt>,
        promptFunctions: Array<PromptFunction>,
        type: String
    ): JPanel {
        // 创建组件    private val featureInfoComboBox: JComboBox<FeatureInfos>
        val customPromptTemplateLabel = JBLabel(label)
        val customPromptTemplateComboBox = ComboBox(prompts)
        val addCustomPromptTemplateButton = JButton(add)
        val editCustomPromptTemplateButton = JButton(edit)
        val deleteCustomPromptTemplateButton = JButton(delete)
        // 设置按钮动作
        if (type == "prompt") {
            addCustomPromptTemplateButton.addActionListener {
                promptOnAddClicked(
                    project!!,
                    customPromptTemplateComboBox
                )
            }
            editCustomPromptTemplateButton.addActionListener {
                promptOnEditClicked(
                    project!!,
                    customPromptTemplateComboBox
                )
            }
            deleteCustomPromptTemplateButton.addActionListener {
                promptOnDeleteClicked(
                    project!!,
                    customPromptTemplateComboBox
                )
            }

            // 创建一行面板
            val rowTemplateFunctionPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
            rowTemplateFunctionPanel.add(customPromptTemplateLabel)
            rowTemplateFunctionPanel.add(customPromptTemplateComboBox)
            rowTemplateFunctionPanel.add(addCustomPromptTemplateButton)
            rowTemplateFunctionPanel.add(editCustomPromptTemplateButton)
            rowTemplateFunctionPanel.add(deleteCustomPromptTemplateButton)
            return rowTemplateFunctionPanel
        }

        val customPromptFunctionTemplateLabel = JBLabel(label)
        val customPromptFunctionTemplateComboBox = ComboBox(promptFunctions)
        val addCustomPromptFunctionTemplateButton = JButton(add)
        val editCustomPromptFunctionTemplateButton = JButton(edit)
        val deleteCustomPromptFunctionTemplateButton = JButton(delete)

        // 创建组件
        addCustomPromptFunctionTemplateButton.addActionListener {
            promptFunctionOnAddClicked(
                project!!,
                customPromptFunctionTemplateComboBox
            )
        }

        editCustomPromptFunctionTemplateButton.addActionListener {
            promptFunctionOnEditClicked(
                project!!,
                customPromptFunctionTemplateComboBox
            )
        }

        deleteCustomPromptFunctionTemplateButton.addActionListener {
            promptFunctionOnDeleteClicked(
                project!!,
                customPromptFunctionTemplateComboBox
            )
        }

        val rowTemplateFunctionPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        rowTemplateFunctionPanel.add(customPromptFunctionTemplateLabel)
        rowTemplateFunctionPanel.add(customPromptFunctionTemplateComboBox)
        rowTemplateFunctionPanel.add(addCustomPromptFunctionTemplateButton)
        rowTemplateFunctionPanel.add(editCustomPromptFunctionTemplateButton)
        rowTemplateFunctionPanel.add(deleteCustomPromptFunctionTemplateButton)
        return rowTemplateFunctionPanel
    }


    /**
     * 新增aiProvider
     */
    private fun aiProviderAdd(project: Project, customModelComboBox: ComboBox<String>) {
        val dialog = AddAiProviderDialog(project, customModelComboBox, null)
        dialog.show()
    }


    /**
     * 修改aiProvider
     */
    private fun aiProviderEdit(project: Project, customModelComboBox: ComboBox<String>) {
        val selectedItem = customModelComboBox.selectedItem as String
        val aiProvider = AiProviderDBComponent.getAiProviderByModelName(selectedItem) ?: return
        val dialog = AddAiProviderDialog(project, customModelComboBox, aiProvider.id)
        dialog.show()
    }


    /**
     * 删除aiProvider
     */
    private fun aiProviderDel(project: Project, customModelComboBox: ComboBox<String>) {
        val selectedItem = customModelComboBox.selectedItem as String
        val aiProvider = AiProviderDBComponent.getAiProviderByModelName(selectedItem) ?: return
        val result =
            Messages.showYesNoDialog(
                "确定要删除 '${aiProvider.customModelName}' 吗？",
                "确认删除",
                Messages.getQuestionIcon()
            )
        if (result == Messages.YES) {
            AiProviderDBComponent.deleteAiProvider(aiProvider.id!!)
            // 清空现有数据
            customModelComboBox.removeAllItems()
            val allProvider = AiProviderDBComponent.getAllAiProviders()
            for (aiProvider in allProvider) {
                customModelComboBox.addItem(aiProvider.customModelName)
            }

            // 刷新显示（可选）
            customModelComboBox.revalidate()
            customModelComboBox.repaint()
        }
    }

    /**
     * 新增提示词
     */
    private fun promptOnAddClicked(project: Project, promptsComboBox: JComboBox<Prompt>) {
        val dialog = AddOrEditPromptDialog(project, promptsComboBox, null)
        dialog.show()
        if (dialog.isOK) {
            refreshPromptComboBox(promptsComboBox)
        }
    }


    /**
     * 编辑提示词
     */
    private fun promptOnEditClicked(project: Project, promptsComboBox: JComboBox<Prompt>) {
        val selectedItem = promptsComboBox.selectedItem as? Prompt ?: return
        val dialog = AddOrEditPromptDialog(project, promptsComboBox, selectedItem.id)
        dialog.show()
        if (dialog.isOK) {
            refreshPromptComboBox(promptsComboBox)
        }
    }

    /**
     * 删除提示词
     */
    private fun promptOnDeleteClicked(project: Project, promptsComboBox: JComboBox<Prompt>) {
        val selectedItem = promptsComboBox.selectedItem as? Prompt ?: return
        val promptName = selectedItem.title
        val result =
            Messages.showYesNoDialog("确定要删除提示词 '$promptName' 吗？", "确认删除", Messages.getQuestionIcon())
        if (result == Messages.YES) {
            PromptDataBaseComponent.deletePrompts(selectedItem.id!!)
            // 清空现有数据
            promptsComboBox.removeAllItems()
            val getAllPrompts = PromptDataBaseComponent.getAllPrompts()
            for (getAllPrompt in getAllPrompts) {
                promptsComboBox.addItem(getAllPrompt)
            }

            // 刷新显示（可选）
            promptsComboBox.revalidate()
            promptsComboBox.repaint()
        }
    }

    /**
     * 新增提示词功能
     */
    private fun promptFunctionOnAddClicked(project: Project, promptsComboBox: JComboBox<PromptFunction>) {
        val dialog = AddEditPromptFunctionDialog(project, promptsComboBox, null)
        dialog.show()
    }

    /**
     * 编辑提示词功能
     */
    private fun promptFunctionOnEditClicked(project: Project, promptsComboBox: JComboBox<PromptFunction>) {
        val promptFunction = promptsComboBox.selectedItem as PromptFunction
        val dialog = AddEditPromptFunctionDialog(project, promptsComboBox, promptFunction.id)
        dialog.show()
    }

    /**
     * 删除提示词功能
     */
    private fun promptFunctionOnDeleteClicked(project: Project, promptsComboBox: JComboBox<PromptFunction>) {
        val promptsFunctionSelected = promptsComboBox.selectedItem ?: return
        val promptFunction = promptsFunctionSelected as PromptFunction
        val result =
            Messages.showYesNoDialog("确定要删除 '${promptFunction.title}' 吗？", "确认删除", Messages.getQuestionIcon())
        if (result == Messages.YES) {
            promptFunction.id?.let { PromptFunctionDataBaseComponent.deletePromptFunction(it) }
            // 清空现有数据
            promptsComboBox.removeAllItems()
            val getAllPrompts = PromptFunctionDataBaseComponent.getAllPrompts()
            for (getAllPrompt in getAllPrompts) {
                promptsComboBox.addItem(getAllPrompt)
            }

            // 刷新显示（可选）
            promptsComboBox.revalidate()
            promptsComboBox.repaint()

        }

    }


    /**
     * 刷新
     */
    private fun refreshPromptComboBox(comboBox: JComboBox<Prompt>) {
        // 清空现有数据
        comboBox.removeAllItems()
        val getAllPrompts = PromptDataBaseComponent.getAllPrompts()
        for (getAllPrompt in getAllPrompts) {
            comboBox.addItem(getAllPrompt)
        }

        // 刷新显示（可选）
        comboBox.revalidate()
        comboBox.repaint()
    }


    private fun updateParams(settings: ThinkAutoDevSettingsState) {
        settings.apply {
            customLanguagesComboBox.selectedItem = DEFAULT_HUMAN_LANGUAGE
        }
    }

    fun exportSettings(destination: ThinkAutoDevSettingsState) {
        destination.apply {
            DEFAULT_HUMAN_LANGUAGE = customLanguagesComboBox.selectedItem as String
        }
    }

    fun isModified(settings: ThinkAutoDevSettingsState): Boolean {
        return settings.language != customLanguagesComboBox.selectedItem as String
    }

    init {
        applySettings(settings)
        LanguageChangedCallback.language = ThinkAutoDevSettingsState.language
    }


}
