package me.snowlight.paymentpg.service

data class ResConfirm(
    val paymentKey: String,
    val orderId: String,
    val status: String,
    val totalAmount: Long,
    val method: String,
)