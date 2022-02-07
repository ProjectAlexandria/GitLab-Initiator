package net.kikkirej.alexandria.initiator.gitlab.camunda

import net.kikkirej.alexandria.initiator.gitlab.config.GeneralProperties
import net.kikkirej.alexandria.initiator.gitlab.db.Analysis
import net.kikkirej.alexandria.initiator.gitlab.db.DBProject
import net.kikkirej.alexandria.initiator.gitlab.db.Version
import org.camunda.bpm.engine.RuntimeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class CamundaLayer (
    @Qualifier("remote") val runtimeService: RuntimeService,
    @Autowired val generalProperties: GeneralProperties,
) {
    fun startProcess(project: DBProject, version: Version, analysis: Analysis, filePath: String) {
        val processVariables = mutableMapOf<String, String>(
            "project_identifier" to project.externalIdentifier,
            "version_name" to version.name,
            "source_type" to project.source.type,
            "source_name" to project.source.name,
            "path" to filePath)
        addProjectVariables(project, processVariables)
        addVersionVariables(version, processVariables)
        val processInstance = runtimeService.startProcessInstanceByKey(generalProperties.processDefintionKey, analysis.id.toString(), processVariables.toMap())
    }

    private fun addProjectVariables(project: DBProject, processVariables: MutableMap<String, String>) {
        for(data in project.metadata){
            processVariables["project_"+data.key] = data.value
        }
    }

    private fun addVersionVariables(version: Version, processVariables: MutableMap<String, String>) {
        for(data in version.metadata){
            processVariables["version_"+data.key] = data.value.toString()
        }
    }
}