package me.snowlight.paymentpg.service.api

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import me.snowlight.paymentpg.controller.ReqPaySucceed
import me.snowlight.paymentpg.service.CaptureMarker
import me.snowlight.paymentpg.service.ResConfirm
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

@Service
class PaymentApi(
    @Value("\${payment.self.domain}")
    domain: String,
    private val captureMarker: CaptureMarker,
) {
    private val client = WebClient.builder().baseUrl(domain)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    suspend fun recapture(orderId: Long) {
        captureMarker.put(orderId)
        client.put().uri("/recapture/$orderId").retrieve().bodyToMono<String>().subscribe()
    }
}