package net.kikkirej.alexandria.initiator.gitlab.db

import org.springframework.data.repository.CrudRepository
import java.util.*

interface SourceRepository : CrudRepository<Source, Long>

interface ProjectRepository : CrudRepository<DBProject, Long>{
    fun findByExternalIdentifierAndSource(externalIdentifier: String, source: Source): Optional<DBProject>
}

interface VersionRepository : CrudRepository<Version, Long> {
    fun findByProjectAndName(project: DBProject, name: String) : Optional<Version>
}

interface AnalysisRepository : CrudRepository<Analysis, Long>