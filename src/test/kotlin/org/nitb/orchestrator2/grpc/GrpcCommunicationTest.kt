package org.nitb.orchestrator2.grpc

import com.google.protobuf.Empty
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.nitb.orchestrator2.bean.TaskServiceClientGenerator

@MicronautTest(propertySources = ["classpath:test-application.yml"])
@Property(name = "O2_GRPC_SERVER_PORT", value = "8081")
@Property(name = "O2_ERRORS_WITH_STACK_TRACE", value = "true")
@Property(name = "O2_MQ_RABBIT_ENABLED", value = "true")
@Property(name = "O2_SECURITY_MANAGER_ENABLED", value = "false")
class GrpcCommunicationTest(
    @Value("grpc.server.port") private val serverPort: Int
) {

    @Test
    fun testServer() {
        val client = taskServiceClientGenerator.newSyncClient("localhost", serverPort)
        client.getWorkerInfo(Empty.newBuilder().build())
    }

    @Inject
    private lateinit var taskServiceClientGenerator: TaskServiceClientGenerator

}