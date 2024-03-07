package me.snowlight.paymentpg.service

import kotlinx.coroutines.flow.Flow
import me.snowlight.paymentpg.config.CacheKey
import me.snowlight.paymentpg.config.CacheManager
import me.snowlight.paymentpg.exception.NotFoundProductException
import me.snowlight.paymentpg.model.Product
import me.snowlight.paymentpg.model.ProductRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.minutes

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val cacheManager: CacheManager,
    @Value("\${spring.profiles.active:local}")
    private val profile: String,
    @Value("\${spring.application.name:payment-pg}")
    private val applicationName: String,
) {
    val CACHE_KEY = "${profile}/${applicationName}/product/".also { cacheManager.TTL[it] = 1.minutes }

    suspend fun get(id: Long): Product {
        val key = CacheKey(CACHE_KEY, id)
        return cacheManager.get(key) {
            productRepository.findById(id)
        }?: throw NotFoundProductException("$id 상품이 존재하지 않습니다.")
    }

    // TASK Paging 기능 추가 필요
    suspend fun getAll(): Flow<Product> {
        return productRepository.findAll()
    }
}