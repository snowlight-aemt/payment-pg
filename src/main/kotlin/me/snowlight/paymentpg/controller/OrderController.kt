package me.snowlight.paymentpg.controller

import me.snowlight.paymentpg.model.Order
import me.snowlight.paymentpg.service.CaptureMarker
import me.snowlight.paymentpg.service.OrderHistoryService
import me.snowlight.paymentpg.service.OrderService
import me.snowlight.paymentpg.service.PaymentService
import me.snowlight.paymentpg.service.QryOrderHistory
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val orderService: OrderService,
    private val orderHistoryService: OrderHistoryService,
    private val paymentService: PaymentService,
    private val captureMarker: CaptureMarker,
) {
    @GetMapping("/order/{id}")
    suspend fun get(@PathVariable id: Long) = orderService.get(id)

    @GetMapping("/order/all")
    suspend fun getAll(@RequestParam userId: Long) = orderService.getAllByUserId(userId)

    @DeleteMapping("/order/{id}")
    suspend fun delete(@PathVariable id: Long) = orderService.delete(id)

    @PostMapping("/order")
    suspend fun create(@RequestBody request: ReqCreateOrder) = orderService.create(request)

    @GetMapping("/history")
    suspend fun getHistory(request: QryOrderHistory) = orderHistoryService.getHistory(request)

    @PutMapping("/recapture/{id}")
    suspend fun recapture(@PathVariable id: Long) {
        paymentService.recapture(id)
    }

    @GetMapping("/capturing")
    suspend fun getCapturingOrder(): List<Order> = captureMarker.getAll()
}