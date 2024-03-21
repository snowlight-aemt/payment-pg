package me.snowlight.paymentpg.service

import kotlinx.coroutines.delay
import me.snowlight.paymentpg.config.common.KafkaProducer
import me.snowlight.paymentpg.controller.PaymentType
import me.snowlight.paymentpg.controller.ReqPayFailed
import me.snowlight.paymentpg.controller.ReqPaySucceed
import me.snowlight.paymentpg.exception.InvalidOrderStatus
import me.snowlight.paymentpg.model.Order
import me.snowlight.paymentpg.model.OrderRepository
import me.snowlight.paymentpg.model.PgStatus
import me.snowlight.paymentpg.service.api.PaymentApi
import me.snowlight.paymentpg.service.api.TossPayApi
import me.snowlight.paymentpg.service.api.TossPayApiError
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class PaymentService(
    private val orderRepository: OrderRepository,
    private val orderService: OrderService,
    private val tossPayApi: TossPayApi,
    private val paymentApi: PaymentApi,
    private val captureMarker: CaptureMarker,
    private val kafkaProducer: KafkaProducer,
) {
    suspend fun recapture(orderId: Long) {
        orderRepository.findById(orderId)?.let { order ->
            delay(getBackoffDelay(order.pgRetryCount).also { logger.debug { " >> $it sec" } })
            this.capture(order)
        }
    }

    private fun getBackoffDelay(count: Int): kotlin.time.Duration {
        val temp = (2.0).pow(count).toInt() * 1000
        val delay = temp + (0..temp).random()
        return delay.milliseconds
    }

    // LEARN save 메서드를 사용해서 Transactional 끊은 이유는 ?
    //  - 외부 API 에 영향을 받지 않기 위해서 - 응닶이 올 때, 까지 대기 해야 한다.
    suspend fun capture(request: ReqPaySucceed) {
        val order = orderService.getOrderByPgOrderId(request.orderId)
        order.captureRequest()
        orderService.save(order)

        logger.debug { ">> order : $order" }
        capture(order)
    }

    // TODO 결제의 응답을 구분하기 위한 코드가 복잡하고 알아 보기 어렵다. (리펙터링 고민)
    suspend fun capture(order: Order) {
        if (order.pgStatus !in setOf(PgStatus.CAPTURE_REQUEST, PgStatus.CAPTURE_RETRY))
            throw InvalidOrderStatus("invalid order status (${order.pgStatus})")

        order.increaseRetryCount()
        captureMarker.put(order.id)

        try {
            tossPayApi.confirm(order.toReqPaySuccess()).also { logger.debug { ">> res: $it" } }
            order.captureSuccess()
        } catch (e: Exception) {
            logger.error(e.message, e)
            when (e) {
                is WebClientRequestException -> {
                    order.captureRetry()
                }
                is WebClientResponseException -> {
                    val response = e.getResponseBodyAs(TossPayApiError::class.java)
                    when (response?.code) {
                        "ALREADY_PROCESSED_PAYMENT" -> order.captureSuccess()
                        "PROVIDER_ERROR", "FAILED_INTERNAL_SYSTEM_PROCESSING" -> {
                            order.captureRetry()
                        }
                        else -> order.captureFail()
                    }
                }
                else -> order.captureFail()
            }
            if (order.pgStatus == PgStatus.CAPTURE_RETRY && order.pgRetryCount >= 3)
                order.captureFail()
            if (order.pgStatus != PgStatus.CAPTURE_SUCCESS)
                throw e
        } finally {
            orderService.save(order)
            captureMarker.remove(order.id)
            if (order.pgStatus == PgStatus.CAPTURE_RETRY) {
                paymentApi.recapture(order.id)
            }
            kafkaProducer.sendPayment(order)
        }
    }

    // TODO 다른 방법도 없는지 생각해 보자 (최신 데이터를 요청한다고 해도 이슈는 없게 설계되어 있지만)
    //  60 초 보다 빠르게 기동되면 데이터 손실(누수) 될 수 있다.
    suspend fun recaptureOnBoot() {
        val now = LocalDateTime.now()
        captureMarker.getAll()
            .filter { Duration.between(it.updatedAt!!, now).seconds >= 60  }
            .forEach {
                captureMarker.remove(it.id)
                paymentApi.recapture(it.id)
            }
    }

    suspend fun authSuccess(request: ReqPaySucceed): Boolean {
        val order = orderService.getOrderByPgOrderId(request.orderId)
        // LEARN try-finally 를 했을때, 이 점이 있을까?
        //  - 코드 가독성오 떨어져 보인다...;
        //  - 기능적으로 이적음 있을까?
        try {
            // LEARN 어디까지 Domain 으로 넘겨야 할 좋을까?
            return if (request.amount != order.amount) {
                logger.error { "amount 에 값이 하지 않습니다." }
                order.authInvalid(request.paymentKey)
                false
            } else {
                order.authSuccess(request.paymentKey)
                true
            }
        } finally {
            orderRepository.save(order)
        }
    }

    suspend fun authFailed(request: ReqPayFailed) {
        val order = orderService.getOrderByPgOrderId(request.orderId)
        order.authFail()
        orderRepository.save(order)

        logger.error { """
            >> Fail on error
             - request: $request
             - order: $order
        """.trimIndent() }
    }

    private fun Order.toReqPaySuccess(): ReqPaySucceed {
        return this.let {
            ReqPaySucceed(
                paymentKey = it.pgKey!!,
                orderId = it.pgOrderId!!,
                amount = it.amount,
                paymentType = PaymentType.NORMAL
            )
        }
    }
}