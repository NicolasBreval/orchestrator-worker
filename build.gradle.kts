import com.google.protobuf.gradle.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.net.URI

val kotlinVersion: String by System.getProperties()

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("org.jetbrains.kotlin.kapt") version "1.7.10"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.micronaut.application") version "3.5.1"
    id("com.google.protobuf") version "0.8.15"
    id("net.nemerosa.versioning") version "2.7.1"
}

version = "0.0.1"
group = "org.nitb.orchestrator2"

repositories {
    maven {
        url = URI.create("https://jitpack.io")
    }
    mavenCentral()
}

dependencies {
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut.grpc:micronaut-grpc-runtime")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    implementation("io.micronaut:micronaut-validation")
    implementation("io.micronaut.jms:micronaut-jms-activemq-classic")
    implementation("io.micronaut.rabbitmq:micronaut-rabbitmq")
    implementation("io.grpc:grpc-services:1.50.2")
    implementation("com.github.NicolasBreval:orchestrator-task-base:0.0.1")

    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("ch.qos.logback:logback-classic")

    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("com.github.fridujo:rabbitmq-mock:1.2.0")
}


application {
    mainClass.set("org.nitb.orchestrator2.ApplicationKt")
}
java {
    sourceCompatibility = JavaVersion.toVersion("17")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    test {
        useJUnit()
        useJUnitPlatform()
    }
    jar {
        manifest {
            attributes (mapOf(
                "Version" to "${project.version}",
                "Built-By" to "nicolasbrevalrodriguez@gmail.com",
                "Build-Timestamp" to DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(OffsetDateTime.now()),
                "Build-Revision" to versioning.info.commit,
                "Created-By" to "Gradle ${gradle.gradleVersion}",
                "Build-Jdk" to "${System.getProperties()["java.version"]} (${System.getProperties()["java.vendor"]} ${System.getProperties()["java.vm.version"]})",
                "Build-OS" to "${System.getProperties()["os.name"]} ${System.getProperties()["os.arch"]} ${System.getProperties()["os.version"]}",
                "Main-Class" to "${application.mainClass}",
                "Class-Path" to configurations.runtimeClasspath.get().files.joinToString(" ") { it.name }
            ))
        }
    }
}
sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/grpc")
            srcDirs("build/generated/source/proto/main/java")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.20.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.46.0"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
            }
        }
    }
}
micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("org.nitb.orchestrator2.*")
    }
}

