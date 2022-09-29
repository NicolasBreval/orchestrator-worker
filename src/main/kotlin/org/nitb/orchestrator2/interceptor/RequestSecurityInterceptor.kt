package org.nitb.orchestrator2.interceptor

import io.grpc.*
import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

@Suppress("UNUSED")
@Singleton
class RequestSecurityInterceptor(
    @Value("\${orchestrator.security.manager-token-enabled}") private val securityEnabled: Boolean,
    @Value("\${orchestrator.security.manager-token}") private val securityToken: String?
): ServerInterceptor {

    companion object {
        const val MANAGER_METADATA_KEY = "MANAGER-TOKEN"
    }

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>?,
        headers: Metadata?,
        next: ServerCallHandler<ReqT, RespT>?
    ): ServerCall.Listener<ReqT> {
        if (call?.methodDescriptor?.serviceName?.contains("ServerReflection") != true && securityEnabled
            && headers?.get(Metadata.Key.of(MANAGER_METADATA_KEY, ASCII_STRING_MARSHALLER)) != securityToken) {
            val metadata = Metadata()
            metadata.put(Metadata.Key.of("cause", ASCII_STRING_MARSHALLER), "Request's manager token is not valid")
            call?.close(Status.PERMISSION_DENIED, metadata)
        }
        return next!!.startCall(call, headers)
    }

}