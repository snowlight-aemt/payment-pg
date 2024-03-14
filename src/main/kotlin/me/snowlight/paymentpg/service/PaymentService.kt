package me.snowlight.paymentpg.service

import me.snowlight.paymentpg.config.Beans
import me.snowlight.paymentpg.controller.ReqPayFailed
import me.snowlight.paymentpg.controller.ReqPaySucceed
import me.snowlight.paymentpg.model.Order
import me.snowlight.paymentpg.model.OrderRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException

private val logger = KotlinLogging.logger {}

@Service
@Transactional
class PaymentService(
    private val orderRepository: OrderRepository,
    private val orderService: OrderService,
    private val tossPayApi: TossPayApi,
) {

    // LEARN save 메서드를 사용해서 Transactional 끊은 이유는 ?
    //  - 외부 API 에 영향을 받지 않기 위해서 - 응닶이 올 때, 까지 대기 해야 한다.
    suspend fun capture(request: ReqPaySucceed): Boolean {
        val order = orderService.getOrderByPgOrderId(request.orderId)
        order.capture()
        orderService.save(order)

        logger.debug { ">> order : $order" }

        return try {
            tossPayApi.confirm(request).also { logger.debug { ">> res: $it" } }
            order.captureSuccess()
            true
        } catch (e: Exception) {
            logger.error(e.message, e)
            when (e) {
                is WebClientRequestException -> order.captureRetry()
                is WebClientResponseException -> order.captureFail()
                else -> order.captureFail()
            }
            false
        } finally {
            orderRepository.save(order)
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
}