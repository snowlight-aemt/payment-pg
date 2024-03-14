package me.snowlight.paymentpg.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.mpp.log
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.toList
import me.snowlight.paymentpg.config.extension.toLocalDate
import me.snowlight.paymentpg.controller.ReqCreateOrder
import me.snowlight.paymentpg.controller.ReqProductQuantity
import me.snowlight.paymentpg.model.Order
import me.snowlight.paymentpg.model.OrderRepository
import me.snowlight.paymentpg.model.PgStatus
import mu.KotlinLogging
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

private val logger = KotlinLogging.logger {}

@SpringBootTest
@ActiveProfiles("test")
class OrderHistoryServiceTest(
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val orderHistoryService: OrderHistoryService,
): StringSpec({
    "order history - user_id" {
        listOf(
            Order(userId = 1, description = "abcd", pgStatus = PgStatus.CREATE),
            Order(userId = 1, description = "abcd", pgStatus = PgStatus.AUTH_FAIL),
            Order(userId = 1, description = "abcd", pgStatus = PgStatus.AUTH_SUCCESS),
            Order(userId = 1, description = "abcd", pgStatus = PgStatus.AUTH_INVALID),
            Order(userId = 1, description = "abcd", pgStatus = PgStatus.CAPTURE_FAIL),
            Order(userId = 1, description = "abcd", pgStatus = PgStatus.CAPTURE_RETRY),
            Order(userId = 1, description = "abcd", pgStatus = PgStatus.CAPTURE_REQUEST),
            Order(userId = 1, description = "abcd", pgStatus = PgStatus.CAPTURE_SUCCESS),
        ).forEach {
            orderRepository.save(it)
        }

        val history = orderHistoryService.getHistory(QryOrderHistory(userId = 1))
        history.size shouldBe 3
    }

    "order history - " {
        var data = "2024-01-01".toLocalDate().atStartOfDay()
        listOf(
            Order(userId = 1, description = "A,C,B", amount = 1000, pgStatus = PgStatus.CAPTURE_REQUEST),
            Order(userId = 1, description = "B,C", amount = 1100, pgStatus = PgStatus.CAPTURE_REQUEST),
            Order(userId = 1, description = "D,G,F", amount = 1200, pgStatus = PgStatus.CAPTURE_REQUEST),
            Order(userId = 1, description = "D,F,Z", amount = 1300, pgStatus = PgStatus.CAPTURE_REQUEST),
            Order(userId = 1, description = "D,B,T", amount = 1400, pgStatus = PgStatus.CAPTURE_REQUEST),
            Order(userId = 1, description = "D,A,Y", amount = 1500, pgStatus = PgStatus.CAPTURE_REQUEST),
            Order(userId = 1, description = "D,G,E", amount = 1600, pgStatus = PgStatus.CAPTURE_REQUEST),
        ).forEach {
            it.pgStatus = PgStatus.CAPTURE_SUCCESS
            val save = orderRepository.save(it)
            save.createdAt = data
            data = data.plusDays(1)
            orderRepository.save(save)
        }

        orderRepository.findAll().toList().forEach { logger.info { it } }

        orderHistoryService.getHistory(QryOrderHistory(userId = 1)).size shouldBe 7

        orderHistoryService.getHistory(QryOrderHistory(userId = 1, keyword = "A")).size shouldBe 2
        orderHistoryService.getHistory(QryOrderHistory(userId = 1, keyword = "B")).size shouldBe 3
        orderHistoryService.getHistory(QryOrderHistory(userId = 1, keyword = "G")).size shouldBe 2

        orderHistoryService.getHistory(QryOrderHistory(userId = 1, keyword = "C B")).size shouldBe 2
        orderHistoryService.getHistory(QryOrderHistory(userId = 1, keyword = "C B")).map {it.description} shouldContainAll listOf("A,C,B", "B,C")

        // 페이징
        orderHistoryService.getHistory(QryOrderHistory(userId = 1, limit = 2)).size shouldBe 2
        orderHistoryService.getHistory(QryOrderHistory(userId = 1, limit = 2, page = 2)).first().id shouldBe
        orderHistoryService.getHistory(QryOrderHistory(userId = 1, limit = 3, page = 1)).last().id

        orderHistoryService.getHistory(QryOrderHistory(userId = 1, limit = 6, page = 2)).size shouldBe 1

        // 키워드, 금액
        orderHistoryService.getHistory(QryOrderHistory(userId = 1, keyword = "D", toAmount = 1200)).size shouldBe 1
        orderHistoryService.getHistory(QryOrderHistory(userId = 1, keyword = "F", fromAmount = 1300)).size shouldBe 1

        // 날짜
        orderHistoryService.getHistory(QryOrderHistory(userId = 1, fromDate = "20240106")).size shouldBe 2
        orderHistoryService.getHistory(QryOrderHistory(userId = 1, toDate = "20240103")).size shouldBe 3
    }

    beforeEach() {
    }
})