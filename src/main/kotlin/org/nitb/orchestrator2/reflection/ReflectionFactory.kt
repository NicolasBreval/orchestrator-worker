package org.nitb.orchestrator2.reflection

import io.grpc.protobuf.services.ProtoReflectionService
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton

@Suppress("UNUSED")
@Factory
@Requires(property = "orchestrator.grpc.reflection", value = "true")
internal class ReflectionFactory {
    @Singleton
    fun reflectionService(): ProtoReflectionService {
        return ProtoReflectionService.newInstance() as ProtoReflectionService
    }
}