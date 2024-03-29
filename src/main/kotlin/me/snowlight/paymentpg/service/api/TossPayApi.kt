package me.snowlight.paymentpg.service.api

import io.netty.channel.ChannelOption
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
class TossPayApi(
    @Value("\${payment.toss.domain}")
    private val domain: String,
    @Value("\${payment.toss.key.secret}")
    private val secret: String,
) {
    private val client = createClient()

    private fun createClient(): WebClient {
        val insecureSSLContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
        var provider = ConnectionProvider.builder("toss-pay")
            .maxConnections(10)
            .pendingAcquireTimeout(Duration.ofSeconds(10))
            .build()
        val connector = ReactorClientHttpConnector(HttpClient.create(provider)
                .secure{ it.sslContext((insecureSSLContext)) }
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
        )

        return WebClient.builder().baseUrl(domain)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(connector)
            .build()
    }

    suspend fun confirm(request: ReqPaySucceed): ResConfirm {
        return client.post()
            .uri("/v1/payments/confirm")
            .header(HttpHeaders.AUTHORIZATION, "Basic $secret")
            .bodyValue(request)
            .retrieve()
            .awaitBody<ResConfirm>()
    }
}