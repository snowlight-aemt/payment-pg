package me.snowlight.paymentpg.config.common

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import me.snowlight.paymentpg.model.Order
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.stereotype.Component
import reactor.kafka.sender.SenderOptions

private val logger = KotlinLogging.logger {  }

@Component
class KafkaProducer(
    private val template: ReactiveKafkaProducerTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    suspend fun send(topic: String, message: String) {
        logger.debug { "send to $topic : $message" }
        template.send(topic, message).awaitSingle()
    }

    suspend fun sendPayment(order: Order) {
        objectMapper.writeValueAsString(order).let { json ->
            send("payment", json)
        }
    }
}

@Configuration
class ReactiveKafkaInitializer {
    @Bean
    fun reactiveProducer(properties: KafkaProperties): ReactiveKafkaProducerTemplate<String, String> {
        return properties.buildProducerProperties()
            .let { prop ->
                // LEARN Idempotence Producer 세팅
                prop[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = true
                SenderOptions.create<String, String>(prop)
            }
            .let { option -> ReactiveKafkaProducerTemplate(option) }
    }
}