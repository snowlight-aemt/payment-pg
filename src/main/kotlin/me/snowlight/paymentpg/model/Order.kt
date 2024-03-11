package me.snowlight.paymentpg.model

import au.com.console.kassava.kotlinEquals
import au.com.console.kassava.kotlinHashCode
import au.com.console.kassava.kotlinToString
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("TB_ORDER")
class Order(
    @Id
    val id: Long = 0,
    var userId: Long,
    var description: String? = null,
    var amount: Long = 0,
    var pgOrderId: String? = null,
    var pgKey: String? = null,
    var pgStatus: PgStatus = PgStatus.CREATE,
    var pgRetryCount: Int = 0,
): BaseEntity() {
    override fun equals(other: Any?): Boolean {
        return kotlinEquals(other, arrayOf(
            Order::id
        ))
    }

    override fun hashCode(): Int {
        return kotlinHashCode(arrayOf(
            Order::id
        ))
    }

    override fun toString(): String {
        return kotlinToString(arrayOf(
            Order::id,
            Order::userId,
            Order::description,
            Order::amount,
            Order::pgOrderId,
            Order::pgKey,
            Order::pgStatus,
            Order::pgRetryCount,
        ), superToString = { super.toString() })
    }

    fun capture() {
        this.pgStatus = PgStatus.CAPTURE_REQUEST
//        this.pgRetryCount = 5
    }

    fun authSuccess(pgKey: String) {
        this.pgKey = pgKey
        this.pgStatus = PgStatus.AUTH_SUCCESS
    }

    fun authInvalid(pgKey: String) {
        this.pgKey = pgKey
        this.pgStatus = PgStatus.AUTH_INVALID
    }

    fun captureSuccess() {
        this.pgStatus = PgStatus.CAPTURE_SUCCESS
    }

    fun captureFail() {
        this.pgStatus = PgStatus.CAPTURE_FAIL
    }

    fun captureRetry() {
        this.pgStatus = PgStatus.CAPTURE_RETRY
    }

    fun authFail() {
        if (this.pgStatus == PgStatus.CREATE) {
            this.pgStatus = PgStatus.AUTH_FAIL
        }
    }
}