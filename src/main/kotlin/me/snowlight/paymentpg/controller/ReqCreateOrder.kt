package me.snowlight.paymentpg.controller

import me.snowlight.paymentpg.model.Order
import me.snowlight.paymentpg.model.ProductInOrder

data class ReqCreateOrder (
    val userId: Long,
    var products: List<ReqProductQuantity>,
)

data class ReqProductQuantity (
    val productId: Long,
    val quantity: Int,
)
