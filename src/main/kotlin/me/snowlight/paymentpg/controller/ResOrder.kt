package me.snowlight.paymentpg.controller

import me.snowlight.paymentpg.config.Beans
import me.snowlight.paymentpg.model.Order
import me.snowlight.paymentpg.model.PgStatus

suspend fun Order.toResOrder(): ResOrder {
    return ResOrder(
        id = this.id,
        userId = this.userId,
        description = this.description,
        amount = this.amount,
        pgOrderId = this.pgOrderId,
        pgKey = this.pgKey,
        pgStatus = this.pgStatus,
        pgRetryCount = this.pgRetryCount,
        products = Beans.beanProductInOrderRepository.findAllByOrderId(this.id).map { productInOrder ->
            ResProductQuantity(
                id = productInOrder.productId,
                name = Beans.beanProductRepository.findById(productInOrder.productId)?.name ?: "unknown",
                price = productInOrder.price,
                quantity = productInOrder.quantity,
            )}
    )
}

data class ResOrder (
    val id: Long = 0,
    val userId: Long,
    val description: String? = null,
    val amount: Long = 0,
    val pgOrderId: String? = null,
    val pgKey: String? = null,
    val pgStatus: PgStatus = PgStatus.CREATE,
    val pgRetryCount: Int = 0,
    val products: List<ResProductQuantity>,
)

data class ResProductQuantity (
    val id: Long,
    val name: String,
    val price: Long,
    val quantity: Int,
)

//ProductQuantity