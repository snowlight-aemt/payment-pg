package me.snowlight.paymentpg.model

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinHashCode
import au.com.console.kassava.kotlinToString
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("TB_PRODUCT_IN_ORDER")
class ProductInOrder(
    var orderId: Long,      // PK
    var productId: Long,    // PK
    var price: Long,
    var quantity: Int = 0,
    @Id
    var seq: Long = 0,
): BaseEntity() {

    override fun equals(other: Any?): Boolean {
        return kotlinEquals(other, arrayOf(
            ProductInOrder::orderId,
            ProductInOrder::productId,
        ))
    }

    override fun hashCode(): Int {
        return kotlinHashCode(arrayOf(
            ProductInOrder::orderId,
            ProductInOrder::productId,
        ))
    }

    override fun toString(): String {
        return kotlinToString(arrayOf(
            ProductInOrder::orderId,
            ProductInOrder::productId,
            ProductInOrder::price,
            ProductInOrder::quantity,
        ), superToString = { super.toString() })
    }
}
