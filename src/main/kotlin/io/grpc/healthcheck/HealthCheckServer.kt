package io.grpc.healthcheck

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.sql.Connection
import java.sql.SQLException
import java.util.*
import javax.sql.DataSource


private val logger = LoggerFactory.getLogger(HealthCheckServer.HealthCheckService::class.java)

@SpringBootApplication
class HealthCheckServer(val port: Int) {
    val server: Server = ServerBuilder
            .forPort(port)
            .addService(HealthCheckService())
            .build()

    fun start() {
        server.start()
        logger.info("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
                Thread {
                    logger.info("*** shutting down gRPC server since JVM is shutting down")
                    stop()
                    logger.error("*** server shut down")
                }
        )
    }

    private fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }


    class HealthCheckService : HealthGrpcKt.HealthCoroutineImplBase() {
        private var datasource: DataSource? = null
        private var database: String? = "test"
        private var host: String? = "localhost"
        private var port: Int? = 5432

        private fun getDataSource(): Connection? {
            if (datasource == null) {
                val config = HikariConfig()
//                config.jdbcUrl = "jdbc:postgresql://localhost/test"
                config.jdbcUrl = ("jdbc:postgresql://" + this.host + ":" + this.port.toString() + "/"
                        + this.database + "?autoReconnect=true&serverTimezone=" + TimeZone
                    .getDefault().getID());
                config.username = "postgres"
                config.password = "admin"
                config.maximumPoolSize = 10
                config.isAutoCommit = false
                config.connectionTestQuery = "SELECT 1";
                config.maxLifetime = 1200000 // по умолчанию закрывает и обновляет соединения каждые maxLifetime=30 минут
                config.addDataSourceProperty("cachePrepStmts", "true")
                config.addDataSourceProperty("prepStmtCacheSize", "250")
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                datasource = HikariDataSource(config)
            }
            return datasource!!.connection
        }


        override suspend fun check(request: HealthCheckRequest) = healthCheckResponse {
            message = "Database ${request.service}"
        }

        override fun watch(request: HealthCheckRequest): Flow<HealthCheckResponse> = flow {
            while (true) {
                delay(1000)
//                emit(healthCheckResponse { message = "Database ${request.service}" })
//                emit(healthCheckResponse {
//                    HealthCheckResponse.ServingStatus.SERVING
//                    message = "Database ${request.service}";
//                };)
                try {
//                    DriverManager.getConnection(
//                        "jdbc:postgresql://127.0.0.1:5432/test", "postgres", "admin"
//                    )
                    getDataSource()!!
                        .use { conn ->
                            if (conn != null) {
                                logger.info("Connected to the database!")
                                emit(healthCheckResponse {
                                    message = "Database ${HealthCheckResponse.ServingStatus.SERVING}";
                                    HealthCheckResponse.ServingStatus.SERVING
                                })
                            } else {
                                logger.error("Failed to make connection!")
                                emit(healthCheckResponse {
                                    message = "Database ${HealthCheckResponse.ServingStatus.NOT_SERVING}";
                                    HealthCheckResponse.ServingStatus.NOT_SERVING
                                })
                            }
                        }
                } catch (e: SQLException) {
                    logger.info("SQL State: %s\n%s", e.sqlState, e.message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

    }
        companion object {
            @Throws(Exception::class)
            @JvmStatic
            fun main(args: Array<String>) {
                SpringApplication.run(HealthCheckServer::class.java, *args)
                val port = System.getenv("PORT")?.toInt() ?: 50051
                val server = HealthCheckServer(port)
                server.start()
                server.blockUntilShutdown()
            }
        }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 50051
    val server = HealthCheckServer(port)
    server.start()
    server.blockUntilShutdown()
}
