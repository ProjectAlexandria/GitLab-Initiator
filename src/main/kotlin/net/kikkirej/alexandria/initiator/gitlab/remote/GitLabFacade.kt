package net.kikkirej.alexandria.initiator.gitlab.remote

import net.kikkirej.alexandria.initiator.gitlab.config.GitLabSourceConfig
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GroupApi
import org.gitlab4j.api.models.Branch
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.Project
import org.springframework.stereotype.Service
import java.util.stream.Collectors
import java.util.stream.Stream

@Service
class GitLabFacade {
    
    private fun gitlabApiFor(sourceConfig: GitLabSourceConfig): GitLabApi {
        return GitLabApi(sourceConfig.url, sourceConfig.accessToken)
    }

    fun analyzingObjects(sourceConfig: GitLabSourceConfig): Map<Project, Collection<Branch>> {
        val gitlabApi = gitlabApiFor(sourceConfig)
        val projectsAndBranches = mutableMapOf<Project, List<Branch>>()
        getProjectsAsList(sourceConfig, gitlabApi)
            .forEach {project ->
                projectsAndBranches[project] = getBranches(gitlabApi, project.id)
                    .filter { branch -> isSearchedBranch(sourceConfig, branch) }
                    .collect(Collectors.toList())
            }
        return projectsAndBranches
    }

//    private fun getProjects(sourceConfig: GitLabSourceConfig, gitlabApi: GitLabApi): Stream<Project> {
//        return if (sourceConfig.groupId == null) {
//            gitlabApi.projectApi.projectsStream
//        } else {
//            val result = gitlabApi.groupApi.groupsStream
//                .map {group -> this.filterGroups(sourceConfig, gitlabApi, group)}
//                .flatMap {i -> i}
//                .collect(Collectors.toList())
//            result.stream()
//        }
//    }

    private fun getProjectsAsList(sourceConfig: GitLabSourceConfig, gitlabApi: GitLabApi): List<Project> {
        return if (sourceConfig.groupId == null) {
            gitlabApi.projectApi.projects
        } else {
            val groupList : MutableList<Group> = mutableListOf();
            getRelevantGroups(gitlabApi.groupApi!!, sourceConfig.groupId!!, groupList)
            val projectList : MutableList<Project> = mutableListOf();
            for(group in groupList) {
                projectList.addAll(filterGroupList(sourceConfig, gitlabApi, group))
            }
            projectList
        }
    }

    private fun getRelevantGroups(groupApi: GroupApi, groupId: String, groupList: MutableList<Group>) {
        val group = groupApi.getGroup(groupId.toInt())
        groupList.add(group)
        val subGroups = groupApi.getSubGroups(groupId)
        for (subGroup in subGroups){
            getRelevantGroups(groupApi, subGroup, groupList)
        }
    }

    private fun getRelevantGroups(groupApi: GroupApi, group: Group, groupList: MutableList<Group>) {
        groupList.add(group)
        val subGroups = groupApi.getSubGroups(group.id)
        for (subGroup in subGroups){
            getRelevantGroups(groupApi, subGroup, groupList)
        }
    }

    private fun filterGroupList(sourceConfig: GitLabSourceConfig, gitlabApi: GitLabApi, group: Group): List <Project> {
        val projectList: MutableList<Project> = mutableListOf();
        for(project in group.projects) {
            if(sourceConfig.repositoryNamePattern.isNullOrEmpty() || project.name?.matches(Regex(sourceConfig.repositoryNamePattern.toString())) != false) {
                projectList.add(project)
            }
        }
        for(subGroup in gitlabApi.groupApi.getSubGroups(group)) {
            projectList.addAll(filterGroupList(sourceConfig, gitlabApi, subGroup))
        }
        return projectList;
    }

//    private fun filterGroups(sourceConfig: GitLabSourceConfig, gitlabApi: GitLabApi, group: Group): Stream<Project> {
//        val projectStream = gitlabApi.groupApi.getProjectsStream(group.id)
//            .filter {project -> project.name?.matches(Regex(sourceConfig.repositoryNamePattern.toString())) ?: true}
//        val subGroupProjectStream = gitlabApi.groupApi.getSubGroupsStream(group)
//            .map {subGroup -> filterGroups(sourceConfig, gitlabApi, subGroup) }
//            .flatMap { i -> i }
//        return Stream.concat(projectStream, subGroupProjectStream)
//    }

    private fun getBranches(gitlabApi: GitLabApi, projectId: Int): Stream<Branch> {
        return gitlabApi.repositoryApi.getBranches(projectId).stream()
    }

    private fun isSearchedBranch(sourceConfig: GitLabSourceConfig, branch: Branch): Boolean {
        return if (sourceConfig.branchNamePattern == null) {
            true
        } else {
            branch.default ||  branch.name.matches(Regex(sourceConfig.branchNamePattern.toString()))
        }
    }
}