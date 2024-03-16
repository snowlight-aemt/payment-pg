package me.snowlight.paymentpg.service.api

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import me.snowlight.paymentpg.controller.ReqPaySucceed
import me.snowlight.paymentpg.service.ResConfirm
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

@Service
class PaymentApi(
    @Value("\${payment.self.domain}")
    private val domain: String,

) {
    private val client = createClient()

    private fun createClient(): WebClient {
        return WebClient.builder().baseUrl(domain)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    suspend fun recapture(orderId: Long) {
        client.put().uri("/recapture/$orderId").retrieve()
    }
}