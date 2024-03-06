package me.snowlight.paymentpg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing

@SpringBootApplication
@EnableR2dbcAuditing
class PaymentPgApplication

fun main(args: Array<String>) {
    runApplication<PaymentPgApplication>(*args)
}
