package think.auto.dev.settings.pseudocode


import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import think.auto.dev.settings.pseudocode.database.PseudocodeStandard
import think.auto.dev.settings.pseudocode.database.PseudocodeStandardDataBaseComponent
import think.auto.dev.utils.VmFileReadUtils
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JList

class PseudocodeStandardDialog(var project: Project?) : DialogWrapper(true) {

    private val editorArea = JBTextArea(10, 50).apply {
        lineWrap = true
        wrapStyleWord = true
    }

    private val allStandards = getAllPseudocodeStandardTitle()

    // 添加ComboBox引用
    private lateinit var standardsComboBox: JComboBox<PseudocodeStandard>

    init {
        title = "AI代码生成编码规范要求"
        init()
        // 设置对话框大小为800x600
        window.size = Dimension(1000, 600)
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row {
                label("编码规范:")
                // 使用完整形式的comboBox并保存引用
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
                        // 添加ActionListener替代whenItemSelected
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
                    .align(Align.FILL)
            }.resizableRow()
        }.also {
            updateEditorContent()
        }
        // 设置面板首选大小
        panel.preferredSize = Dimension(1000, 600)
        return panel
    }

    private fun updateEditorContent() {
        val pseudocodeStandard = standardsComboBox.selectedItem as PseudocodeStandard
        editorArea.text = pseudocodeStandard.content ?: ""
    }

    private fun showAddStandardDialog() {
        val name = Messages.showInputDialog("请输入新编码规范名称:", "新增编码规范", null)
        name?.takeIf { it.isNotBlank() }?.let { title ->
            val newStandard = PseudocodeStandard(title = title, content = "")
            PseudocodeStandardDataBaseComponent.insertPseudocodeStandard(newStandard)
            standardsComboBox.addItem(newStandard)
            standardsComboBox.revalidate()
            standardsComboBox.repaint()
        }
    }

    private fun showEditStandardDialog() {
        val pseudocodeStandard = standardsComboBox.selectedItem as PseudocodeStandard
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
        val getAllPrompts = PseudocodeStandardDataBaseComponent.getAllPseudocodeStandardTitle()
        for (getAllPrompt in getAllPrompts) {
            standardsComboBox.addItem(getAllPrompt)
        }

        standardsComboBox.revalidate()
        standardsComboBox.repaint()
    }

    private fun deleteCurrentStandard() {
        val pseudocodeStandard = standardsComboBox.selectedItem as PseudocodeStandard
        if (Messages.showYesNoDialog(
                "确定删除 '${pseudocodeStandard.title}' 吗?",
                "删除编码规范",
                Messages.getQuestionIcon()
            ) == Messages.YES
        ) {
            pseudocodeStandard.id?.let { PseudocodeStandardDataBaseComponent.deletePseudocodeStandard(it) }
            val remainingStandards = getAllPseudocodeStandardTitle()
            standardsComboBox.removeAllItems()
            for (remainingStandard in remainingStandards) {
                standardsComboBox.addItem(remainingStandard)
            }

            standardsComboBox.revalidate()
            standardsComboBox.repaint()
        }
    }

    fun getAllPseudocodeStandardTitle(): MutableList<PseudocodeStandard> {
        val remainingStandards = PseudocodeStandardDataBaseComponent.getAllPseudocodeStandardTitle()
        if (remainingStandards.isEmpty()) {
            val vmFileReadUtils = VmFileReadUtils("pseudocodeDefaultStandard")
            val nameStandard = vmFileReadUtils.readTemplate("function_name_standard.vm")
            remainingStandards.add(PseudocodeStandard(title = "命名要求", content = nameStandard))
        }

        return remainingStandards
    }

    override fun doOKAction() {
        val pseudocodeStandard = standardsComboBox.selectedItem as PseudocodeStandard
        pseudocodeStandard.content = editorArea.text
        val id = pseudocodeStandard.id
        if (id != null) {
            PseudocodeStandardDataBaseComponent.updatePseudocodeStandard(pseudocodeStandard)
        } else {
            PseudocodeStandardDataBaseComponent.insertPseudocodeStandard(pseudocodeStandard)
        }

        super.doOKAction()
    }

    fun getEditorText(): String = editorArea.text

    // 获取ComboBox引用的方法
    fun getStandardsComboBox(): JComboBox<PseudocodeStandard> = standardsComboBox
}