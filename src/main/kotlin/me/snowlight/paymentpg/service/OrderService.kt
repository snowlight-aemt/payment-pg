package me.snowlight.paymentpg.service

import kotlinx.coroutines.flow.toList
import me.snowlight.paymentpg.controller.ReqCreateOrder
import me.snowlight.paymentpg.controller.ResOrder
import me.snowlight.paymentpg.controller.toResOrder
import me.snowlight.paymentpg.exception.NotFoundOrderRepository
import me.snowlight.paymentpg.exception.NotFoundProductException
import me.snowlight.paymentpg.model.Order
import me.snowlight.paymentpg.model.OrderRepository
import me.snowlight.paymentpg.model.ProductInOrder
import me.snowlight.paymentpg.model.ProductInOrderRepository
import me.snowlight.paymentpg.model.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Transactional
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val productInOrderRepository: ProductInOrderRepository,
) {
    @Transactional(readOnly = true)
    suspend fun get(id: Long): ResOrder {
        return orderRepository.findById(id)?.toResOrder()?: throw NotFoundOrderRepository("$id | 주문 정보가 없습니다.")
    }

    @Transactional(readOnly = true)
    suspend fun getAllByUserId(userId: Long): List<ResOrder> {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map { it.toResOrder() }
    }

    @Transactional(readOnly = true)
    suspend fun getAll() = orderRepository.findAll()

    suspend fun delete(id: Long) = orderRepository.deleteById(id);

    suspend fun create(request: ReqCreateOrder): ResOrder {
        val productIds = request.products.map { it.productId }.toSet()
        val productsById = productRepository.findAllById(productIds).toList().associateBy { it.id }

        productIds.filter { !productsById.containsKey(it) }.let {
            if (it.isNotEmpty())
                throw NotFoundProductException("$it 상품은 존재하지 않습니다.")
        }

        val amount = request.products.sumOf { productsById[it.productId]!!.price * it.quantity }
        val description = request.products.joinToString(", ") { "${productsById[it.productId]!!.name} * ${it.quantity}" }

        val newOrder = orderRepository.save(
            Order(
                userId = request.userId,
                description = description,
                amount = amount,
                pgOrderId = "${UUID.randomUUID()}".replace("-", "")
            )
        )
        request.products.forEach {
            productInOrderRepository.save(
                ProductInOrder(
                    orderId = newOrder.id,
                    productId = it.productId,
                    price = productsById[it.productId]!!.price,
                    quantity = it.quantity
                ))
        }

        return newOrder.toResOrder()
    }
}