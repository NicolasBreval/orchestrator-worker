package org.nitb.orchestrator2.model

import io.micronaut.core.annotation.Introspected
import org.nitb.orchestrator2.task.impl.BaseTask

@Introspected
data class TaskDefinition (
    val type: String,
    val name: String,
    val parameters: Map<String, Any?>,
    val strParameters: String,
    val bean: BaseTask<*, *>? = null
) {
    fun toTaskInfo() = TaskInfo(type, name, parameters, strParameters)
}