package net.kikkirej.alexandria.initiator.gitlab.remote

import net.kikkirej.alexandria.initiator.gitlab.config.GitLabSourceConfig
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.models.Branch
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.Project
import org.springframework.stereotype.Service
import java.util.stream.Stream

@Service
class GitLabFacade {
    
    private fun gitlabApiFor(sourceConfig: GitLabSourceConfig): GitLabApi {
        return GitLabApi(sourceConfig.url, sourceConfig.accessToken)
    }

    fun analyzingObjects(sourceConfig: GitLabSourceConfig): Map<Project, Collection<Branch>> {
        val gitlabApi = gitlabApiFor(sourceConfig)
        val projectsAndBranches = mutableMapOf<Project, Collection<Branch>>()
        getProjects(sourceConfig, gitlabApi)
            .forEach {project ->
                projectsAndBranches[project] = getBranches(gitlabApi, project.id)
                    .filter {branch -> isSearchedBranch(sourceConfig, project.defaultBranch, branch)}
                    .toList()
            }
        return projectsAndBranches
    }

    private fun getProjects(sourceConfig: GitLabSourceConfig, gitlabApi: GitLabApi): Stream<Project> {
        return if (sourceConfig.groupId == null) {
            gitlabApi.projectApi.projectsStream
        } else {
            gitlabApi.groupApi.groupsStream
                .map {group -> this.filterGroups(sourceConfig, gitlabApi, group)}
                .flatMap {i -> i}
        }
    }

    private fun filterGroups(sourceConfig: GitLabSourceConfig, gitlabApi: GitLabApi, group: Group): Stream<Project> {
        val projectStream = group.projects.stream()
            .filter {project -> project.name?.matches(Regex(sourceConfig.repositoryNamePattern.toString())) ?: false}
        val subGroupProjectStream = gitlabApi.groupApi.getSubGroupsStream(group)
            .map {subGroup -> filterGroups(sourceConfig, gitlabApi, subGroup) }
            .flatMap { i -> i }
        return Stream.concat(projectStream, subGroupProjectStream)
    }

    private fun getBranches(gitlabApi: GitLabApi, projectId: Int): Stream<Branch> {
        return gitlabApi.repositoryApi.getBranches(projectId).stream()
    }

    private fun isSearchedBranch(sourceConfig: GitLabSourceConfig, defaultBranch: String, branch: Branch): Boolean {
        return if (sourceConfig.branchNamePattern == null) {
            true
        } else {
            branch.name == defaultBranch ||  branch.name.matches(Regex(sourceConfig.branchNamePattern.toString()))
        }
    }
}