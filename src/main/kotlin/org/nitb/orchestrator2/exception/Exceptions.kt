package org.nitb.orchestrator2.exception

class ImpossibleRemoveTaskException(msg: String, throwable: Throwable): Exception(msg, throwable)

class ImpossibleAddTaskException(msg: String, throwable: Throwable): Exception(msg, throwable)

class ImpossibleStopTaskException(msg: String, throwable: Throwable): Exception(msg, throwable)

class ImpossibleStartTaskException(msg: String, throwable: Throwable): Exception(msg, throwable)
