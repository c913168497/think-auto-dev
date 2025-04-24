package think.auto.dev.agent.flow

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import think.auto.dev.agent.flow.autocode.handler.GetNeedClassAndFunFlow
import think.auto.dev.settings.aiProvider.AiProviderDBComponent
import think.auto.dev.settings.pseudocode.database.PseudocodeStandard
import think.auto.dev.settings.pseudocode.database.PseudocodeStandardDataBaseComponent
import think.auto.dev.utils.VmFileReadUtils
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ActionEvent
import javax.swing.*
class AIWorkflowAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        AIWorkflowWizard(project).show()
    }

    override fun update(e: AnActionEvent) {
        // 可以根据条件启用/禁用动作
        e.presentation.isEnabledAndVisible = true
    }
}

class AIWorkflowWizard(project: Project?) : DialogWrapper(project) {
    private val steps = listOf(GetNeedClassAndFunFlow.FUNCTION_NAME, "主要处理", "编码规范配置") // 修改第三步名称
    private var currentStep = 0
    private val modelSelections = mutableMapOf<Int, String>()

    // 伪代码规范对话框相关组件
    private val editorArea = JBTextArea(10, 50).apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private lateinit var standardsComboBox: JComboBox<PseudocodeStandard>
    private val allStandards = getAllPseudocodeStandardTitle()

    private val availableModels = AiProviderDBComponent.getAllAiProviders().map { it.customModelName }.toTypedArray()

