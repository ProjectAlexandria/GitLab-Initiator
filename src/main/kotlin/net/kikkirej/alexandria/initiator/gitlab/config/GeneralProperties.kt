package net.kikkirej.alexandria.initiator.gitlab.config

import org.camunda.bpm.extension.rest.EnableCamundaRestClient
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCamundaRestClient
@ConfigurationProperties("alexandria")
class GeneralProperties{
    var sharedfolder: String = "/alexandriadata"
    var processDefintionKey="alexandria-default"
}