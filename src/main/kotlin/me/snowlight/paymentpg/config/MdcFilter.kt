package me.snowlight.paymentpg.config

import io.micrometer.context.ContextRegistry
import me.snowlight.paymentpg.config.extension.txid
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.*


private val logger = KotlinLogging.logger {}

val KEY_TXID = "txid"

@Component
@Order(1)
class MdcFilter: WebFilter {
    init {
        propagateMdcThroughReactor()
    }

    private fun propagateMdcThroughReactor() {
        Hooks.enableAutomaticContextPropagation()
        ContextRegistry.getInstance().registerThreadLocalAccessor(
            KEY_TXID,
            { MDC.get(KEY_TXID) },
            { value -> MDC.put(KEY_TXID, value) },
            { MDC.remove(KEY_TXID) }
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val uuid = exchange.request.headers["x-txid"]?.firstOrNull()?:"${UUID.randomUUID()}"
        // "%d{HH:mm:ss.SSS}|%highlight(%-5level)|%X{txid:-}|%green(%t)|%blue(\\(%F:%L\\))|%msg%n"
        logger.debug { "Filter" }
        logger.debug { "request id : ${exchange.request.id}" }
        MDC.put(KEY_TXID, uuid)
        return chain.filter(exchange).contextWrite {
            Context.of(KEY_TXID, uuid)
        }.doOnError {
            exchange.request.txid = uuid;
        }
    }
}