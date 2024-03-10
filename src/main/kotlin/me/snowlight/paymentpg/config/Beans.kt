package me.snowlight.paymentpg.config

import me.snowlight.paymentpg.model.ProductInOrderRepository
import me.snowlight.paymentpg.model.ProductRepository
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class Beans: ApplicationContextAware {
    companion object {
        lateinit var ctx: ApplicationContext
            private set

        fun <T: Any> getBean(byClass: KClass<T>, vararg arg: Any): T {
            return ctx.getBean(byClass.java, arg)
        }
    }
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        ctx = applicationContext
    }
}