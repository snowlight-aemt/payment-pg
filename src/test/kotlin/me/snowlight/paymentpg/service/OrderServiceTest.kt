package me.snowlight.paymentpg.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import me.snowlight.paymentpg.controller.PaymentType
import me.snowlight.paymentpg.controller.ReqCreateOrder
import me.snowlight.paymentpg.controller.ReqPaySucceed
import me.snowlight.paymentpg.controller.ReqProductQuantity
import me.snowlight.paymentpg.exception.NotFoundProductException
import me.snowlight.paymentpg.model.OrderRepository
import me.snowlight.paymentpg.model.PgStatus
import me.snowlight.paymentpg.model.Product
import me.snowlight.paymentpg.model.ProductInOrder
import me.snowlight.paymentpg.model.ProductInOrderRepository
import me.snowlight.paymentpg.model.ProductRepository
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException


// LEARN Test Case @Transactional @Rollback 지원하지 않음.
//@Transactional
@SpringBootTest
@ActiveProfiles("test", "toss-pay-test")
class OrderServiceTest(
    @Autowired private val orderService: OrderService,
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val productInOrderRepository: ProductInOrderRepository,
    @Autowired private val productRepository: ProductRepository,
    @Autowired private val tossPayApi: TossPayApi,
): StringSpec({

    beforeTest {
        productRepository.save(Product(id = 1L, "apple", 10000).apply { new = true })
        productRepository.save(Product(id = 2L, "banana", 50000).apply { new = true })
    }

    afterTest {
        productRepository.deleteAll()
        orderRepository.deleteAll()
        productInOrderRepository.deleteAll()
    }

    "create order, success case" {
        val order = orderService.create(
            ReqCreateOrder(
                1L, listOf(
                    ReqProductQuantity(1L, 10),
                    ReqProductQuantity(2L, 300)
                )
            )
        )

        order.products.size shouldBe 2
        order.products[0].id shouldBe 1
        order.products[0].quantity shouldBe 10
        order.products[1].id shouldBe 2
        order.products[1].quantity shouldBe 300

        order.amount shouldBe (10000 * 10) + (50000 * 300)
        order.description shouldNotBe null

    }

    "create order, fail case - found no product" {
        shouldThrow<NotFoundProductException> {
            orderService.create(
                ReqCreateOrder(
                    1L, listOf(
                        ReqProductQuantity(1L, 10),
                        ReqProductQuantity(2L, 300),
                        ReqProductQuantity(3L, 300)
                    )
                )
            )
        }
    }

    "order payment - mock" {
        val resOrder = orderService.create(
            ReqCreateOrder(
                11L, listOf(
                    ReqProductQuantity(1L, 10),
                    ReqProductQuantity(2L, 300),
                ))
        )
        orderService.get(resOrder.id).pgStatus shouldBe PgStatus.CREATE

        val request = ReqPaySucceed(
            paymentType = PaymentType.NORMAL,
            orderId = resOrder.pgOrderId!!,
            paymentKey = "ABCD",
            amount = resOrder.amount
        )
        orderService.authSuccess(request)
        val orderAuthed = orderService.get(resOrder.id).also { it.pgStatus shouldBe PgStatus.AUTH_SUCCESS }

        Mockito.`when`(tossPayApi.confirm(request)).thenReturn(ResConfirm(
            paymentKey = orderAuthed.pgKey!!,
            orderId = orderAuthed.pgOrderId!!,
            status = "Done",
            totalAmount = orderAuthed.amount,
            method = "card",
        ))

        orderService.capture(request)
        orderService.get(resOrder.id).pgStatus shouldBe PgStatus.CAPTURE_SUCCESS
    }

    "order payment fail - mock" {
        val resOrder = orderService.create(
            ReqCreateOrder(
                11L, listOf(
                    ReqProductQuantity(1L, 10),
                    ReqProductQuantity(2L, 300),
                ))
        )
        orderService.get(resOrder.id).pgStatus shouldBe PgStatus.CREATE

        val request = ReqPaySucceed(
            paymentType = PaymentType.NORMAL,
            orderId = resOrder.pgOrderId!!,
            paymentKey = "ABCD",
            amount = resOrder.amount
        )
        orderService.authSuccess(request)
        orderService.get(resOrder.id).also { it.pgStatus shouldBe PgStatus.AUTH_SUCCESS }

        Mockito.`when`(tossPayApi.confirm(request)).thenThrow(WebClientRequestException::class.java)

        orderService.capture(request)
        orderService.get(resOrder.id).pgStatus shouldBe PgStatus.CAPTURE_RETRY
    }


    "order payment fail response - mock" {
        val resOrder = orderService.create(
            ReqCreateOrder(
                11L, listOf(
                    ReqProductQuantity(1L, 10),
                    ReqProductQuantity(2L, 300),
                ))
        )
        orderService.get(resOrder.id).pgStatus shouldBe PgStatus.CREATE

        val request = ReqPaySucceed(
            paymentType = PaymentType.NORMAL,
            orderId = resOrder.pgOrderId!!,
            paymentKey = "ABCD",
            amount = resOrder.amount
        )
        orderService.authSuccess(request)
        orderService.get(resOrder.id).also { it.pgStatus shouldBe PgStatus.AUTH_SUCCESS }

        Mockito.`when`(tossPayApi.confirm(request)).thenThrow(WebClientResponseException::class.java)

        orderService.capture(request)
        orderService.get(resOrder.id).pgStatus shouldBe PgStatus.CAPTURE_FAIL
    }
})

@Configuration
@Profile("toss-pay-test")
class TossPayTestConfig {
    @Bean
    @Primary
    fun testTossPayApi(): TossPayApi {
        return Mockito.mock(TossPayApi::class.java)
    }
}