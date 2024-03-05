package me.snowlight.paymentpg.config.validator

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import me.snowlight.paymentpg.config.extension.toLocalDate
import me.snowlight.paymentpg.config.extension.toString
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [DateValidator::class])
annotation class DataString(
    val message: String = "not a valid date",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
class DateValidator: ConstraintValidator<DataString, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        val text = value?.filter { it.isDigit() } ?: return true
        return kotlin.runCatching {
            text.toLocalDate().let {
                if(text != it.toString("yyyyMMdd")) null else true
            }
        }.getOrNull() != null
    }
}
