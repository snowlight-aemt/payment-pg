package me.snowlight.paymentpg.service

import me.snowlight.paymentpg.exception.NotFoundOrderRepository
import me.snowlight.paymentpg.model.OrderRepository
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val orderRepository: OrderRepository,
) {
    suspend fun get(id: Long) = orderRepository.findById(id)?: throw NotFoundOrderRepository()

    suspend fun getAll() = orderRepository.findAll()
}