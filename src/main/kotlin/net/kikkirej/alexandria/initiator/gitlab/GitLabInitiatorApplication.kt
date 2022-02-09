package net.kikkirej.alexandria.initiator.gitlab

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class GitLabInitiatorApplication

fun main(args: Array<String>) {
	runApplication<GitLabInitiatorApplication>(*args)
}
