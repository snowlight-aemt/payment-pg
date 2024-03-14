package me.snowlight.paymentpg.config

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

interface WithRedisContainer {
    companion object {
        private val container = GenericContainer(DockerImageName.parse("redis")).apply {
            addExposedPorts(6379)
            start()
        }

        @DynamicPropertySource
        @JvmStatic
        fun setProperty(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.port") {
                "${container.getMappedPort(6379)}"
            }
        }
    }
}