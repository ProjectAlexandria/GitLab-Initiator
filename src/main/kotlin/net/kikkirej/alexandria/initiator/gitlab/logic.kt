package net.kikkirej.alexandria.initiator.gitlab

import net.kikkirej.alexandria.initiator.gitlab.camunda.CamundaLayer
import net.kikkirej.alexandria.initiator.gitlab.config.GeneralProperties
import net.kikkirej.alexandria.initiator.gitlab.config.GitLabInitConfig
import net.kikkirej.alexandria.initiator.gitlab.config.GitLabSourceConfig
import net.kikkirej.alexandria.initiator.gitlab.db.*
import net.kikkirej.alexandria.initiator.gitlab.remote.GitCloneService
import net.kikkirej.alexandria.initiator.gitlab.remote.GitLabFacade
import org.gitlab4j.api.models.Branch
import org.gitlab4j.api.models.Project
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.File

@Component
@Transactional
class GitLabInitiatorLogic(
    @Autowired val gitLabFacade: GitLabFacade,
    @Autowired val gitCloneService: GitCloneService,
    @Autowired val gitLabInitConfig: GitLabInitConfig,
    @Autowired val versionRepository: VersionRepository,
    @Autowired val analysisRepository: AnalysisRepository,
    @Autowired val projectRepository: ProjectRepository,
    @Autowired val sourceRepository: SourceRepository,
    @Autowired val generalProperties: GeneralProperties,
    @Autowired val camundaLayer: CamundaLayer)
{
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(initialDelay = 100, fixedRate = 24*60*60*1000)
    fun run() {
        for(sourceConfig in gitLabInitConfig.sources) {
            val dbSource: Source = getSourceDB(sourceConfig)
            val objectToAnalyze = gitLabFacade.analyzingObjects(sourceConfig)
            handleAnalysisOf(objectToAnalyze, dbSource, sourceConfig)
        }
    }

    private fun handleAnalysisOf(
        objectToAnalyze: Map<Project, Collection<Branch>>,
        dbSource: Source,
        sourceConfig: GitLabSourceConfig
    ) {
        for(repository in objectToAnalyze.keys){
            log.info("Starting analysis for Repository $repository")
            val dbProject = getDBProject(repository, dbSource)
            val branches = objectToAnalyze[repository]
            for(branch in branches!!){
                log.info("Starting analysis for Branch $branch")
                val version = getDBVersion(branch, dbProject)
                val analysis = Analysis(version = version)
                analysisRepository.save(analysis)
                val filePath = getFilePath(analysis)
                gitCloneService.clone(filePath, repository.httpUrlToRepo, version.name, sourceConfig)
                camundaLayer.startProcess(project = dbProject, version= version, analysis= analysis, filePath)
            }
        }
    }

    private fun getFilePath(analysis: Analysis): String {
        val upperFolder = File(generalProperties.sharedfolder)
        val analysisFolder = File(upperFolder.absolutePath + File.separator + analysis.id)
        return analysisFolder.absolutePath
    }

    private fun getDBVersion(branch: Branch, dbProject: DBProject) : Version {
        val versionOptional = versionRepository.findByProjectAndName(dbProject, branch.name)
        val version: Version
        if(versionOptional.isPresent){
            version = versionOptional.get()
            version.default_version=branch.default
        }else{
            version = Version(name = branch.name, project = dbProject)
        }

        version.setMetadata("protected", branch.protected)
        version.setMetadata("shA1", branch.commit.id)
        version.setMetadata("web_url", branch.webUrl)
        version.setMetadata("developers_can_merge", branch.developersCanMerge)
        version.setMetadata("developers_can_push", branch.developersCanPush)
        version.setMetadata("commit_timestamp", branch.commit.timestamp)
        versionRepository.save(version)
        return version
    }

    private fun getDBProject(project: Project, source: Source): DBProject {
        val projectOptional = projectRepository.findByExternalIdentifierAndSource(project.id.toString(), source)
        val dbProject: DBProject
        if(projectOptional.isPresent){
            dbProject = projectOptional.get()
            dbProject.url = project.webUrl
        }else{
            dbProject = DBProject(
                source = source,
                url = project.webUrl,
                externalIdentifier = project.id.toString()
            )
        }

        dbProject.setMetadata("description", project.description)
        dbProject.setMetadata("forks_count", project.forksCount)
        dbProject.setMetadata("full_name", project.nameWithNamespace)
        dbProject.setMetadata("git_transport_url", project.sshUrlToRepo)
        dbProject.setMetadata("http_transport_url", project.httpUrlToRepo)
        dbProject.setMetadata("merge_method", project.mergeMethod.name)
        dbProject.setMetadata("fork", project.forkedFromProject)
        dbProject.setMetadata("archived", project.archived)
        dbProject.setMetadata("delete_branch_on_merge", project.removeSourceBranchAfterMerge)
        dbProject.setMetadata("private", project.public.not())
        dbProject.setMetadata("url", project.webUrl)
        dbProject.setMetadata("owner_name", project.owner.name)
        dbProject.setMetadata("has_wiki", project.wikiEnabled)
        dbProject.setMetadata("has_issues", project.issuesEnabled)
        dbProject.setMetadata("has_downloads", project.packagesEnabled)
        dbProject.setMetadata("size", project.statistics.storageSize)
        dbProject.setMetadata("ssh_url", project.sshUrlToRepo)
        dbProject.setMetadata("star_count", project.starCount)
        dbProject.setMetadata("visibility_name", project.visibility.name)
        dbProject.setMetadata("open_issue_count", project.openIssuesCount)
        dbProject.setMetadata("pushed_at", project.lastActivityAt)
        projectRepository.save(dbProject)
        return dbProject
    }

    private fun getSourceDB(sourceConfig: GitLabSourceConfig): Source {
        val sourceOptional = sourceRepository.findById(sourceConfig.id)
        if(sourceOptional.isPresent){
            if(sourceOptional.get().type != "GitLab"){
                throw RuntimeException("")
            }
            return sourceOptional.get()
        }
        val source = Source(id = sourceConfig.id, name = sourceConfig.name,)
        sourceRepository.save(source)
        return source
    }

}

private fun Version.setMetadata(key: String, value: Any?) {
    for(obj in metadata){
        if(obj.key==key){
            if(value==null){
                obj.value=""
                obj.type=""
                return
            }
            obj.value = value.toString()
            obj.type = value::class.java.typeName
            return
        }
    }
    val versionMetadata: VersionMetadata = if(value == null){
        VersionMetadata(key = key, value = "", type = "", version = this)
    }else{
        VersionMetadata(key = key, value = value.toString(), type = value::class.java.typeName, version = this)
    }
    metadata.add(versionMetadata)
}

private fun DBProject.setMetadata(key: String, value: Any?) {
    for (obj in metadata) {
        if (obj.key == key) {
            if (value == null) {
                obj.value = ""
                obj.type = ""
                return
            }
            obj.value = value.toString()
            obj.type = value::class.java.typeName
            return
        }
    }
    val projectMetadata: ProjectMetadata = if (value == null) {
        ProjectMetadata(key = key, value = "", type = "", project = this)
    } else {
        ProjectMetadata(key = key, value = value.toString(), type = value::class.java.typeName, project = this)
    }
    metadata.add(projectMetadata)
}