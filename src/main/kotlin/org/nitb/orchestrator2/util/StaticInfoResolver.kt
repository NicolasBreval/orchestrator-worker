package org.nitb.orchestrator2.util

import org.slf4j.LoggerFactory
import java.util.jar.Attributes
import java.util.jar.JarInputStream

object StaticInfoResolver {

    val manifestProperties: Attributes? get() = manifestLocation

    private val logger = LoggerFactory.getLogger(StaticInfoResolver::class.java)

    private val manifestLocation: Attributes? =
        try {
            JarInputStream(StaticInfoResolver::class.java.protectionDomain.codeSource.location.openStream()).manifest.mainAttributes
        } catch (e: Exception) {
            logger.error("Unable to obtain MANIFEST.MF file")
            null
        }

}