    private val stepIndicatorPanel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 10))
    private val contentPanel = JPanel(BorderLayout())
    private val modelComboBox = JComboBox<String>()
    private val pseudocodePanel = createPseudocodePanel()

    init {
        title = "AI工作流配置向导"
        init()
        updateStepUI()
    }

    private fun createPseudocodePanel(): JComponent {
        return panel {
            row {
                label("编码规范:")
                standardsComboBox = comboBox(allStandards)
                    .applyToComponent {
                        renderer = object : DefaultListCellRenderer() {
                            override fun getListCellRendererComponent(
                                list: JList<*>,
                                value: Any?,
                                index: Int,
                                isSelected: Boolean,
                                cellHasFocus: Boolean
                            ) = super.getListCellRendererComponent(
                                list,
                                (value as? PseudocodeStandard)?.title ?: value,
                                index,
                                isSelected,
                                cellHasFocus
                            )
                        }
                        addActionListener { _: ActionEvent ->
                            updateEditorContent()
                        }
                    }
                    .gap(RightGap.SMALL)
                    .component

                button("新增") {
                    showAddStandardDialog()
                }
                button("修改") {
                    showEditStandardDialog()
                }
                button("删除") {
                    deleteCurrentStandard()
                }
            }

            row {
                cell(JBScrollPane(editorArea))
            }.resizableRow()
        }.apply {
            preferredSize = Dimension(900, 500)
        }
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout(10, 10)).apply {
            add(stepIndicatorPanel, BorderLayout.NORTH)
            add(contentPanel, BorderLayout.CENTER)
            add(createNavigationButtons(), BorderLayout.SOUTH)
        }
    }

    private fun updateStepUI() {
        stepIndicatorPanel.removeAll()
        steps.forEachIndexed { index, stepName ->
            val label = JLabel(stepName).apply {
                if (index == currentStep) {
                    font = font.deriveFont(Font.BOLD)
                    foreground = JBColor.BLUE
                } else if (index < currentStep) {
                    foreground = JBColor.GREEN
                } else {
                    foreground = JBColor.GRAY
                }
            }
            stepIndicatorPanel.add(label)
            if (index < steps.size - 1) {
                stepIndicatorPanel.add(JLabel("→"))
            }
        }

        contentPanel.removeAll()

        if (currentStep < 2) { // 只有前两步显示模型选择
            contentPanel.add(JLabel("为『${steps[currentStep]}』选择AI模型:").apply {
                border = BorderFactory.createEmptyBorder(0, 0, 10, 0)
            }, BorderLayout.NORTH)

            modelComboBox.model = DefaultComboBoxModel(availableModels)
            modelSelections[currentStep]?.let { modelComboBox.selectedItem = it }
                ?: run {
                    modelComboBox.selectedIndex = 0
                    modelSelections[currentStep] = modelComboBox.selectedItem as String
                }

            modelComboBox.addActionListener {
                modelSelections[currentStep] = modelComboBox.selectedItem as String
            }

            contentPanel.add(modelComboBox, BorderLayout.CENTER)

            val helpText = when (currentStep) {
                0 -> "预处理步骤建议使用DeepSeek进行快速初步处理"
                else -> "主要处理步骤可以使用GPT-4进行深度分析"
            }

            contentPanel.add(JLabel(helpText).apply {
                foreground = JBColor.GRAY
                border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
            }, BorderLayout.SOUTH)
        } else {
            // 第三步直接显示完整的编码规范配置面板
            contentPanel.add(pseudocodePanel, BorderLayout.CENTER)
        }

        contentPanel.revalidate()
        contentPanel.repaint()
        stepIndicatorPanel.revalidate()
        stepIndicatorPanel.repaint()
    }

    // 伪代码规范相关方法保持不变
    private fun updateEditorContent() {
        val pseudocodeStandard = standardsComboBox.selectedItem as? PseudocodeStandard ?: return
        editorArea.text = pseudocodeStandard.content ?: ""
    }

    private fun showAddStandardDialog() {
        val name = Messages.showInputDialog("请输入新编码规范名称:", "新增编码规范", null)
        name?.takeIf { it.isNotBlank() }?.let { title ->
            val newStandard = PseudocodeStandard(title = title, content = "")
            PseudocodeStandardDataBaseComponent.insertPseudocodeStandard(newStandard)
            standardsComboBox.addItem(newStandard)
            standardsComboBox.selectedItem = newStandard
            updateEditorContent()
        }
    }

    private fun showEditStandardDialog() {
        val pseudocodeStandard = standardsComboBox.selectedItem as? PseudocodeStandard ?: return
        val newName = Messages.showInputDialog(
            "修改编码规范名称:",
            "修改编码规范",
            null,
            pseudocodeStandard.title,
            null
        ) ?: return

        val updatedStandard = pseudocodeStandard.copy(title = newName)
        PseudocodeStandardDataBaseComponent.updatePseudocodeStandard(updatedStandard)
        standardsComboBox.removeAllItems()
        getAllPseudocodeStandardTitle().forEach { standardsComboBox.addItem(it) }
        standardsComboBox.selectedItem = updatedStandard
    }

    private fun deleteCurrentStandard() {
        val pseudocodeStandard = standardsComboBox.selectedItem as? PseudocodeStandard ?: return
        if (Messages.showYesNoDialog(
                "确定删除 '${pseudocodeStandard.title}' 吗?",
                "删除编码规范",
                Messages.getQuestionIcon()
            ) == Messages.YES
        ) {
            pseudocodeStandard.id?.let { PseudocodeStandardDataBaseComponent.deletePseudocodeStandard(it) }
            standardsComboBox.removeAllItems()
            getAllPseudocodeStandardTitle().forEach { standardsComboBox.addItem(it) }
        }
    }

    private fun getAllPseudocodeStandardTitle(): MutableList<PseudocodeStandard> {
        val remainingStandards = PseudocodeStandardDataBaseComponent.getAllPseudocodeStandardTitle()
        if (remainingStandards.isEmpty()) {
            val vmFileReadUtils = VmFileReadUtils("pseudocodeDefaultStandard")
            val nameStandard = vmFileReadUtils.readTemplate("function_name_standard.vm")
            remainingStandards.add(PseudocodeStandard(title = "命名要求", content = nameStandard))
        }
        return remainingStandards
    }

    private fun createNavigationButtons(): JPanel {
        return JPanel(FlowLayout(FlowLayout.RIGHT, 5, 5)).apply {
            if (currentStep > 0) {
                add(JButton("上一步").apply {
                    addActionListener {
                        currentStep--
                        updateStepUI()
                    }
                })
            }

            if (currentStep < steps.size - 1) {
                add(JButton("下一步").apply {
                    addActionListener {
                        currentStep++
                        updateStepUI()
                    }
                })
            } else {
                add(JButton("完成").apply {
                    addActionListener {
                        // 保存伪代码规范
                        val pseudocodeStandard = standardsComboBox.selectedItem as? PseudocodeStandard
                        pseudocodeStandard?.let {
                            it.content = editorArea.text
                            if (it.id != null) {
                                PseudocodeStandardDataBaseComponent.updatePseudocodeStandard(it)
                            } else {
                                PseudocodeStandardDataBaseComponent.insertPseudocodeStandard(it)
                            }
                        }

                        close(OK_EXIT_CODE)
                        showSelectedModels()
                    }
                })
            }
        }
    }

    private fun showSelectedModels() {
        val message = buildString {
            append("已选择以下AI模型配置:\n\n")
            modelSelections.forEach { (step, model) ->
                append("${steps[step]}: $model\n")
            }
        }

        Messages.showInfoMessage(message, "AI工作流配置")
    }
}