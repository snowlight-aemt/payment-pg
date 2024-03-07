package me.snowlight.paymentpg.service

import kotlinx.coroutines.flow.Flow
import me.snowlight.paymentpg.exception.NotFoundProductException
import me.snowlight.paymentpg.model.Product
import me.snowlight.paymentpg.model.ProductRepository
import org.springframework.stereotype.Service

@Service
class ProductService(
    private val productRepository: ProductRepository,
) {
    suspend fun get(id: Long): Product {
        return productRepository.findById(id)?: throw NotFoundProductException("$id 상품이 존재하지 않습니다.")
    }

    // TASK Paging 기능 추가 필요
    suspend fun getAll(): Flow<Product> {
        return productRepository.findAll()
    }
}