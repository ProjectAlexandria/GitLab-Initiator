package net.kikkirej.alexandria.initiator.gitlab.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("alexandria.initiator.gitlab")
class GitLabInitConfig {
    var sources: List<GitLabSourceConfig> = listOf()
    var cron: String = "5/* * * * * *"
}

class GitLabSourceConfig(
    var id: Long =-1,
    var name: String="dummy",
    var url: String = "https://gitlab.com/",
    var organization: String?="dummy",
    var accessUsername: String="dummy",
    var accessToken: String="dummy",
    var repositoryNamePattern: String? = null,
    var groupId: String? = null,
    val branchNamePattern: String? = null,
)
