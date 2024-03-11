package me.snowlight.paymentpg.service

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import me.snowlight.paymentpg.controller.ReqPaySucceed
import mu.KotlinLogging
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
        val connector = ReactorClientHttpConnector(HttpClient.create(provider).secure{ it.sslContext((insecureSSLContext)) } )

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

/*
curl --request POST \
  --url https://api.tosspayments.com/v1/payments/confirm \
  --header 'Authorization: Basic dGVzdF9za181T1dSYXBkQThkbVBaNHpPcXhRUjNvMXpFcVpLOg==' \
  --header 'Content-Type: application/json' \
  --data '{"paymentKey":"5zJ4xY7m0kODnyRpQWGrN2xqGlNvLrKwv1M9ENjbeoPaZdL6","orderId":"a4CWyWY5m89PNh7xJwhk1","amount":15000}'
 */