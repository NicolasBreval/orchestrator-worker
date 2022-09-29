package org.nitb.orchestrator2.exception

import com.fasterxml.jackson.core.JsonProcessingException
import com.google.rpc.Code
import com.google.rpc.ErrorInfo
import com.google.rpc.Status
import org.nitb.orchestrator2.service.TaskError
import org.nitb.orchestrator2.task.exception.TaskParametersCheckingException

object ExceptionUtils {

    fun checkExceptionCauseListContains(throwable: Throwable?, expected: Class<out Throwable>): Boolean {
        var cause = throwable
        var contained = false

        while (cause != null) {
            if (expected.isAssignableFrom(cause::class.java)) {
                contained = true
                break
            }
            cause = cause.cause
        }

        return contained
    }

}

fun Throwable.toTaskError(taskName: String, errorsWithStackTrace: Boolean, errorMessage: String = ""): TaskError = TaskError.newBuilder()
    .setTask(taskName)
    .setError(errorMessage)
    .setMessage(if (errorsWithStackTrace) this.stackTraceToString() else this.message).build()

fun Throwable.toProtoStatus(errorsWithStackTrace: Boolean): Status {
    val code = when (this) {
        is NullPointerException -> Code.DATA_LOSS_VALUE
        is JsonProcessingException -> Code.INVALID_ARGUMENT_VALUE
        is ImpossibleRemoveTaskException -> Code.INTERNAL_VALUE
        is ImpossibleAddTaskException -> {
            if (ExceptionUtils.checkExceptionCauseListContains(this, TaskParametersCheckingException::class.java)) Code.INVALID_ARGUMENT_VALUE
            else Code.INTERNAL_VALUE
        }
        else -> Code.UNKNOWN_VALUE
    }

    val message = when (this) {
        is NullPointerException -> "Invalid null request"
        is JsonProcessingException -> "Invalid argument value in some task's parameters argument. Task's parameters argument must be a JSON string with parameters for a task"
        is ImpossibleRemoveTaskException -> "Error updating task, it's not possible to remove previous instance of this task, please check reason for more information"
        is ImpossibleAddTaskException -> "Error adding task, it's not possible to create task instance, please check reason for more information"
        else -> "Unknown error produced, please check response message"
    }

    val reason = if (errorsWithStackTrace) this.stackTraceToString() else this.message

    return Status.newBuilder()
        .setCode(code)
        .setMessage(message)
        .addDetails(com.google.protobuf.Any.pack(
            ErrorInfo
            .newBuilder()
            .setReason(reason)
            .build())
        ).build()
}