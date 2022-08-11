package io.grpc.healthcheck

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import io.grpc.healthcheck.HealthGrpcKt.HealthCoroutineStub
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class HealthCheckClient(val channel: ManagedChannel) : Closeable {
    private val stub: HealthCoroutineStub = HealthCoroutineStub(channel)
    private val logger = LoggerFactory.getLogger(HealthCheckClient::class.java)

    fun health(s: String) = runBlocking {
        val request = healthCheckRequest { service = s }
        try {
            val response = stub.check(request)
            logger.info("Health checker client received: ${response.message}")
        } catch (e: StatusException) {
            logger.error("RPC failed: ${e.status}")
        }
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}
fun main(args: Array<String>) {
    val isRemote = args.size == 1

    Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { dispatcher ->
        val builder = if (isRemote)
            ManagedChannelBuilder.forTarget(args[0].removePrefix("https://") + ":443").useTransportSecurity()
        else
            ManagedChannelBuilder.forTarget("localhost:50051").usePlaintext()

        HealthCheckClient(
            builder.executor(dispatcher.asExecutor()).build()
        ).use {
            val dbState = args.singleOrNull() ?: "connected"
            it.health(dbState)
        }
    }
}
