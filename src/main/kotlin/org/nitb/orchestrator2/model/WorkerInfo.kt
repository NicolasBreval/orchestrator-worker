package org.nitb.orchestrator2.model

import io.micronaut.core.annotation.Introspected
import java.lang.management.ManagementFactory
import com.sun.management.OperatingSystemMXBean
import org.nitb.orchestrator2.service.WorkerInfo
import org.nitb.orchestrator2.util.StaticInfoResolver

@Introspected
data class WorkerInfo(
    val name: String,
    val serverName: String,
    val serverPort: Int,
    val activeTasks: List<TaskInfo>,
    val disabledTasks: List<TaskInfo>,
    val cpuNumber: Int = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java).availableProcessors,
    val availableMemory: Long = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java).freeMemorySize,
    val serverCpuUsage: Double = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java).processCpuLoad,
    val systemCpuUsage: Double = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java).cpuLoad,
    val osArch: String = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java).arch,
    val osVersion: String = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java).version,
    val workerVersion: String? = StaticInfoResolver.manifestProperties?.getValue("Version"),
    val buildRevision: String? = StaticInfoResolver.manifestProperties?.getValue("Build-Revision")
) {

    fun toGrpc(): WorkerInfo {
        return WorkerInfo.newBuilder()
            .setName(name)
            .setServerName(serverName)
            .setServerPort(serverPort)
            .addAllActiveTasks(activeTasks.map { it.toGrpc() })
            .addAllDisabledTasks(disabledTasks.map { it.toGrpc() })
            .setCpuNumber(cpuNumber)
            .setAvailableMemory(availableMemory)
            .setServerCpuUsage(serverCpuUsage)
            .setSystemCpuUsage(systemCpuUsage)
            .setOsArch(osArch)
            .setOsVersion(osVersion)
            .setWorkerVersion(workerVersion ?: "")
            .setBuildRevision(buildRevision ?: "")
            .build()
    }

}