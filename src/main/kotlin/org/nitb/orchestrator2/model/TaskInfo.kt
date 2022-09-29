package org.nitb.orchestrator2.model

import com.fasterxml.jackson.annotation.JsonIgnore
import io.micronaut.core.annotation.Introspected
import org.nitb.orchestrator2.service.TaskInfo

@Introspected
data class TaskInfo (
    val type: String,
    val name: String,
    val parameters: Map<String, Any?>,
    @JsonIgnore
    @Transient
    private val strParameters: String
) {
    fun toGrpc(): TaskInfo {
        return TaskInfo.newBuilder().setType(type).setName(name).setParameters(strParameters).build()
    }
}