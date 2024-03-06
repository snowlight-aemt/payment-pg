package me.snowlight.paymentpg

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import me.snowlight.paymentpg.model.Order
import me.snowlight.paymentpg.model.OrderRepository
import me.snowlight.paymentpg.model.Product
import me.snowlight.paymentpg.model.ProductInOrder
import me.snowlight.paymentpg.model.ProductInOrderRepository
import me.snowlight.paymentpg.model.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class PaymentPgApplicationTests(
    @Autowired private val productRepository: ProductRepository,
    @Autowired private val orderRepository: OrderRepository,
    @Autowired private val productInOrderRepository: ProductInOrderRepository,
): StringSpec({
    "product save" {
        val prevCount = productRepository.count()
        productRepository.save(Product(id = 1, name = "test 01", price = 100000).apply { new = true })
        val currCount = productRepository.count()

        currCount shouldBe prevCount + 1
    }

    "order save" {
        val prevCount = orderRepository.count()
        orderRepository.save(Order(userId = 1))
        val currCount = orderRepository.count()

        currCount shouldBe prevCount + 1
    }

    "productInOrder save" {
        val prevCount = productInOrderRepository.count()
        productInOrderRepository.save(ProductInOrder(1, 1, 1, 1))
        val currCount = productInOrderRepository.count()

        currCount shouldBe prevCount + 1
    }
})
