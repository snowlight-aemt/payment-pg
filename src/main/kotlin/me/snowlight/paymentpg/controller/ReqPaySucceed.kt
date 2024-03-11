package me.snowlight.paymentpg.controller

data class ReqPaySucceed (
    val paymentType: PaymentType,
    val orderId: String,
    val paymentKey: String,
    val amount: Long,
)

enum class PaymentType {
    NORMAL, BRANDPAY, KEYIN
}