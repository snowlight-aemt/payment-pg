package me.snowlight.paymentpg.model

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductInOrderRepository: CoroutineCrudRepository<ProductInOrder, Long> {
    suspend fun findAllByOrderId(orderId: Long): List<ProductInOrder>
}