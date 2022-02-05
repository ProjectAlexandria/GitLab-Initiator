package net.kikkirej.alexandria.initiator.gitlab

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GitLabInitiatorApplication

fun main(args: Array<String>) {
	runApplication<GitLabInitiatorApplication>(*args)
}
