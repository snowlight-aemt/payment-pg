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
        return productRepository.findById(id)?: throw NotFoundProductException();
    }

    // TASK Paging 기능 추가 필요
    suspend fun getAll(): Flow<Product> {
        return productRepository.findAll();
    }
}