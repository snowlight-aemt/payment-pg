package me.snowlight.paymentpg.service

import kotlinx.coroutines.flow.toList
import me.snowlight.paymentpg.config.Beans
import me.snowlight.paymentpg.controller.ReqCreateOrder
import me.snowlight.paymentpg.controller.ReqPayFailed
import me.snowlight.paymentpg.controller.ReqPaySucceed
import me.snowlight.paymentpg.controller.ResOrder
import me.snowlight.paymentpg.controller.toResOrder
import me.snowlight.paymentpg.exception.NotFoundOrderRepository
import me.snowlight.paymentpg.exception.NotFoundProductException
import me.snowlight.paymentpg.model.Order
import me.snowlight.paymentpg.model.OrderRepository
import me.snowlight.paymentpg.model.ProductInOrder
import me.snowlight.paymentpg.model.ProductInOrderRepository
import me.snowlight.paymentpg.model.ProductRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*

private val logger = KotlinLogging.logger {}

@Transactional
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val productInOrderRepository: ProductInOrderRepository,
    private val tossPayApi: TossPayApi,
) {
    @Transactional(readOnly = true)
    suspend fun get(id: Long): ResOrder {
        return orderRepository.findById(id)?.toResOrder()?: throw NotFoundOrderRepository("$id | 주문 정보가 없습니다.")
    }

    @Transactional(readOnly = true)
    suspend fun getAllByUserId(userId: Long): List<ResOrder> {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map { it.toResOrder() }
    }

    @Transactional(readOnly = true)
    suspend fun getAll() = orderRepository.findAll()

    suspend fun delete(id: Long) = orderRepository.deleteById(id);

    suspend fun create(request: ReqCreateOrder): ResOrder {
        val productIds = request.products.map { it.productId }.toSet()
        val productsById = productRepository.findAllById(productIds).toList().associateBy { it.id }

        productIds.filter { !productsById.containsKey(it) }.let {
            if (it.isNotEmpty())
                throw NotFoundProductException("$it 상품은 존재하지 않습니다.")
        }

        val amount = request.products.sumOf { productsById[it.productId]!!.price * it.quantity }
        val description = request.products.joinToString(", ") { "${productsById[it.productId]!!.name} * ${it.quantity}" }

        val newOrder = orderRepository.save(
            Order(
                userId = request.userId,
                description = description,
                amount = amount,
                pgOrderId = "${UUID.randomUUID()}".replace("-", "")
            )
        )

//        val products = request.products.map {
//            ProductInOrder(
//                orderId = 1,
//                productId = 1,
//                price = 1000,
//                quantity = 1,
//            )
//        }
//        productInOrderRepository.saveAll(products).

        request.products.forEach {
            productInOrderRepository.save(
                ProductInOrder(
                    orderId = newOrder.id,
                    productId = it.productId,
                    price = productsById[it.productId]!!.price,
                    quantity = it.quantity
                ))
        }

        return newOrder.toResOrder()
    }

    // LEARN authSuccess 만을 위한 service 에 getOrderByPgOrderId 생성 | repository 를 직접 사용하지 않음 왜?
    //  - service 메서드를 생성 하는 방법: 서비스에서 DTO 로 응닶을 지정한 경우 Order 도메인에 비즈니스 로직을 사용하기 힘들다... ResOrder --> Order

    // LEARN authSuccess , capture 를 분리해서 service 추가한 이유?
    @Transactional(readOnly = true)
    suspend fun getOrderByPgOrderId(pgOrderId: String): Order {
        return this.orderRepository.findByPgOrderId(pgOrderId)
            ?: throw NotFoundOrderRepository("$pgOrderId | 해당 주문은 존재하지 않습니다.")
    }

    // LEARN @Transactional 어노테이션이 있는 같은 클래스 안에서 호출을 하면 적용되지 않는다. (Proxy 때문에)
    //  따라서 Spring Context 를 통해서 save 가 호출되어야 정상 동작을 한다.
    //  - authCapture -> save X
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    suspend fun save(order: Order) {
        this.orderRepository.save(order)
    }
    // LEARN save 메서드를 사용해서 Transactional 끊은 이유는 ?
    //  - 외부 API 에 영향을 받지 않기 위해서 - 응닶이 올 때, 까지 대기 해야 한다.
    suspend fun capture(request: ReqPaySucceed): Boolean {
        val order = this.getOrderByPgOrderId(request.orderId)
        order.capture()
        Beans.beanOrderService.save(order)

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
        val order = this.getOrderByPgOrderId(request.orderId)
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
        val order = this.getOrderByPgOrderId(request.orderId)
        order.authFail()
        orderRepository.save(order)

        logger.error { """
            >> Fail on error
             - request: $request
             - order: $order
        """.trimIndent() }
    }
}