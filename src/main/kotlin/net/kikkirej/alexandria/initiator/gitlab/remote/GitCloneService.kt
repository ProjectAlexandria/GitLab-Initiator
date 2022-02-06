package net.kikkirej.alexandria.initiator.gitlab.remote

import net.kikkirej.alexandria.initiator.gitlab.config.GitLabSourceConfig
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.springframework.stereotype.Service
import java.io.File

@Service
class GitCloneService {
    fun clone(destination: String, cloneUrl: String, branch: String, source: GitLabSourceConfig){
        val cloneCommand = Git.cloneRepository()
        cloneCommand.setBranch(branch)
        cloneCommand.setDirectory(File(destination))
        cloneCommand.setURI(cloneUrl)
        cloneCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(source.accessUsername,source.accessToken))
        cloneCommand.call()
    }
}