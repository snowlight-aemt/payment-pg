package me.snowlight.paymentpg.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import me.snowlight.paymentpg.model.Product
import me.snowlight.paymentpg.model.ProductRepository
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

// LEARN `productService.get` 캐쉬 되면 테스트에 이슈가 있을 수 있다. 해결 방법이 있을까?
@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest(
    @Autowired private val productService: ProductService,
    @Autowired private val productRepository: ProductRepository,
): StringSpec ({
    beforeTest {
        productRepository.save(Product(1, "apply", 1000).apply { new = true })
        productRepository.save(Product(2, "banana", 5000).apply { new = true })
        productRepository.save(Product(3, "melon", 700).apply { new = true })
    }

    afterTest {
        productRepository.deleteAll()
    }

    "get product" {
        val loadedProduct = productService.get(1L)

        loadedProduct.price shouldBe 1000
        loadedProduct.name shouldBe "apply"
    }
})