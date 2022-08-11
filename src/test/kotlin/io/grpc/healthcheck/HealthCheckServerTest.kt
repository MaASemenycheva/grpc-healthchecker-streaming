package io.grpc.healthcheck

import io.grpc.testing.GrpcServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthCheckServerTest {

    @get:Rule
    val grpcServerRule: GrpcServerRule = GrpcServerRule().directExecutor()

    @Test
    fun health() = runBlocking {
        val serviceGRPC = HealthCheckServer.HealthCheckService()
        grpcServerRule.serviceRegistry.addService(serviceGRPC)

        val stub = HealthGrpcKt.HealthCoroutineStub(grpcServerRule.channel)
        val testName = "connected successfully"

        val reply = stub.check(healthCheckRequest { service = testName })
        assertEquals("Database $testName", reply.message)
    }
}
