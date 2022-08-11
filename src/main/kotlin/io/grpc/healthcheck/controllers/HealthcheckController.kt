package controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

//http://localhost:8080/actuator/prometheus
//http://localhost:8080/actuator
//http://localhost:8080/actuator/health/{*path}
//http://localhost:8080/actuator/health
//http://localhost:8080/actuator/info

@RestController
class HealthcheckController {
    @GetMapping("/healthcheckDB")
    fun healthcheckDB(@RequestParam(name = "nameDb", required = false) name: String): String {
        return "Connect to $name"
    }
}