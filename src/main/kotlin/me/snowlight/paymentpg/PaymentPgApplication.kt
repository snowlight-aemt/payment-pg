package me.snowlight.paymentpg

import kotlinx.coroutines.runBlocking
import me.snowlight.paymentpg.service.PaymentService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontProperties.Application
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing

@SpringBootApplication
@EnableR2dbcAuditing
class PaymentPgApplication(
    private val paymentService: PaymentService,
): ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        // LEARN suspend fun 안되기 때문에 `runBlocking` 추가
        runBlocking {
            paymentService.recaptureOnBoot()
        }
    }
}

fun main(args: Array<String>) {
    runApplication<PaymentPgApplication>(*args)
}
