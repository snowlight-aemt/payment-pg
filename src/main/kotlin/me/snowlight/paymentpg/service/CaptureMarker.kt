package me.snowlight.paymentpg.service

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import me.snowlight.paymentpg.exception.NotFoundOrderRepository
import me.snowlight.paymentpg.model.Order
import me.snowlight.paymentpg.model.OrderRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service

@Service
class CaptureMarker(
    template: ReactiveRedisTemplate<Any, Any>,
    @Value("\${spring.profiles.active}")
    profile: String,
    private val orderRepository: OrderRepository
) {
    private val ops = template.opsForSet()
    private val key = "$profile/capture-marker"

    suspend fun put(orderId: Long) {
        ops.add(key, orderId).awaitSingleOrNull()
    }

    suspend fun getAll(): List<Order> {
        return ops.members(key).asFlow().map {
            orderRepository.findById(it as Long)?: throw NotFoundOrderRepository("$it | 주문 정보가 존재하지 않습니다.")
        }.toList().sortedBy { it.updatedAt }
    }

    suspend fun remove(orderId: Long) {
        ops.remove(key, orderId).awaitSingleOrNull()
    }
}