package io.grpc.healthcheck

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.healthcheck.HealthGrpcKt.HealthCoroutineStub
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class HealthCheckStreamClient(val channel: ManagedChannel) : Closeable {
    private val stub: HealthCoroutineStub = HealthCoroutineStub(channel)

    fun healthChecker(s: String) = runBlocking {
        val request = healthCheckRequest { service = s }
        val flow = stub.watch(request)
        flow.collect { response ->
            logger.info(response.message)
        }
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}
private val logger = LoggerFactory.getLogger(HealthCheckStreamClient::class.java)

fun main(args: Array<String>) {
    val isRemote = args.size == 1

    Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { dispatcher ->
        val builder = if (isRemote)
            ManagedChannelBuilder.forTarget(args[0].removePrefix("https://") + ":443").useTransportSecurity()
        else
            ManagedChannelBuilder.forTarget("localhost:50051").usePlaintext()

        HealthCheckStreamClient(
                builder.executor(dispatcher.asExecutor()).build()
        ).use {
            val dbState = args.singleOrNull() ?: "connected successfully"
            it.healthChecker(dbState)
        }
    }
}
