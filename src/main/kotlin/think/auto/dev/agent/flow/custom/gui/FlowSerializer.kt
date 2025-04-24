package think.auto.dev.agent.flow.custom.gui

import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import think.auto.dev.settings.flowEngine.database.FlowEngineConfig
import think.auto.dev.settings.flowEngine.database.FlowEngineConfigDataBaseComponent
import java.io.File

object FlowSerializer {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun saveFlow(project: Project, flow: FlowDefinition, flowEngineConfig: FlowEngineConfig?) {
        val json = Json { encodeDefaults = true }
        if (flowEngineConfig != null) {
            val flowEngineConfig = FlowEngineConfig(id = flowEngineConfig.id, title = flow.name, content = json.encodeToString(flow))
            FlowEngineConfigDataBaseComponent.updateFlowEngineConfig(flowEngineConfig)
        }else{
            val flowEngineConfig = FlowEngineConfig(title = flow.name, content = json.encodeToString(flow))
            FlowEngineConfigDataBaseComponent.insertFlowEngineConfig(flowEngineConfig)
        }

//        // 创建保存目录
//        val basePath = project.basePath ?: return
//        val flowsDir = File(basePath, ".idea/flows")
//        flowsDir.mkdirs()
//
//        // 保存流程定义
//        val flowFile = File(flowsDir, "${flow.name.replace(" ", "_")}.json")
//        flowFile.writeText(gson.toJson(flow))
//
//        // 刷新文件系统
//        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(flowFile)
    }

    fun loadFlow(project: Project, flowName: String): FlowDefinition? {
        val basePath = project.basePath ?: return null
        val flowFile = File(basePath, ".idea/flows/${flowName.replace(" ", "_")}.json")

        if (!flowFile.exists()) return null

        return try {
            gson.fromJson(flowFile.readText(), FlowDefinition::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun listFlows(project: Project): List<String> {
        val basePath = project.basePath ?: return emptyList()
        val flowsDir = File(basePath, ".idea/flows")

        if (!flowsDir.exists()) return emptyList()

        return flowsDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
            ?.map { it.nameWithoutExtension.replace("_", " ") }
            ?: emptyList()
    }
}