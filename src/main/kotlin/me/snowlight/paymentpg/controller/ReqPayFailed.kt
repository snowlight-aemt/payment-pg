package me.snowlight.paymentpg.controller

data class ReqPayFailed (
    val code: String,
    val message: String,
    val orderId: String,
)
