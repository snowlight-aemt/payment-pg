package me.snowlight.paymentpg.model

import me.snowlight.paymentpg.controller.ResOrder
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository: CoroutineCrudRepository<Order, Long> {
    suspend fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Order>
}