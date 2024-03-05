package me.snowlight.paymentpg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PaymentPgApplication

fun main(args: Array<String>) {
    runApplication<PaymentPgApplication>(*args)
}
