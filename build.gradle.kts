import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

val grpcVersion = "1.39.0"
val grpcKotlinVersion = "1.2.0"
val protobufVersion = "3.19.2"
val coroutinesVersion = "1.6.0"

plugins {
    application
    kotlin("jvm") version "1.6.10"
    id("com.google.protobuf") version "0.8.18"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

repositories {
    mavenLocal()
    google()
    jcenter()
    mavenCentral()
}

dependencies {
    // grpc
    implementation(kotlin("stdlib-jdk8"))
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    runtimeOnly("io.grpc:grpc-netty-shaded:$grpcVersion")
    // testing
    testImplementation(kotlin("test-junit"))
    testImplementation("io.grpc:grpc-testing:$grpcVersion")
    //hikari connection
    implementation("org.postgresql:postgresql:42.2.5")
    implementation("com.zaxxer:HikariCP:2.4.6")
    // logging
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
    implementation("org.slf4j:slf4j-api:1.7.9")
    implementation("org.slf4j:slf4j-simple:1.7.9")
    implementation("ch.qos.logback:logback-classic:1.0.13")
    implementation("ch.qos.logback:logback-core:1.0.13")
    implementation("org.slf4j:slf4j-log4j12:1.7.36")
    // prometheus
    implementation("io.micrometer:micrometer-registry-prometheus:1.9.2")
    implementation("org.springframework.boot:spring-boot-starter-actuator:2.7.2")
    // web service
    implementation("org.springframework.boot:spring-boot-starter-web:2.7.2")

//    implementation("org.springframework.boot:spring-boot-starter-web:2.7.2")
//
//    implementation("org.springframework.boot:spring-boot-starter-actuator:2.7.2")
//    implementation("io.micrometer:micrometer-registry-prometheus:1.9.2")
//
//    implementation ("org.springframework.boot:spring-boot-dependencies:2.0.5.RELEASE")
//    implementation ("org.springframework.boot:spring-boot-starter-web")

}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk7@jar"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("io.grpc.healthcheck.HealthCheckServerKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
}

tasks.register<JavaExec>("HealthCheckClient") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    main = "io.grpc.healthcheck.HealthCheckClientKt"
}

val otherStartScripts = tasks.register<CreateStartScripts>("otherStartScripts") {
    mainClassName = "io.grpc.healthcheck.HealthCheckClientKt"
    applicationName = "HealthCheckClientKt"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

tasks.named("startScripts") {
    dependsOn(otherStartScripts)
}

task("stage").dependsOn("installDist")

tasks.replace("assemble").dependsOn(":installDist")
