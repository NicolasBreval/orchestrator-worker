package org.nitb.orchestrator2.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Value
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.nitb.orchestrator2.exception.*
import org.nitb.orchestrator2.model.TaskDefinition
import org.nitb.orchestrator2.model.WorkerInfo
import org.nitb.orchestrator2.task.enums.TaskStatus
import org.nitb.orchestrator2.task.mq.impl.MQManager
import org.nitb.orchestrator2.task.util.TaskBuilder
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Context
@Singleton
class TaskPoolManagerService(
    @Value("\${orchestrator.errors.with-stack-trace}") private val errorsWithStackTrace: Boolean,
    @Value("\${orchestrator.metrics.print-task-list}") private val printTaskList: Boolean,
    @Value("\${orchestrator.server.port}") private val serverPort: Int?,
    @Value("\${orchestrator.server.name}") private val serverName: String?,
    @Value("\${grpc.server.port}") private val defaultServerPort: Int
) {

    fun addTasks(definitions: TaskDefinitionList): TaskResult {
        val successList = mutableListOf<String>()
        val errorsList = mutableListOf<TaskError>()

        definitions.taskListList.forEach { taskDefinition ->
            try {
                addNewTask(taskDefinition, definitions.targetWorker)
                successList.add(taskDefinition.name)
            } catch (e: Exception) {
                when (e) {
                    is ImpossibleRemoveTaskException, is ImpossibleAddTaskException -> {
                        errorsList.add(e.toTaskError(taskDefinition.name, errorsWithStackTrace, "TASK ADD ERROR"))
                    }
                    else -> throw e
                }
            }
        }

        return TaskResult.newBuilder().addAllSuccess(successList).addAllErrors(errorsList).build()
    }

    fun removeTasks(taskList: List<String>): TaskResult {
        val successList = mutableListOf<String>()
        val errorsList = mutableListOf<TaskError>()

        taskList.forEach { taskName ->
            try {
                removeTask(taskName)
                successList.add(taskName)
            } catch (e: Exception) {
                when (e) {
                    is ImpossibleRemoveTaskException, is ImpossibleAddTaskException -> {
                        errorsList.add(e.toTaskError(taskName, errorsWithStackTrace, "TASK ADD ERROR"))
                    }
                    else -> throw e
                }
            }
        }

        return TaskResult.newBuilder().addAllSuccess(successList).addAllErrors(errorsList).build()
    }

    fun startTasks(taskList: List<String>): TaskResult {
        val successList = mutableListOf<String>()
        val errorsList = mutableListOf<TaskError>()

        taskList.forEach { taskName ->
            try {
                startTask(taskName)
                successList.add(taskName)
            } catch (e: Exception) {
                when (e) {
                    is ImpossibleStartTaskException -> {
                        errorsList.add(e.toTaskError(taskName, errorsWithStackTrace, "TASK START ERROR"))
                    }
                    else -> throw e
                }
            }
        }

        return TaskResult.newBuilder().addAllSuccess(successList).addAllErrors(errorsList).build()
    }

    fun stopTasks(taskList: List<String>): TaskResult {
        val successList = mutableListOf<String>()
        val errorsList = mutableListOf<TaskError>()

        taskList.forEach { taskName ->
            try {
                stopTask(taskName)
                successList.add(taskName)
            } catch (e: Exception) {
                when (e) {
                    is ImpossibleStopTaskException -> {
                        errorsList.add(e.toTaskError(taskName, errorsWithStackTrace, "TASK STOP ERROR"))
                    }
                    else -> throw e
                }
            }
        }

        return TaskResult.newBuilder().addAllSuccess(successList).addAllErrors(errorsList).build()
    }

    fun getWorkerInfo(): org.nitb.orchestrator2.service.WorkerInfo = workerInfo.toGrpc()

    @Suppress("UNUSED")
    @Scheduled(fixedDelay = "1m")
    fun showCurrentTasks() {
        if (printTaskList && taskPool.isNotEmpty())
            logger.info("Current tasks registered on system:${taskPool.map { "\n\t${it.key} [${it.value.type}] - ${it.value.bean?.status ?: "ERROR OBTAINING STATUS"}" }.joinToString("")}")
    }

    @Suppress("UNUSED")
    @Scheduled(fixedDelay = "30s")
    fun sendInfoToManager() {
        taskPool.entries.filter { it.value.bean != null }.groupBy { it.value.bean?.status == TaskStatus.STOPPED }.let {
            val activeTasks = it[false]?.map { task -> task.value.toTaskInfo() } ?: listOf()
            val disabledTasks = it[true]?.map { task -> task.value.toTaskInfo() } ?: listOf()

            mqManager.send(workerName, managerQueue, WorkerInfo(workerName, serverName
                ?: InetAddress.getLocalHost().hostName, serverPort ?: defaultServerPort, activeTasks, disabledTasks))
        }
    }

    private val taskPool = ConcurrentHashMap<String, TaskDefinition>()

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val workerName = UUID.randomUUID().toString()

    private val workerInfo get() = taskPool.entries.filter { it.value.bean != null }.groupBy { it.value.bean?.status == TaskStatus.STOPPED }.let {
        val activeTasks = it[false]?.map { task -> task.value.toTaskInfo() } ?: listOf()
        val disabledTasks = it[true]?.map { task -> task.value.toTaskInfo() } ?: listOf()

        WorkerInfo(workerName, serverName ?: InetAddress.getLocalHost().hostName,
            serverPort ?: defaultServerPort, activeTasks, disabledTasks)
    }

    @Inject
    private lateinit var applicationContext: ApplicationContext

    @Inject
    private lateinit var taskBuilder: TaskBuilder

    @Inject
    private lateinit var jsonMapper: ObjectMapper

    @Inject
    private lateinit var mqManager: MQManager<*, *, *>

    @Value("\${orchestrator.mq.manager.queue}")
    private lateinit var managerQueue: String

    @Throws(JsonProcessingException::class, JsonMappingException::class, ImpossibleRemoveTaskException::class, ImpossibleAddTaskException::class)
    private fun addNewTask(definition: Task, targetWorker: String) {
        val parameters = jsonMapper.readValue(definition.parameters, object : TypeReference<Map<String, Any?>>() {})
        var update = true

        val newTask = if (targetWorker == workerName) null
        else if (taskPool.containsKey(definition.name)) { // If already exists a task with same name, tries to update it
            if (taskPool[definition.name]?.parameters != parameters) {
                try {
                    applicationContext.destroyBean(taskPool[definition.name]!!.bean!!)
                } catch (e: Exception) {
                    throw ImpossibleRemoveTaskException("Error removing task '${definition.name}' for update", e)
                }

                try {
                    taskBuilder.newTask(definition.type, definition.name, parameters)
                } catch (e: Exception) {
                    // If new definition of task is not correct and produces an exception during their creation, re-creates old version
                    taskBuilder.newTask(taskPool[definition.name]!!.type, definition.name, taskPool[definition.name]!!.parameters)
                    throw ImpossibleAddTaskException("Error to update task '${definition.name}'", e)
                }
            } else {
                update = false
                logger.info("Nothing change to apply to task '${definition.name}'")
                taskPool[definition.name]?.bean
            }
        } else {
            try {
                taskBuilder.newTask(definition.type, definition.name, parameters)
            } catch (e: Exception) {
                throw ImpossibleAddTaskException("Error to update task '${definition.name}'", e)
            }
        }

        if (update)
            taskPool[definition.name] = TaskDefinition(definition.type, definition.name, parameters, definition.parameters, newTask)
    }

    @Throws(ImpossibleRemoveTaskException::class)
    private fun removeTask(taskName: String) {
        try {
            stopTask(taskName)
            taskPool.remove(taskName)
        } catch (e: Exception) {
            throw ImpossibleRemoveTaskException("Error removing task '$taskName'", e)
        }
    }

    @Throws(ImpossibleStopTaskException::class)
    private fun stopTask(taskName: String) {
        val task = taskPool[taskName]

        if (task?.bean == null)
            logger.debug("Task '$taskName' wasn't initialized, so it's not necessary to stop it")
        if (task?.bean != null && task.bean.status != TaskStatus.STOPPED) {
            try {
                applicationContext.destroyBean(task.bean)
            } catch (e: Exception) {
                throw ImpossibleStopTaskException("Error destroying task's bean of '$taskName'", e)
            }
        } else {
            logger.debug("Task '$taskName' won't be stopped because it's already stopped")
        }
    }

    @Throws(ImpossibleStartTaskException::class)
    private fun startTask(taskName: String) {
        val task = taskPool[taskName] ?: throw ImpossibleStartTaskException("Not exists any task with name '$taskName'")

        if (task.bean == null || task.bean.status == TaskStatus.STOPPED) {
            try {
                addNewTask(Task.newBuilder().setName(task.name).setType(task.type).setParameters(task.strParameters).build(), workerName)
            } catch (e: Exception) {
                throw ImpossibleStartTaskException("Error starting task's bean of '$taskName'", e)
            }
        } else {
            logger.debug("Task '$taskName' won't be started because it's already started")
        }
    }
}