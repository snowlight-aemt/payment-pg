package me.snowlight.paymentpg.model

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository: CoroutineCrudRepository<Order, Long> {
}