package org.nitb.orchestrator2.bean

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.nitb.orchestrator2.service.TaskServiceGrpc

@Singleton
class TaskServiceClientGenerator(
    @Value("\${grpc.client.plaintext}") private val plainText: Boolean,
    @Value("\${grpc.client.max-retry-attempts}") private val maxRetryAttempts: Int?
) {

    fun newAsyncClient(server: String, port: Int = 80): TaskServiceGrpc.TaskServiceStub {
        return TaskServiceGrpc.newStub(getChannel(server, port))
    }

    fun newSyncClient(server: String, port: Int = 80): TaskServiceGrpc.TaskServiceBlockingStub {
        return TaskServiceGrpc.newBlockingStub(getChannel(server, port))
    }

    private fun getChannel(server: String, port: Int = 80): ManagedChannel {
        return ManagedChannelBuilder.forAddress(server, port).let {
            if (plainText)
                it.usePlaintext()
            else it
        }.let {
            if (maxRetryAttempts != null)
                it.maxRetryAttempts(maxRetryAttempts)
            else it
        }.build()
    }

}