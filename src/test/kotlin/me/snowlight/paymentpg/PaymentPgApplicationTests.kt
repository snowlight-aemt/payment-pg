package me.snowlight.paymentpg

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import me.snowlight.paymentpg.model.Product
import me.snowlight.paymentpg.model.ProductRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class PaymentPgApplicationTests(
    @Autowired private val productRepository: ProductRepository,
): StringSpec({
    "product save" {
        val prevCount = productRepository.count()
        val product = productRepository.save(Product(id = 1, name = "test 01", price = 100000))
        val currCount = productRepository.count()

        prevCount shouldBe currCount + 1
    }
}) {

    @Test
    fun contextLoads() {
    }

}
