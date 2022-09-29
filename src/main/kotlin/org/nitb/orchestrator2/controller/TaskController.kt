package org.nitb.orchestrator2.controller

import com.google.protobuf.Empty
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value
import io.micronaut.grpc.annotation.GrpcService
import jakarta.inject.Inject
import org.nitb.orchestrator2.exception.toProtoStatus
import org.nitb.orchestrator2.service.*
import org.nitb.orchestrator2.service.TaskServiceGrpc.TaskServiceImplBase
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

@Suppress("UNUSED")
@GrpcService
class TaskController(
    @Value("\${orchestrator.errors.with-stack-trace}") private val errorsWithStackTrace: Boolean
): TaskServiceImplBase() {

    override fun addTasks(request: TaskDefinitionList?, responseObserver: StreamObserver<TaskResult>?) {
        try {
            if (request == null)
                throw NullPointerException("Received an invalid null request on addTasks")

            val result = taskPoolManagerService.addTasks(request)

            responseObserver?.onNext(result)
            responseObserver?.onCompleted()
        } catch (e: Exception) {
            responseObserver?.onError(StatusProto.toStatusRuntimeException(e.toProtoStatus(errorsWithStackTrace)))
            logger.error("Error during task adding", e)
        }
    }

    override fun removeTasks(request: TaskServiceNameList?, responseObserver: StreamObserver<TaskResult>?) {
        try {
            if (request == null)
                throw NullPointerException("Received an invalid null request on removeTasks")

            val result = taskPoolManagerService.removeTasks(request.namesList)

            responseObserver?.onNext(result)
            responseObserver?.onCompleted()
        } catch (e: Exception) {
            responseObserver?.onError(StatusProto.toStatusRuntimeException(e.toProtoStatus(errorsWithStackTrace)))
            logger.error("Error during task removing", e)
        }
    }

    override fun startTasks(request: TaskServiceNameList?, responseObserver: StreamObserver<TaskResult>?) {
        try {
            if (request == null)
                throw NullPointerException("Received an invalid null request on startTasks")

            val result = taskPoolManagerService.startTasks(request.namesList)

            responseObserver?.onNext(result)
            responseObserver?.onCompleted()
        } catch (e: Exception) {
            responseObserver?.onError(StatusProto.toStatusRuntimeException(e.toProtoStatus(errorsWithStackTrace)))
            logger.error("Error during task starting", e)
        }
    }

    override fun stopTasks(request: TaskServiceNameList?, responseObserver: StreamObserver<TaskResult>?) {
        try {
            if (request == null)
                throw NullPointerException("Received an invalid null request on stopTasks")

            val result = taskPoolManagerService.stopTasks(request.namesList)

            responseObserver?.onNext(result)
            responseObserver?.onCompleted()
        } catch (e: Exception) {
            responseObserver?.onError(StatusProto.toStatusRuntimeException(e.toProtoStatus(errorsWithStackTrace)))
            logger.error("Error during task stopping", e)
        }
    }

    override fun getWorkerInfo(request: Empty?, responseObserver: StreamObserver<WorkerInfo>?) {
        try {
            if (request == null)
                throw NullPointerException("Received an invalid null request on getWorkerInfo")

            val info = taskPoolManagerService.getWorkerInfo()

            responseObserver?.onNext(info)
            responseObserver?.onCompleted()
        } catch (e: Exception) {
            responseObserver?.onError(StatusProto.toStatusRuntimeException(e.toProtoStatus(errorsWithStackTrace)))
            logger.error("Error obtaining worker info", e)
        }
    }

    override fun shutdown(request: Empty?, responseObserver: StreamObserver<Empty>?) {
        responseObserver?.onNext(Empty.newBuilder().build())
        responseObserver?.onCompleted()
        exitProcess(0)
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Inject
    private lateinit var taskPoolManagerService: TaskPoolManagerService

    @Inject
    private lateinit var applicationContext: ApplicationContext

    private fun checkSecurityToken() {

    }
}