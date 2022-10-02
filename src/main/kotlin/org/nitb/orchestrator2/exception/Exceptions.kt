package org.nitb.orchestrator2.exception

class ImpossibleRemoveTaskException(msg: String? = null, throwable: Throwable? = null): Exception(msg, throwable)

class ImpossibleAddTaskException(msg: String? = null, throwable: Throwable? = null): Exception(msg, throwable)

class ImpossibleStopTaskException(msg: String? = null, throwable: Throwable? = null): Exception(msg, throwable)

class ImpossibleStartTaskException(msg: String? = null, throwable: Throwable? = null): Exception(msg, throwable)
