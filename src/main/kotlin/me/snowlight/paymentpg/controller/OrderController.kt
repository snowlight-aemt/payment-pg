package me.snowlight.paymentpg.controller

import me.snowlight.paymentpg.service.OrderService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController(
    private val orderService: OrderService,
) {
    @GetMapping("/order/{id}")
    suspend fun get(@PathVariable id: Long) = orderService.get(id)

    @GetMapping("/order/all")
    suspend fun getAll() = orderService.getAll()

    @PostMapping("/order")
    suspend fun create(@RequestBody request: ReqCreateOrder) = orderService.create(request)
}
